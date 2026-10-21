package who.smart.trust.core

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
