package who.smart.trust.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import who.smart.trust.trust.TrustError

/** Skill `hcert-trust-list`: the trust store the host fills. */
class TrustListTest {
    @Test fun `STT-TRU-001 STT-KID-001 a DSC is found by its kid, the first 8 bytes of its SHA-256`() {
        val store = Testkit.store(Testkit.esPem)
        val kid = Platform.sha256(Testkit.esDer).copyOf(8)
        assertEquals(1, store.candidates(kid).size)
        assertEquals(0, store.candidates(ByteArray(8)).size)
    }

    @Test fun `STT-KID-003 a kid reads and writes as standard and URL-safe base64`() {
        val kid = HcertCose.kidFromDsc(Testkit.esDer)
        for (alphabet in who.smart.trust.cose.Base64Alphabet.entries) {
            assertEquals(kid.toHex(), HcertCose.kidFromBase64(HcertCose.kidToBase64(kid, alphabet)).ok().toHex())
        }
    }

    @Test fun `STT-TRU-002 a GDHCN DID trustlist v2 document loads its DSCs`() {
        val did = """{"id":"did:web:tng-cdn-dev.who.int:v2:trustlist:-:XXA:DSC","verificationMethod":[
          {"id":"did:web:tng-cdn-dev.who.int:v2:trustlist:-:XXA:DSC#k1","type":"JsonWebKey2020",
           "publicKeyJwk":{"kty":"EC","crv":"P-256","x5c":["${Base64Std.encode(Testkit.esDer)}"]}},
          "did:web:tng-cdn-dev.who.int:v2:trustlist:-:XXB:DSC"]}"""
        val store = HcertTrustStore()
        assertEquals(1, store.addDidDocument(did).ok())
        val d = store.candidates(HcertCose.kidFromDsc(Testkit.esDer)).single()
        assertEquals(null to "XXA", d.domain to d.participant)
        assertEquals("XX", d.country)
    }

    @Test fun `STT-TRU-005 a static list of PEM certificates loads`() {
        val store = HcertTrustStore()
        assertEquals(2, store.addPem(Testkit.esPem + "\n" + Testkit.psPem).ok())
        assertEquals(2, store.size())
    }

    private fun did(domain: String) = """{"id":"did:web:tng-cdn.who.int:v2:trustlist:$domain:XXA:DSC","verificationMethod":[
      {"id":"did:web:tng-cdn.who.int:v2:trustlist:$domain:XXA:DSC#k1","type":"JsonWebKey2020",
       "publicKeyJwk":{"kty":"EC","crv":"P-256","x5c":["${Base64Std.encode(Testkit.esDer)}"]}}]}"""

    @Test fun `STT-SEL-006 trust domains are read from the GDHCN code system, not a closed list`() {
        val store = HcertTrustStore()
        assertEquals(emptyList(), store.trustDomains())
        assertEquals(1, store.addDidDocument(did("FUTURE")).ok()) // nothing loaded: kept as given
        assertEquals("FUTURE", store.candidates(HcertCose.kidFromDsc(Testkit.esDer)).single().domain)

        store.clear()
        val production = readRepo("vectors/gdhcn-trust-domains/CodeSystem-Domains.json")
        assertEquals(4, store.loadTrustDomains(production).ok())
        assertEquals(listOf("DDCC", "ICVP", "IPS-PILGRIMAGE", "PH4H"), store.trustDomains())
        assertEquals(1, store.addDidDocument(did("DDCC")).ok())
        assertEquals(TrustError.UnknownTrustDomain("FUTURE"), store.addDidDocument(did("FUTURE")).err())
        assertEquals(1, store.size()) // refused whole, nothing added
        store.clear()
        assertEquals(1, store.addDidDocument(did("-")).ok()) // the wildcard names no domain

        // A domain registered later is a new code in the data, not a new library release.
        val withFuture = production.replace("\"concept\" : [", "\"concept\" : [{\"code\":\"FUTURE\"},")
        assertEquals(5, store.loadTrustDomains(withFuture).ok())
        assertEquals(1, HcertTrustStore().apply { loadTrustDomains(withFuture).ok() }.addDidDocument(did("FUTURE")).ok())
        for (env in listOf("UAT", "DEV")) assertEquals(4, HcertTrustStore().loadTrustDomains(readRepo("vectors/gdhcn-trust-domains/CodeSystem-Domains-$env.json")).ok())
        assertIs<TrustError.Parse>(store.loadTrustDomains("""{"resourceType":"ValueSet"}""").err())
    }
}
