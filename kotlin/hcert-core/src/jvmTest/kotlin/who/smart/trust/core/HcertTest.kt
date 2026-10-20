package who.smart.trust.core

import java.io.File
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.PSSParameterSpec
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import who.smart.trust.WitResult
import who.smart.trust.dvc.DvcMin
import who.smart.trust.dvc.DvcMinVaccineDetails
import who.smart.trust.issuer.IssueError
import who.smart.trust.issuer.IssueRequest
import who.smart.trust.issuer.IssuedHcert
import who.smart.trust.issuer.Validity
import who.smart.trust.payloads.Payload
import who.smart.trust.payloads.RawPayload
import who.smart.trust.types.CoseAlgorithm
import who.smart.trust.types.FailureReason
import who.smart.trust.types.Step
import who.smart.trust.verifier.VerificationResult
import who.smart.trust.verifier.VerifierOptions

private val root = File(System.getProperty("repo.root"))
private fun read(path: String) = File(root, path).readText()

private val options = VerifierOptions(DEFAULT_LIMITS, clockSkewSeconds = 300)
/** 2026-10-20T00:00:00Z, inside the PR 129 vector's validity. */
private const val NOW = 1792454400L

private fun <T, E> WitResult<T, E>.ok(): T = when (this) {
    is WitResult.Ok -> value
    is WitResult.Err -> throw AssertionError("expected Ok, got $error")
}

/** QR Code Generation 12: the PR 129 DVC example, verified as published. [STT-TST-001] */
class Pr129VectorTest {
    private val vector = parseJson(read("vectors/smart-trust-pr129/dvc-qr-example-XXA.json")) as Map<*, *>
    private val certs = vector["certificates"] as Map<*, *>
    private val store = HcertTrustStore().apply { addPem(read("vectors/smart-trust-pr129/dvc-qr-example-XXA-dsc.pem")).ok() }
    private val reg = SubclaimRegistry()

    private fun hc1(name: String) = (certs[name] as Map<*, *>)["hc1"] as String

    @Test fun kidMatchesTheVector() {
        assertEquals("58733e058aaa6621", store.candidates(Base64Std.decode("WHM+BYqqZiE=")).single().kid.toHex())
    }

    @Test fun bothCertificatesVerifyAndDecode() {
        for (name in listOf("hepb1", "bcg")) {
            val r = HcertVerifier.verify(hc1(name), store, reg, NOW, options)
            val v = assertIs<VerificationResult.Verified>(r, "$name: $r").value
            assertEquals("XX", v.iss)
            assertEquals(-6, v.subclaim)
            val p = assertIs<Payload.DvcMin>(v.payload).value
            val expected = (certs[name] as Map<*, *>)["payload"] as Map<*, *>
            assertEquals(expected["n"], p.n)
            assertEquals(expected["nt"], p.nt)
            assertEquals((expected["vx"] as Map<*, *>)["vp"], p.vx.vp)
            assertEquals((expected["vx"] as Map<*, *>)["bo"], p.vx.bo)
            assertTrue(v.diagnostics.isEmpty(), v.diagnostics.toString())
        }
    }

    /** Deterministic re-encoding reproduces the issued CWT byte for byte. [STT-TST-002, STT-CBR-001] */
    @Test fun cwtReencodesByteForByte() {
        val cose = HcertCodec.fromHc1(hc1("hepb1"), DEFAULT_LIMITS).ok()
        val s = HcertCose.parseSign1(cose, DEFAULT_LIMITS).ok()
        assertContentEquals(s.payload, HcertCodec.cborDeterministic(s.payload, DEFAULT_LIMITS).ok())
        assertEquals(210, s.payload.size)
        assertEquals(295, cose.size)
    }

    /** J2: a CHW device with no trust list reads the payload, marked unverified. [STT-VER-011] */
    @Test fun unverifiedReadForTriage() {
        val u = HcertVerifier.readUnverified(hc1("hepb1"), reg, DEFAULT_LIMITS).ok()
        assertEquals("58733e058aaa6621", u.kid!!.toHex())
        assertEquals(-6, u.subclaim)
        assertEquals("XM9V38", (u.payload as Payload.DvcMin).value.vx.vp)
        assertTrue(u.diagnostics.first().message.startsWith("NOT VERIFIED"))
        val bad = HcertVerifier.readUnverified("HC1:abc", reg, DEFAULT_LIMITS)
        assertEquals(FailureReason.BASE45_INVALID, (bad as WitResult.Err).error.reason)
    }

    @Test fun expiredAndNotYetValid() {
        val late = HcertVerifier.verify(hc1("bcg"), store, reg, 1822206600L + 301, options)
        assertEquals(FailureReason.EXPIRED, assertIs<VerificationResult.Failed>(late).value.reason)
        val early = HcertVerifier.verify(hc1("bcg"), store, reg, 1790670600L - 301, options)
        assertEquals(FailureReason.NOT_YET_VALID, assertIs<VerificationResult.Failed>(early).value.reason)
    }

    @Test fun unknownKid() {
        val f = assertIs<VerificationResult.Failed>(HcertVerifier.verify(hc1("bcg"), HcertTrustStore(), reg, NOW, options)).value
        assertEquals(Step.TRUST_LOOKUP to FailureReason.KID_NOT_FOUND, f.step to f.reason)
    }

    @Test fun tamperedSignature() {
        val cose = HcertCodec.fromHc1(hc1("bcg"), DEFAULT_LIMITS).ok()
        cose[cose.size - 1] = (cose[cose.size - 1].toInt() xor 1).toByte()
        val f = assertIs<VerificationResult.Failed>(HcertVerifier.verify(HcertCodec.toHc1(cose), store, reg, NOW, options)).value
        assertEquals(Step.SIGNATURE to FailureReason.SIGNATURE_INVALID, f.step to f.reason)
    }
}

/** Negative inputs fail at the named step with the named reason. [STT-TST-004, STT-VER-008] */
class NegativeTest {
    private val store = HcertTrustStore()
    private val reg = SubclaimRegistry()
    private fun fail(hc1: String) = assertIs<VerificationResult.Failed>(HcertVerifier.verify(hc1, store, reg, NOW, options)).value

    @Test fun prefixes() {
        assertEquals(FailureReason.BAD_PREFIX, fail("NCFOXN").reason)
        assertEquals(FailureReason.UNSUPPORTED_CONTEXT_VERSION, fail("HC2:NCFOXN").reason)
    }

    @Test fun base45() {
        assertEquals(FailureReason.BASE45_INVALID, fail("HC1:abc").reason)
        assertEquals(FailureReason.BASE45_INVALID, fail("HC1:A").reason)
        assertEquals(FailureReason.BASE45_INVALID, fail("HC1::::").reason)
    }

    @Test fun inflateBomb() {
        val bomb = HcertCodec.toHc1(ByteArray(1_000_000))
        val f = fail(bomb)
        assertEquals(Step.INFLATE to FailureReason.LIMIT_EXCEEDED, f.step to f.reason)
    }

    @Test fun notZlib() = assertEquals(FailureReason.INFLATE_FAILED, fail("HC1:" + Base45.encode(byteArrayOf(1, 2, 3))).reason)

    @Test fun inputTooLong() = assertEquals(FailureReason.LIMIT_EXCEEDED, fail("HC1:" + "0".repeat(9000)).reason)

    @Test fun deepCbor() {
        val nested = ByteArray(40) { 0x81.toByte() } + byteArrayOf(0)
        val f = assertIs<VerificationResult.Failed>(HcertVerifier.verifyCose(nested, store, reg, NOW, options)).value
        assertEquals(FailureReason.LIMIT_EXCEEDED, f.reason)
    }

    @Test fun unsupportedAlgorithm() {
        val protected = CborEncoder.encode(CborItem.Map(listOf(CborItem.Int(1) to CborItem.Int(-8))))
        val cose = CborEncoder.encode(CborItem.Array(listOf(CborItem.Bytes(protected), CborItem.Map(emptyList()), CborItem.Bytes(byteArrayOf()), CborItem.Bytes(byteArrayOf()))))
        val f = assertIs<VerificationResult.Failed>(HcertVerifier.verifyCose(cose, store, reg, NOW, options)).value
        assertEquals(Step.HEADER to FailureReason.UNSUPPORTED_ALGORITHM, f.step to f.reason)
    }
}

/** RFC 9285 section 4.3 examples. [STT-B45-001] */
class Base45Test {
    @Test fun rfcExamples() {
        assertEquals("BB8", Base45.encode("AB".encodeToByteArray()))
        assertEquals("%69 VD92EX0", Base45.encode("Hello!!".encodeToByteArray()))
        assertEquals("UJCLQE7W581", Base45.encode("base-45".encodeToByteArray()))
        assertEquals("ietf!", Base45.decode("QED8WEX0").decodeToString())
    }
}

class EcdsaTest {
    @Test fun roundTrip() {
        val raw = ByteArray(64) { (it * 7 + 0x80).toByte() }
        assertContentEquals(raw, EcdsaSignature.derToRaw(EcdsaSignature.rawToDer(raw)))
        val small = ByteArray(64).also { it[31] = 1; it[63] = 2 }
        assertContentEquals(small, EcdsaSignature.derToRaw(EcdsaSignature.rawToDer(small)))
    }
}

/** A software signer, for tests only. [STT-COS-006, STT-ISS-003] */
private class TestSigner(keyPem: String, private val alg: CoseAlgorithm) {
    private val key = KeyFactory.getInstance(if (alg == CoseAlgorithm.ES256) "EC" else "RSA")
        .generatePrivate(PKCS8EncodedKeySpec(Pem.blocks(keyPem, "PRIVATE KEY").single()))

    fun sign(tbs: ByteArray): ByteArray = when (alg) {
        CoseAlgorithm.ES256 -> Signature.getInstance("SHA256withECDSA").run { initSign(key); update(tbs); sign() } // DER
        CoseAlgorithm.PS256 -> Signature.getInstance("RSASSA-PSS").run {
            setParameter(PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1))
            initSign(key); update(tbs); sign()
        }
    }
}

/** prepare -> sign anywhere -> finish, then verify. [STT-ISS-004, STT-GEN-001] */
class IssueVerifyTest {
    private val es = read("vectors/testkit/es256-dsc.pem")
    private val ps = read("vectors/testkit/ps256-dsc-test-eku.pem")
    private val esDer = Pem.blocks(es).single()
    private val psDer = Pem.blocks(ps).single()
    private val reg = SubclaimRegistry()
    private val now = Platform.certificate(esDer).notBefore + 3600

    private val dvc = DvcMin(
        n = "Test CHILD", dob = "2026-09-28", s = "female", nt = "LAO", gn = "Test MOTHER",
        vx = DvcMinVaccineDetails(vp = "XM9V38", dv = "2026-09-28", `is` = "org-test", bo = "HEPB-TEST"),
        v = "1.0.0", extensions = listOf("zz" to CborEncoder.encode(CborItem.Text("kept"))),
    )

    private fun request(payload: Payload, der: ByteArray, alg: CoseAlgorithm, dev: Boolean = true) =
        IssueRequest(payload, "XX", Validity(now, now + 86400), der, alg, allowDevelopmentSubclaims = dev)

    private fun issue(payload: Payload, der: ByteArray, keyFile: String, alg: CoseAlgorithm): IssuedHcert {
        val pending = HcertIssuer.prepare(request(payload, der, alg), reg).ok()
        return HcertIssuer.finish(pending, TestSigner(read(keyFile), alg).sign(pending.toBeSigned())).ok()
    }

    @Test fun es256DvcRoundTrip() {
        val issued = issue(Payload.DvcMin(dvc), esDer, "vectors/testkit/es256-key.pem", CoseAlgorithm.ES256)
        assertTrue(issued.hc1.startsWith("HC1:"))
        assertTrue(issued.hc1.all { it in Base45.ALPHABET || it == ':' }, "HC1 is QR Alphanumeric only") // [STT-QR-001]
        val store = HcertTrustStore().apply { addPem(es).ok() }
        val v = assertIs<VerificationResult.Verified>(HcertVerifier.verify(issued.hc1, store, reg, now, options)).value
        val got = (v.payload as Payload.DvcMin).value
        assertEquals(dvc.copy(extensions = emptyList()), got.copy(extensions = emptyList()))
        assertEquals(listOf("zz"), got.extensions.map { it.first }) // unknown element kept [STT-PAY-008]
        assertContentEquals(dvc.extensions.single().second, got.extensions.single().second)
        val s = HcertCose.parseSign1(issued.cose, DEFAULT_LIMITS).ok()
        assertEquals(64, s.signature.size)
        assertTrue(s.tagged && s.kidInProtected)
    }

    @Test fun ps256RawPayloadWithTestEku() {
        val body = CborEncoder.encode(CborItem.Map(listOf(CborItem.Text("t") to CborItem.Text("x"))))
        val issued = issue(Payload.Raw(RawPayload(4, body)), psDer, "vectors/testkit/ps256-key.pem", CoseAlgorithm.PS256)
        val store = HcertTrustStore().apply { addPem(ps).ok() }
        val v = assertIs<VerificationResult.Verified>(HcertVerifier.verify(issued.hc1, store, reg, now, options)).value
        assertContentEquals(body, (v.payload as Payload.Raw).value.cbor)
    }

    /** A DVC under a DSC whose EKU allows only test certificates. [STT-VER-004] */
    @Test fun ekuMismatch() {
        val issued = issue(Payload.DvcMin(dvc), psDer, "vectors/testkit/ps256-key.pem", CoseAlgorithm.PS256)
        val store = HcertTrustStore().apply { addPem(ps).ok() }
        val f = assertIs<VerificationResult.Failed>(HcertVerifier.verify(issued.hc1, store, reg, now, options)).value
        assertEquals(Step.KEY_USAGE to FailureReason.KEY_USAGE_MISMATCH, f.step to f.reason)
    }

    /** A DCC (subclaim 1) is a test, vaccination or recovery certificate by its own t, v or r entry. [STT-VER-004] */
    @Test fun dccCertificateTypeFromItsContent() {
        fun dcc(entry: String) = CborEncoder.encode(CborItem.Map(listOf(CborItem.Text(entry) to CborItem.Array(listOf(CborItem.Map(emptyList()))))))
        val store = HcertTrustStore().apply { addPem(ps).ok() } // EKU: test only
        val test = issue(Payload.Raw(RawPayload(1, dcc("t"))), psDer, "vectors/testkit/ps256-key.pem", CoseAlgorithm.PS256)
        assertIs<VerificationResult.Verified>(HcertVerifier.verify(test.hc1, store, reg, now, options))
        val vacc = issue(Payload.Raw(RawPayload(1, dcc("v"))), psDer, "vectors/testkit/ps256-key.pem", CoseAlgorithm.PS256)
        val f = assertIs<VerificationResult.Failed>(HcertVerifier.verify(vacc.hc1, store, reg, now, options)).value
        assertEquals(FailureReason.KEY_USAGE_MISMATCH, f.reason)
    }

    @Test fun developmentSubclaimNeedsOptIn() {
        val r = HcertIssuer.prepare(request(Payload.DvcMin(dvc), esDer, CoseAlgorithm.ES256, dev = false), reg)
        assertEquals(IssueError.DevelopmentSubclaimNotAllowed(-6), (r as WitResult.Err).error)
    }

    @Test fun validityMustFitTheDsc() {
        val req = request(Payload.DvcMin(dvc), esDer, CoseAlgorithm.ES256).copy(validity = Validity(now, now + 20L * 365 * 86400))
        assertEquals(IssueError.ValidityOutsideDsc, (HcertIssuer.prepare(req, reg) as WitResult.Err).error)
    }

    @Test fun invalidPayloadIsRefused() {
        val bad = dvc.copy(nt = "LA", vx = dvc.vx.copy(`is` = null, cn = null))
        val e = (HcertIssuer.prepare(request(Payload.DvcMin(bad), esDer, CoseAlgorithm.ES256), reg) as WitResult.Err).error
        val codes = (e as IssueError.PayloadInvalid).value.map { it.code }.toSet()
        assertEquals(setOf("not-alpha3", "must-have-issuer-or-clinician-name"), codes)
    }

    @Test fun signatureFromAnotherKeyIsRefused() {
        val pending = HcertIssuer.prepare(request(Payload.DvcMin(dvc), esDer, CoseAlgorithm.ES256), reg).ok()
        val other = java.security.KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val sig = Signature.getInstance("SHA256withECDSA").run { initSign(other.private); update(pending.toBeSigned()); sign() }
        assertIs<IssueError.BadSignatureFormat>((HcertIssuer.finish(pending, sig) as WitResult.Err).error)
    }

    @Test fun didDocumentLoadsTheDsc() {
        val did = """{"id":"did:web:tng-cdn-dev.who.int:v2:trustlist:-:XXA:DSC","verificationMethod":[
          {"id":"did:web:tng-cdn-dev.who.int:v2:trustlist:-:XXA:DSC#k1","type":"JsonWebKey2020",
           "publicKeyJwk":{"kty":"EC","crv":"P-256","x5c":["${Base64Std.encode(esDer)}"]}},
          "did:web:tng-cdn-dev.who.int:v2:trustlist:-:XXB:DSC"]}"""
        val store = HcertTrustStore()
        assertEquals(1, store.addDidDocument(did).ok())
        val d = store.candidates(HcertCose.kidFromDsc(esDer)).single()
        assertEquals(null to "XXA", d.domain to d.participant)
        assertEquals("XX", d.country)
    }
}
