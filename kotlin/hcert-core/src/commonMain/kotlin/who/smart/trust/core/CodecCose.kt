package who.smart.trust.core

import who.smart.trust.WitResult
import who.smart.trust.codec.Codec
import who.smart.trust.codec.CodecError
import who.smart.trust.cose.Base64Alphabet
import who.smart.trust.cose.Cose
import who.smart.trust.cose.CoseError
import who.smart.trust.cose.Sign1
import who.smart.trust.types.CborBytes
import who.smart.trust.types.CoseAlgorithm
import who.smart.trust.types.DecodeLimits
import who.smart.trust.types.Kid

/** A decode bound was hit. */
class LimitException(message: String) : Exception(message)

/** Bounds for untrusted input. An HC1 string in a version 40 QR symbol is at most 4296 characters. */
val DEFAULT_LIMITS = DecodeLimits(
    maxInputChars = 8192,
    maxInflatedBytes = 65536,
    maxDepth = 16,
    maxItems = 4096,
    maxStringBytes = 16384,
)

internal const val COSE_SIGN1_TAG = 18L

internal fun CoseAlgorithm.label(): Long = when (this) {
    CoseAlgorithm.ES256 -> -7
    CoseAlgorithm.PS256 -> -37
}

internal fun algorithmOf(label: Long): CoseAlgorithm? = when (label) {
    -7L -> CoseAlgorithm.ES256
    -37L -> CoseAlgorithm.PS256
    else -> null
}

/** Steps 4, 6, 7, 8. [STT-GEN-002] */
object HcertCodec : Codec {
    private val context = Regex("^HC([1-9A-Z]):")

    override fun base45Encode(data: ByteArray): String = Base45.encode(data)

    override fun base45Decode(text: String): WitResult<ByteArray, CodecError> = try {
        WitResult.Ok(Base45.decode(text))
    } catch (e: IllegalArgumentException) {
        WitResult.Err(CodecError.Base45Invalid(e.message ?: "invalid Base45"))
    }

    override fun zlibCompress(data: ByteArray, level: Int): ByteArray = Platform.deflate(data, level)

    override fun zlibInflate(data: ByteArray, maxOut: Int): WitResult<ByteArray, CodecError> = try {
        WitResult.Ok(Platform.inflate(data, maxOut))
    } catch (e: LimitException) {
        WitResult.Err(CodecError.LimitExceeded(e.message ?: "inflate limit"))
    } catch (e: IllegalArgumentException) {
        WitResult.Err(CodecError.InflateFailed(e.message ?: "invalid ZLIB data"))
    }

    override fun detectContext(text: String): WitResult<Pair<String, String>, CodecError> {
        val m = context.find(text) ?: return WitResult.Err(CodecError.BadPrefix)
        return WitResult.Ok(m.groupValues[1] to text.substring(m.range.last + 1))
    }

    override fun toHc1(cose: ByteArray): String = "HC1:" + Base45.encode(Platform.deflate(cose, 9))

    override fun fromHc1(text: String, limits: DecodeLimits): WitResult<ByteArray, CodecError> {
        if (text.length > limits.maxInputChars) {
            return WitResult.Err(CodecError.LimitExceeded("input of ${text.length} characters exceeds ${limits.maxInputChars}"))
        }
        val (version, rest) = when (val c = detectContext(text)) {
            is WitResult.Ok -> c.value
            is WitResult.Err -> return c
        }
        if (version != "1") return WitResult.Err(CodecError.UnsupportedContextVersion("HC$version"))
        val deflated = when (val b = base45Decode(rest)) {
            is WitResult.Ok -> b.value
            is WitResult.Err -> return b
        }
        return zlibInflate(deflated, limits.maxInflatedBytes)
    }

    override fun cborDeterministic(cbor: CborBytes, limits: DecodeLimits): WitResult<CborBytes, CodecError> = try {
        WitResult.Ok(CborEncoder.encode(cborDecode(cbor, limits)))
    } catch (e: CborException) {
        WitResult.Err(if (e.limit) CodecError.LimitExceeded(e.message!!) else CodecError.CborMalformed(e.message!!))
    }
}

/** Step 5. [STT-COS-001 to STT-COS-005, STT-KID-001, STT-KID-003, STT-VER-003] */
object HcertCose : Cose {
    override fun kidFromDsc(dscDer: ByteArray): Kid = Platform.sha256(dscDer).copyOf(8)

    override fun kidToBase64(k: Kid, alphabet: Base64Alphabet): String = when (alphabet) {
        Base64Alphabet.STANDARD -> Base64Std.encode(k)
        Base64Alphabet.URL_SAFE -> Base64Std.encodeUrl(k)
    }

    override fun kidFromBase64(text: String): WitResult<Kid, CoseError> = try {
        WitResult.Ok(Base64Std.decode(text))
    } catch (e: IllegalArgumentException) {
        WitResult.Err(CoseError.Malformed("kid is not base64"))
    }

    override fun parseSign1(bytes: ByteArray, limits: DecodeLimits): WitResult<Sign1, CoseError> {
        val item = try {
            cborDecode(bytes, limits)
        } catch (e: CborException) {
            return WitResult.Err(CoseError.Malformed(e.message!!))
        }
        val tagged = item is CborItem.Tag
        if (item is CborItem.Tag && item.tag != COSE_SIGN1_TAG) return WitResult.Err(CoseError.Malformed("tag ${item.tag} is not COSE_Sign1 (18)"))
        val arr = (if (item is CborItem.Tag) item.item else item) as? CborItem.Array
            ?: return WitResult.Err(CoseError.Malformed("COSE_Sign1 is not an array"))
        if (arr.items.size != 4) return WitResult.Err(CoseError.Malformed("COSE_Sign1 has ${arr.items.size} elements, not 4"))
        val protected = (arr.items[0] as? CborItem.Bytes)?.value ?: return WitResult.Err(CoseError.Malformed("protected header is not a byte string"))
        val unprotected = arr.items[1] as? CborItem.Map ?: return WitResult.Err(CoseError.Malformed("unprotected header is not a map"))
        val payload = (arr.items[2] as? CborItem.Bytes)?.value ?: return WitResult.Err(CoseError.Malformed("payload is not a byte string"))
        val signature = (arr.items[3] as? CborItem.Bytes)?.value ?: return WitResult.Err(CoseError.Malformed("signature is not a byte string"))

        val ph = if (protected.isEmpty()) CborItem.Map(emptyList()) else try {
            cborDecode(protected, limits) as? CborItem.Map ?: return WitResult.Err(CoseError.HeaderInvalid("protected header is not a map"))
        } catch (e: CborException) {
            return WitResult.Err(CoseError.HeaderInvalid(e.message!!))
        }
        for (h in listOf(ph, unprotected)) {
            if (h.entries.map { it.first }.toSet().size != h.entries.size) return WitResult.Err(CoseError.HeaderInvalid("duplicate header label"))
        }
        if (unprotected[1L] != null) return WitResult.Err(CoseError.HeaderInvalid("alg must be in the protected header"))
        val algItem = ph[1L] ?: return WitResult.Err(CoseError.HeaderInvalid("no alg in the protected header"))
        val algLabel = (algItem as? CborItem.Int)?.value ?: return WitResult.Err(CoseError.HeaderInvalid("alg is not an integer"))
        val alg = algorithmOf(algLabel) ?: return WitResult.Err(CoseError.UnsupportedAlgorithm(algLabel.toInt()))
        fun kidOf(h: CborItem.Map): WitResult<ByteArray?, CoseError> {
            val k = h[4L] ?: return WitResult.Ok(null)
            val b = (k as? CborItem.Bytes)?.value ?: return WitResult.Err(CoseError.HeaderInvalid("kid is not a byte string"))
            if (b.size != 8) return WitResult.Err(CoseError.HeaderInvalid("kid is ${b.size} bytes, not 8"))
            return WitResult.Ok(b)
        }
        val pk = when (val r = kidOf(ph)) { is WitResult.Ok -> r.value; is WitResult.Err -> return r }
        val uk = when (val r = kidOf(unprotected)) { is WitResult.Ok -> r.value; is WitResult.Err -> return r }
        return WitResult.Ok(Sign1(protected, payload, signature, alg, pk ?: uk, kidInProtected = pk != null, tagged = tagged))
    }

    override fun sigStructure(protected: ByteArray, payload: ByteArray): ByteArray = CborEncoder.encode(
        CborItem.Array(listOf(CborItem.Text("Signature1"), CborItem.Bytes(protected), CborItem.Bytes(ByteArray(0)), CborItem.Bytes(payload))),
    )

    override fun verifySign1(s: Sign1, dscDer: ByteArray): WitResult<Unit, CoseError> {
        val alg = s.alg ?: return WitResult.Err(CoseError.HeaderInvalid("no alg"))
        if (alg == CoseAlgorithm.ES256 && s.signature.size != 64) {
            return WitResult.Err(CoseError.BadSignatureFormat("ES256 signature is ${s.signature.size} bytes, not 64"))
        }
        try {
            Platform.certificate(dscDer)
        } catch (e: Exception) {
            return WitResult.Err(CoseError.BadCertificate(e.message ?: "unreadable certificate"))
        }
        val ok = Platform.verify(alg, dscDer, sigStructure(s.protected, s.payload), s.signature)
        return if (ok) WitResult.Ok(Unit) else WitResult.Err(CoseError.SignatureInvalid)
    }

    override fun ecdsaDerToRaw(der: ByteArray): WitResult<ByteArray, CoseError> = try {
        WitResult.Ok(EcdsaSignature.derToRaw(der))
    } catch (e: IllegalArgumentException) {
        WitResult.Err(CoseError.BadSignatureFormat(e.message ?: "bad DER"))
    }

    override fun ecdsaRawToDer(raw: ByteArray): WitResult<ByteArray, CoseError> = try {
        WitResult.Ok(EcdsaSignature.rawToDer(raw))
    } catch (e: IllegalArgumentException) {
        WitResult.Err(CoseError.BadSignatureFormat(e.message ?: "bad r||s"))
    }
}
