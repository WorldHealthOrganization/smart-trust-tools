package who.smart.trust.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import who.smart.trust.types.FailureReason
import who.smart.trust.verifier.VerificationResult

/** Skill `hcert-encode-cbor`: QR Code Generation step 4. */
class EncodeCborTest {
    @Test fun `STT-CBR-001 maps are encoded in deterministic key order, whatever order they were built in`() {
        val a = CborItem.Map(listOf(CborItem.Text("b") to CborItem.Int(2), CborItem.Int(-1) to CborItem.Int(1), CborItem.Int(10) to CborItem.Int(3)))
        val b = CborItem.Map(a.entries.reversed())
        assertContentEquals(CborEncoder.encode(a), CborEncoder.encode(b))
        // RFC 8949 4.2.1: shorter encoded keys first, then bytewise: 10, -1, "b"
        assertEquals(listOf(CborItem.Int(10), CborItem.Int(-1), CborItem.Text("b")), (cborDecode(CborEncoder.encode(a), DEFAULT_LIMITS) as CborItem.Map).entries.map { it.first })
    }

    @Test fun `STT-CBR-002 text is NFC-normalised on encode, and a non-normalised string still decodes`() {
        val decomposed = "Hoà" // a + combining grave
        val encoded = CborEncoder.encode(CborItem.Text(decomposed))
        assertEquals(CborItem.Text("Hoà"), cborDecode(encoded, DEFAULT_LIMITS))
        val utf8 = decomposed.encodeToByteArray()
        val raw = byteArrayOf((0x60 + utf8.size).toByte()) + utf8 // as another encoder may send it
        assertEquals(CborItem.Text(decomposed), cborDecode(raw, DEFAULT_LIMITS))
    }

    @Test fun `STT-CBR-003 nesting beyond the limit is refused`() {
        val nested = ByteArray(40) { 0x81.toByte() } + byteArrayOf(0)
        val f = assertIs<VerificationResult.Failed>(HcertVerifier.verifyCose(nested, HcertTrustStore(), SubclaimRegistry(), NOW, OPTIONS)).value
        assertEquals(FailureReason.LIMIT_EXCEEDED, f.reason)
    }
}
