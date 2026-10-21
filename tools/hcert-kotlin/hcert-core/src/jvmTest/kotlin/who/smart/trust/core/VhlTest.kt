package who.smart.trust.core

import java.io.File
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import who.smart.trust.HcertResult
import who.smart.trust.issuer.IssueRequest
import who.smart.trust.issuer.Validity
import who.smart.trust.payloads.Payload
import who.smart.trust.payloads.RawPayload
import who.smart.trust.types.CoseAlgorithm
import who.smart.trust.types.FailureReason
import who.smart.trust.verifier.VerificationResult
import who.smart.trust.verifier.VerifierOptions
import who.smart.trust.vhl.SignatureRequest
import who.smart.trust.vhl.VhlError
import who.smart.trust.vhl.VhlPayload

private val repo = File(System.getProperty("repo.root"))
private fun file(path: String) = File(repo, path).readText()
private fun <T, E> HcertResult<T, E>.get(): T = when (this) {
    is HcertResult.Ok -> value
    is HcertResult.Err -> throw AssertionError("expected Ok, got $error")
}

/** The IHE ITI-YY3 example payload. */
private val ihe = VhlPayload(
    url = "https://vhl-sharer.example.org/List/_search?_id=abc123def456&code=folder&status=current&patient.identifier=urn:oid:2.16.840.1.113883.2.4.6.3|PASSPORT123&_include=List:item",
    key = "86F8LY5LlWAa1-OS_FgrTnYNqFHJP2ey5RSKLJBN9jk",
    exp = 1735689600, flag = "LP", label = "Patient Health Summary", v = 1,
    fhirBaseUrl = "https://vhl-sharer.example.org",
)

/** [STT-VHL-001, STT-VHL-003, STT-VHL-004, STT-VHL-009] */
class VhlPayloadTest {
    @Test fun ihePayloadIsValid() = assertEquals(emptyList(), HcertVhl.validate(ihe, null))

    @Test fun rules() {
        fun codes(p: VhlPayload, now: Long? = null) = HcertVhl.validate(p, now).map { it.code }.toSet()
        // the ITI-YY4 example key: 44 characters, 33 bytes (OI-22)
        assertEquals(setOf("bad-key"), codes(ihe.copy(key = "dGhpcyBpcyBhIHNlY3JldCBrZXkgdXNlZCBmb3IgZW5j")))
        assertEquals(setOf("bad-flag"), codes(ihe.copy(flag = "PL")))
        assertEquals(setOf("unsupported-flag-U"), codes(ihe.copy(flag = "LU")))
        assertEquals(setOf("label-too-long"), codes(ihe.copy(label = "x".repeat(81))))
        assertEquals(setOf("not-https"), codes(ihe.copy(url = "http://vhl-sharer.example.org/List?_id=1")))
        assertEquals(setOf("expired"), codes(ihe, now = 1735689601))
    }

    /** The ITI-YY3 example link, re-encoded from its JSON (upstream it encodes "extensions"). */
    @Test fun vhlinkRoundTripAndTheIheExample() {
        val link = HcertVhl.toVhlink(ihe)
        assertTrue(link.startsWith("vhlink:/eyJ1cmwiOi"))
        assertEquals(ihe, HcertVhl.fromVhlink(link).get())
        val upstream = "vhlink:/eyJ1cmwiOiJodHRwczovL3ZobC1zaGFyZXIuZXhhbXBsZS5vcmcvTGlzdC9fc2VhcmNoP19pZD1hYmMxMjNkZWY0NTYmY29kZT1mb2xkZXImc3RhdHVzPWN1cnJlbnQmcGF0aWVudC5pZGVudGlmaWVyPXVybjpvaWQ6Mi4xNi44NDAuMS4xMTM4ODMuMi40LjYuM3xQQVNTUE9SVDEyMyZfaW5jbHVkZT1MaXN0Oml0ZW0iLCJrZXkiOiI4NkY4TFk1TGxXQWExLU9TX0ZnclRuWU5xRkhKUDJleTVSU0tMSkJOOWprIiwiZXhwIjoxNzM1Njg5NjAwLCJmbGFnIjoiTFAiLCJsYWJlbCI6IlBhdGllbnQgSGVhbHRoIFN1bW1hcnkiLCJ2IjoxLCJleHRlbnNpb25zIjp7ImZoaXJCYXNlVXJsIjoiaHR0cHM6Ly92aGwtc2hhcmVyLmV4YW1wbGUub3JnIn19"
        val p = HcertVhl.fromVhlink(upstream).get()
        assertEquals(ihe.copy(fhirBaseUrl = null), p) // its "extensions" is not the payload's "extension"
        assertIs<VhlError.NotAVhlink>((HcertVhl.fromVhlink("shlink:/abc") as HcertResult.Err).error)
    }

    @Test fun newKeyIs32RandomBytes() {
        val a = HcertVhl.newKey()
        assertEquals(43, a.length)
        assertEquals(32, Base64Std.decode(a).size)
        assertTrue(a != HcertVhl.newKey())
    }
}

/** A VHL carried in an HCERT. [STT-VHL-002] */
class VhlHcertTest {
    private val pem = file("vectors/testkit/es256-dsc.pem")
    private val der = Pem.blocks(pem).single()
    private val key = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(Pem.blocks(file("vectors/testkit/es256-key.pem"), "PRIVATE KEY").single()))
    private val reg = SubclaimRegistry()
    private val store = HcertTrustStore().apply { addPem(pem).get() }
    private val now = Platform.certificate(der).notBefore + 3600
    private val options = VerifierOptions(DEFAULT_LIMITS, 300)
    private val payload = ihe.copy(exp = now + 86400)

    private fun issue(p: Payload): ByteArray {
        val pending = HcertIssuer.prepare(IssueRequest(p, "XX", Validity(now, now + 86400), der, CoseAlgorithm.ES256, false), reg).get()
        val sig = Signature.getInstance("SHA256withECDSA").run { initSign(key); update(pending.toBeSigned()); sign() }
        return HcertIssuer.finish(pending, sig).get().cose
    }

    private fun key5(cose: ByteArray): CborItem {
        val s = HcertCose.parseSign1(cose, DEFAULT_LIMITS).get()
        val claims = cborDecode(s.payload, DEFAULT_LIMITS) as CborItem.Map
        return (claims[-260L] as CborItem.Map)[5L]!!
    }

    @Test fun issuedAsACborMapAndVerified() {
        val cose = issue(Payload.Vhl(payload))
        assertIs<CborItem.Map>(key5(cose))
        val v = assertIs<VerificationResult.Verified>(HcertVerifier.verify(HcertCodec.toHc1(cose), store, reg, now, options)).value
        assertEquals(5, v.subclaim)
        assertEquals(payload, (v.payload as Payload.Vhl).value)
    }

    /** IHE ITI-YY3's vhlink:/ string at key 5 still verifies, and the map is smaller. */
    @Test fun aVhlinkStringAtKey5StillVerifies() {
        val text = CborEncoder.encode(CborItem.Text(HcertVhl.toVhlink(payload)))
        val cose = issue(Payload.Raw(RawPayload(5, text)))
        assertIs<CborItem.Text>(key5(cose))
        val v = assertIs<VerificationResult.Verified>(HcertVerifier.verify(HcertCodec.toHc1(cose), store, reg, now, options)).value
        assertEquals(payload, (v.payload as Payload.Vhl).value)
        val asMap = HcertCodec.toHc1(issue(Payload.Vhl(payload))).length
        val asText = HcertCodec.toHc1(cose).length
        assertTrue(asMap < asText * 0.8, "map $asMap vs text $asText characters")
    }

    @Test fun anExpiredLinkFails() {
        val cose = issue(Payload.Vhl(payload.copy(exp = now - 1000)))
        val f = assertIs<VerificationResult.Failed>(HcertVerifier.verify(HcertCodec.toHc1(cose), store, reg, now, options)).value
        assertEquals(FailureReason.EXPIRED, f.reason)
    }
}

/** ITI-YY5. [STT-VHL-005, STT-VHL-006] */
class VhlManifestTest {
    /** The exact body of the ITI-YY5 HTTP Message Signatures example. */
    @Test fun requestMatchesTheIheExample() {
        val r = HcertVhl.buildManifestRequest(ihe, "Dr. Smith Hospital", "user-pin", 10000).get()
        assertEquals("https://vhl-sharer.example.org/List/_search", r.url)
        assertEquals("application/x-www-form-urlencoded", r.contentType)
        assertEquals(
            "_id=abc123def456&code=folder&status=current&patient.identifier=urn%3Aoid%3A2.16.840.1.113883.2.4.6.3%7CPASSPORT123&_include=List%3Aitem&recipient=Dr.+Smith+Hospital&passcode=user-pin&embeddedLengthMax=10000",
            r.body,
        )
    }

    @Test fun listUrlFormAndRefusals() {
        val list = ihe.copy(url = "https://vhl-sharer.example.org/List?_id=abc&code=folder&status=current&patient.identifier=s%7Cv", flag = null)
        assertEquals("https://vhl-sharer.example.org/List/_search", HcertVhl.buildManifestRequest(list, "Clinic B", null, null).get().url)
        assertIs<VhlError.Manifest>((HcertVhl.buildManifestRequest(ihe, "Clinic B", null, null) as HcertResult.Err).error) // P without passcode
        assertIs<VhlError.Manifest>((HcertVhl.buildManifestRequest(list, "Clinic B", "1234", null) as HcertResult.Err).error) // passcode without P
        val noId = list.copy(url = "https://vhl-sharer.example.org/List?code=folder&status=current&patient.identifier=s%7Cv")
        assertIs<VhlError.Manifest>((HcertVhl.buildManifestRequest(noId, "Clinic B", null, null) as HcertResult.Err).error)
    }

    @Test fun caseBBundleFromIhe() {
        val docs = HcertVhl.parseManifest(file("vectors/ihe-vhl/iti-yy5-manifest-case-b.json")).get()
        assertEquals(4, docs.size)
        assertEquals("https://vhl-sharer.example.org/Binary/doc001-content", docs[0].url)
        assertEquals("application/pdf", docs[0].contentType)
        // the example abbreviates doc002..doc004 to bare ids: handed back as references
        assertEquals(listOf("DocumentReference/doc002", "DocumentReference/doc003", "DocumentReference/doc004"), docs.drop(1).map { it.reference })
    }

    @Test fun caseAWithoutIncludes() {
        val bundle = """{"resourceType":"Bundle","type":"searchset","entry":[{"resource":{"resourceType":"List","entry":[{"item":{"reference":"DocumentReference/d1"}}]},"search":{"mode":"match"}}]}"""
        val d = HcertVhl.parseManifest(bundle).get().single()
        assertEquals("DocumentReference/d1", d.reference)
        assertNull(d.url)
        assertIs<VhlError.Manifest>((HcertVhl.parseManifest("""{"resourceType":"Bundle","type":"document"}""") as HcertResult.Err).error)
    }
}

/** JWE dir / A256GCM, checked against node:crypto. [STT-VHL-007] */
class VhlJweTest {
    private val fixture = parseJson(file("vectors/ihe-vhl/jwe-dir-a256gcm.json")) as Map<*, *>
    private val key = fixture["key"] as String

    @Test fun decryptsWhatNodeEncrypted() {
        for (k in listOf("jwe", "jweZipDef")) {
            val d = HcertVhl.decrypt(fixture[k] as String, key).get()
            assertEquals(fixture["plaintext"], d.content.decodeToString(), k)
            assertEquals("application/fhir+json", d.contentType)
        }
    }

    @Test fun roundTripAndRefusals() {
        val jwe = HcertVhl.encrypt("hello".encodeToByteArray(), "text/plain", key).get()
        assertEquals("", jwe.split('.')[1])
        assertEquals("hello", HcertVhl.decrypt(jwe, key).get().content.decodeToString())
        assertIs<VhlError.DecryptFailed>((HcertVhl.decrypt(jwe, HcertVhl.newKey()) as HcertResult.Err).error)
        val parts = jwe.split('.').toMutableList()
        parts[3] = Base64Std.encodeUrl(Base64Std.decode(parts[3]).also { it[0] = (it[0].toInt() xor 1).toByte() })
        assertIs<VhlError.DecryptFailed>((HcertVhl.decrypt(parts.joinToString("."), key) as HcertResult.Err).error)
        val a128 = Base64Std.encodeUrl("""{"alg":"dir","enc":"A128GCM"}""".encodeToByteArray()) + ".." + jwe.substringAfter("..")
        assertIs<VhlError.Unsupported>((HcertVhl.decrypt(a128, key) as HcertResult.Err).error)
    }
}

/** RFC 9421 / RFC 9530. [STT-VHL-008] */
class VhlSignatureTest {
    private val body = """{"hello": "world"}""".encodeToByteArray()
    private val request = SignatureRequest("POST", "vhl-sharer.example.org", "/List/_search", "application/x-www-form-urlencoded", body, "receiver-key-123", "ecdsa-p256-sha256", 1735689600)

    /** RFC 9530 appendix B.1's sha-256 of {"hello": "world"}: the value IHE's example shows without the colons (OI-26). */
    @Test fun contentDigestIsRfc9530() {
        assertEquals("sha-256=:X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=:", HcertVhl.buildSignatureBase(request).contentDigest)
    }

    @Test fun signatureBaseHasTheIheShape() {
        val b = HcertVhl.buildSignatureBase(request)
        val params = """("@method" "@path" "@authority" "content-type" "content-digest");created=1735689600;keyid="receiver-key-123";alg="ecdsa-p256-sha256""""
        assertEquals("sig1=$params", b.signatureInput)
        assertEquals(
            listOf(
                "\"@method\": POST",
                "\"@path\": /List/_search",
                "\"@authority\": vhl-sharer.example.org",
                "\"content-type\": application/x-www-form-urlencoded",
                "\"content-digest\": sha-256=:X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=:",
                "\"@signature-params\": $params",
            ).joinToString("\n"),
            b.base.decodeToString(),
        )
    }

    @Test fun derSignatureBecomesRawAndVerifies() {
        val b = HcertVhl.buildSignatureBase(request)
        val kp = java.security.KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val der = Signature.getInstance("SHA256withECDSA").run { initSign(kp.private); update(b.base); sign() }
        val header = HcertVhl.signatureHeader(der, "ecdsa-p256-sha256").get()
        val raw = Base64Std.decode(header.removePrefix("sig1=:").removeSuffix(":"))
        assertEquals(64, raw.size)
        assertTrue(Signature.getInstance("SHA256withECDSA").run { initVerify(kp.public); update(b.base); verify(EcdsaSignature.rawToDer(raw)) })
    }
}
