package who.smart.trust.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import who.smart.trust.payloads.Payload
import who.smart.trust.types.FailureReason
import who.smart.trust.types.Step
import who.smart.trust.verifier.VerificationResult

/** Skill `hcert-conformance-testing`: the shared vectors (QR Code Generation 12). */
class ConformanceVectorsTest {
    private val vector = parseJson(readRepo("vectors/smart-trust-pr129/dvc-qr-example-XXA.json")) as Map<*, *>
    private val certs = vector["certificates"] as Map<*, *>
    private val store = HcertTrustStore().apply { addPem(readRepo("vectors/smart-trust-pr129/dvc-qr-example-XXA-dsc.pem")).ok() }
    private val reg = SubclaimRegistry()
    private fun hc1(name: String) = (certs[name] as Map<*, *>)["hc1"] as String

    @Test fun `STT-TST-001 STT-KID-001 the vector's kid is the DSC's`() {
        assertEquals("58733e058aaa6621", store.candidates(Base64Std.decode("WHM+BYqqZiE=")).single().kid.toHex())
    }

    @Test fun `STT-TST-001 STT-PAY-001 both PR 129 certificates verify and decode as published`() {
        for (name in listOf("hepb1", "bcg")) {
            val v = assertIs<VerificationResult.Verified>(HcertVerifier.verify(hc1(name), store, reg, NOW, OPTIONS), name).value
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

    @Test fun `STT-TST-002 STT-CBR-001 deterministic re-encoding reproduces the issued CWT byte for byte`() {
        val cose = HcertCodec.fromHc1(hc1("hepb1"), DEFAULT_LIMITS).ok()
        val s = HcertCose.parseSign1(cose, DEFAULT_LIMITS).ok()
        assertContentEquals(s.payload, HcertCodec.cborDeterministic(s.payload, DEFAULT_LIMITS).ok())
        assertEquals(210, s.payload.size)
        assertEquals(295, cose.size)
    }

    @Test fun `STT-TST-004 negative cases fail at the named step with the named reason`() {
        val late = assertIs<VerificationResult.Failed>(HcertVerifier.verify(hc1("bcg"), store, reg, 1822206600L + 301, OPTIONS)).value
        assertEquals(FailureReason.EXPIRED, late.reason)
        val early = assertIs<VerificationResult.Failed>(HcertVerifier.verify(hc1("bcg"), store, reg, 1790670600L - 301, OPTIONS)).value
        assertEquals(FailureReason.NOT_YET_VALID, early.reason)
        val unknown = assertIs<VerificationResult.Failed>(HcertVerifier.verify(hc1("bcg"), HcertTrustStore(), reg, NOW, OPTIONS)).value
        assertEquals(Step.TRUST_LOOKUP to FailureReason.KID_NOT_FOUND, unknown.step to unknown.reason)
        val cose = HcertCodec.fromHc1(hc1("bcg"), DEFAULT_LIMITS).ok()
        cose[cose.size - 1] = (cose[cose.size - 1].toInt() xor 1).toByte()
        val tampered = assertIs<VerificationResult.Failed>(HcertVerifier.verify(HcertCodec.toHc1(cose), store, reg, NOW, OPTIONS)).value
        assertEquals(Step.SIGNATURE to FailureReason.SIGNATURE_INVALID, tampered.step to tampered.reason)
    }
}
