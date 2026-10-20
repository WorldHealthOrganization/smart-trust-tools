package who.smart.trust.core

import who.smart.trust.HcertWorld
import who.smart.trust.WitResult
import who.smart.trust.issuer.IssueError
import who.smart.trust.issuer.IssueRequest
import who.smart.trust.issuer.IssuedHcert
import who.smart.trust.issuer.Issuer
import who.smart.trust.issuer.PendingSignature
import who.smart.trust.payloads.Payload
import who.smart.trust.payloads.RawPayload
import who.smart.trust.subclaims.PayloadKind
import who.smart.trust.subclaims.Registry
import who.smart.trust.trust.TrustStore
import who.smart.trust.trust.TrustedDsc
import who.smart.trust.types.CoseAlgorithm
import who.smart.trust.types.Diagnostic
import who.smart.trust.types.FailureReason
import who.smart.trust.types.Kid
import who.smart.trust.types.NumericDate
import who.smart.trust.types.Severity
import who.smart.trust.types.Step
import who.smart.trust.types.ValidationIssue
import who.smart.trust.verifier.UnverifiedHcert
import who.smart.trust.verifier.VerificationFailure
import who.smart.trust.verifier.VerificationResult
import who.smart.trust.verifier.VerifiedHcert
import who.smart.trust.verifier.Verifier
import who.smart.trust.verifier.VerifierOptions

internal object Claim {
    const val ISS = 1L
    const val EXP = 4L
    const val IAT = 6L
    const val HCERT = -260L
}

private val alpha2 = Regex("[A-Z]{2}")

/** Two-phase issuance, steps 3 to 8. [STT-ISS-004, STT-COS-006, STT-CWT-001 to STT-CWT-005, STT-SEL-003, STT-SEL-004] */
object HcertIssuer : Issuer {
    private class Pending(
        val protected: ByteArray,
        val payload: ByteArray,
        val alg: CoseAlgorithm,
        private val kidBytes: Kid,
        val dscDer: ByteArray,
    ) : PendingSignature {
        val tbs = HcertCose.sigStructure(protected, payload)
        override fun toBeSigned(): ByteArray = tbs.copyOf()
        override fun algorithm(): CoseAlgorithm = alg
        override fun kid(): Kid = kidBytes.copyOf()
        override fun close() {}
    }

    private fun invalid(path: String, code: String, message: String) =
        WitResult.Err(IssueError.PayloadInvalid(listOf(ValidationIssue(path, code, message, Severity.BLOCKING))))

    override fun prepare(req: IssueRequest, reg: Registry): WitResult<PendingSignature, IssueError> {
        val (subclaim, body) = when (val p = req.payload) {
            is Payload.DvcMin -> {
                val key = reg.subclaimFor(PayloadKind.DVC_MIN) ?: return WitResult.Err(IssueError.UnknownSubclaim(-6))
                val issues = HcertDvc.validate(p.value).filter { it.severity == Severity.BLOCKING }
                if (issues.isNotEmpty()) return WitResult.Err(IssueError.PayloadInvalid(issues))
                val item = try {
                    HcertDvc.item(p.value)
                } catch (e: Exception) {
                    return invalid("extensions", "invalid-extension", e.message ?: "invalid extension")
                }
                key to item
            }
            is Payload.Raw -> {
                val item = try {
                    cborDecode(p.value.cbor, DEFAULT_LIMITS)
                } catch (e: CborException) {
                    return invalid("", "cbor", e.message!!)
                }
                p.value.subclaim to item
            }
        }
        if (reg.lookup(subclaim) == null) return WitResult.Err(IssueError.UnknownSubclaim(subclaim))
        if (subclaim < 0 && !req.allowDevelopmentSubclaims) return WitResult.Err(IssueError.DevelopmentSubclaimNotAllowed(subclaim))
        req.iss?.let { if (!alpha2.matches(it)) return invalid("iss", "not-alpha2", "iss must be two upper-case letters") }
        if (req.validity.exp <= req.validity.iat) return invalid("exp", "exp-not-after-iat", "exp must be after iat")

        val dsc = try {
            Platform.certificate(req.dscDer)
        } catch (e: Exception) {
            return WitResult.Err(IssueError.BadCertificate(e.message ?: "unreadable certificate"))
        }
        // [STT-CWT-004]
        if (req.validity.iat < dsc.notBefore || req.validity.exp > dsc.notAfter) return WitResult.Err(IssueError.ValidityOutsideDsc)

        val kid = HcertCose.kidFromDsc(req.dscDer)
        val claims = buildList<Pair<CborItem, CborItem>> {
            req.iss?.let { add(CborItem.Int(Claim.ISS) to CborItem.Text(it)) }
            add(CborItem.Int(Claim.EXP) to CborItem.Int(req.validity.exp))
            add(CborItem.Int(Claim.IAT) to CborItem.Int(req.validity.iat))
            add(CborItem.Int(Claim.HCERT) to CborItem.Map(listOf(CborItem.Int(subclaim.toLong()) to body)))
        }
        val payload = CborEncoder.encode(CborItem.Map(claims))
        val protected = CborEncoder.encode(
            CborItem.Map(listOf(CborItem.Int(1) to CborItem.Int(req.alg.label()), CborItem.Int(4) to CborItem.Bytes(kid))),
        )
        return WitResult.Ok(Pending(protected, payload, req.alg, kid, req.dscDer.copyOf()))
    }

    override fun finish(pending: PendingSignature, signature: ByteArray): WitResult<IssuedHcert, IssueError> {
        val p = pending as? Pending ?: return WitResult.Err(IssueError.BadSignatureFormat("not a PendingSignature from prepare"))
        val sig = when (p.alg) {
            CoseAlgorithm.ES256 -> if (signature.size == 64) signature else try {
                EcdsaSignature.derToRaw(signature)
            } catch (e: IllegalArgumentException) {
                return WitResult.Err(IssueError.BadSignatureFormat("ES256 signature is neither 64-byte r||s nor DER: ${e.message}"))
            }
            CoseAlgorithm.PS256 -> signature
        }
        // A signature from the wrong key is caught here, not by the first verifier.
        if (!Platform.verify(p.alg, p.dscDer, p.tbs, sig)) {
            return WitResult.Err(IssueError.BadSignatureFormat("the signature does not verify with the DSC's public key"))
        }
        val cose = CborEncoder.encode(
            CborItem.Tag(
                COSE_SIGN1_TAG,
                CborItem.Array(listOf(CborItem.Bytes(p.protected), CborItem.Map(emptyList()), CborItem.Bytes(p.payload), CborItem.Bytes(sig))),
            ),
        )
        return WitResult.Ok(IssuedHcert(HcertCodec.toHc1(cose), cose))
    }
}

/**
 * QR Code Generation section 11, in the STT-VER-001 order. Every failure is a
 * value naming its step and reason; nothing about an unverified certificate
 * is returned. [STT-VER-001 to STT-VER-008, STT-KID-002, STT-SEL-004, STT-CWT-006]
 */
object HcertVerifier : Verifier {
    /** EKU OIDs (HCERT A.2) and the certificate types they allow. [STT-VER-004] */
    private const val EKU_TEST = "1.3.6.1.4.1.1847.2021.1.1"
    private const val EKU_VACCINATION = "1.3.6.1.4.1.1847.2021.1.2"
    private const val EKU_RECOVERY = "1.3.6.1.4.1.1847.2021.1.3"
    private val hcertEkus = setOf(EKU_TEST, EKU_VACCINATION, EKU_RECOVERY)

    /**
     * The EKU a certificate type needs. DVC (-6) and DDCC VS (3) are
     * vaccinations, DDCC TR (4) is a test; a DCC (1) says which by carrying
     * a v, t or r entry (EU DCC schema).
     */
    private fun ekuFor(subclaim: Int, body: CborItem?): String? = when (subclaim) {
        -6, 3 -> EKU_VACCINATION
        4 -> EKU_TEST
        1 -> (body as? CborItem.Map)?.let { m ->
            listOfNotNull(
                EKU_VACCINATION.takeIf { m["v"] != null },
                EKU_TEST.takeIf { m["t"] != null },
                EKU_RECOVERY.takeIf { m["r"] != null },
            ).singleOrNull()
        }
        else -> null
    }

    private class Fail(val step: Step, val reason: FailureReason, val detail: String) : Exception(detail)

    /** Prefix, Base45 and inflate: HC1 text to COSE bytes. */
    private fun front(hc1: String, limits: who.smart.trust.types.DecodeLimits): ByteArray {
        if (hc1.length > limits.maxInputChars) throw Fail(Step.PREFIX, FailureReason.LIMIT_EXCEEDED, "input exceeds ${limits.maxInputChars} characters")
        val (version, rest) = when (val c = HcertCodec.detectContext(hc1)) {
            is WitResult.Ok -> c.value
            is WitResult.Err -> throw Fail(Step.PREFIX, FailureReason.BAD_PREFIX, "no HC[1-9A-Z]: context identifier")
        }
        if (version != "1") throw Fail(Step.PREFIX, FailureReason.UNSUPPORTED_CONTEXT_VERSION, "context identifier HC$version")
        val deflated = try {
            Base45.decode(rest)
        } catch (e: IllegalArgumentException) {
            throw Fail(Step.BASE45, FailureReason.BASE45_INVALID, e.message ?: "invalid Base45")
        }
        return try {
            Platform.inflate(deflated, limits.maxInflatedBytes)
        } catch (e: LimitException) {
            throw Fail(Step.INFLATE, FailureReason.LIMIT_EXCEEDED, e.message ?: "inflate limit")
        } catch (e: IllegalArgumentException) {
            throw Fail(Step.INFLATE, FailureReason.INFLATE_FAILED, e.message ?: "invalid ZLIB data")
        }
    }

    override fun verify(hc1: String, store: TrustStore, reg: Registry, now: NumericDate, options: VerifierOptions): VerificationResult = try {
        VerificationResult.Verified(run(front(hc1, options.limits), store, reg, now, options))
    } catch (f: Fail) {
        failed(f.step, f.reason, f.detail)
    }

    override fun readUnverified(hc1: String, reg: Registry, limits: who.smart.trust.types.DecodeLimits): WitResult<UnverifiedHcert, VerificationFailure> = try {
        WitResult.Ok(unverified(front(hc1, limits), reg, limits))
    } catch (f: Fail) {
        WitResult.Err(VerificationFailure(f.step, f.reason, f.detail, emptyList()))
    }

    /** [STT-VER-011] Everything here is unverified; the first diagnostic says so. */
    private fun unverified(cose: ByteArray, reg: Registry, limits: who.smart.trust.types.DecodeLimits): UnverifiedHcert {
        val diagnostics = mutableListOf(Diagnostic(Step.SIGNATURE, Severity.WARNING, "NOT VERIFIED: no trust lookup and no signature check were made"))
        val s = parse(cose, limits)
        val claims = decodeClaims(s.payload, limits)
        val iss = (claims[Claim.ISS] as? CborItem.Text)?.value
        val iat = (claims[Claim.IAT] as? CborItem.Int)?.value
        val exp = (claims[Claim.EXP] as? CborItem.Int)?.value
        val entries = (claims[Claim.HCERT] as? CborItem.Map)?.entries.orEmpty()
        if (entries.size > 1) diagnostics += Diagnostic(Step.HCERT, Severity.WARNING, "hcert has ${entries.size} entries; the first is shown")
        val first = entries.firstOrNull()
        val subclaim = (first?.first as? CborItem.Int)?.value?.toInt()
        val payload = if (first == null || subclaim == null) {
            diagnostics += Diagnostic(Step.HCERT, Severity.WARNING, "no hcert entry")
            null
        } else try {
            payloadOf(subclaim, first.second, reg)
        } catch (f: Fail) {
            diagnostics += Diagnostic(Step.PAYLOAD, Severity.WARNING, f.detail)
            null
        }
        return UnverifiedHcert(s.kid, iss, iat, exp, subclaim, payload, diagnostics)
    }

    override fun verifyCose(cose: ByteArray, store: TrustStore, reg: Registry, now: NumericDate, options: VerifierOptions): VerificationResult = try {
        VerificationResult.Verified(run(cose, store, reg, now, options))
    } catch (f: Fail) {
        failed(f.step, f.reason, f.detail)
    }

    private fun failed(step: Step, reason: FailureReason, detail: String) =
        VerificationResult.Failed(VerificationFailure(step, reason, detail, emptyList()))

    private fun parse(cose: ByteArray, limits: who.smart.trust.types.DecodeLimits): who.smart.trust.cose.Sign1 =
        when (val r = HcertCose.parseSign1(cose, limits)) {
            is WitResult.Ok -> r.value
            is WitResult.Err -> throw when (val e = r.error) {
                is who.smart.trust.cose.CoseError.HeaderInvalid -> Fail(Step.HEADER, FailureReason.HEADER_INVALID, e.value)
                is who.smart.trust.cose.CoseError.UnsupportedAlgorithm -> Fail(Step.HEADER, FailureReason.UNSUPPORTED_ALGORITHM, "alg ${e.value}")
                is who.smart.trust.cose.CoseError.Malformed ->
                    Fail(Step.COSE, if (e.value.contains("exceeds") || e.value.contains("more than") || e.value.contains("deeper")) FailureReason.LIMIT_EXCEEDED else FailureReason.COSE_MALFORMED, e.value)
                else -> Fail(Step.COSE, FailureReason.COSE_MALFORMED, e.toString())
            }
        }

    private fun decodeClaims(payload: ByteArray, limits: who.smart.trust.types.DecodeLimits): CborItem.Map = try {
        cborDecode(payload, limits) as? CborItem.Map
    } catch (e: CborException) {
        throw Fail(Step.CWT, if (e.limit) FailureReason.LIMIT_EXCEEDED else FailureReason.COSE_MALFORMED, e.message!!)
    } ?: throw Fail(Step.CWT, FailureReason.COSE_MALFORMED, "CWT claim set is not a map")

    private fun payloadOf(subclaim: Int, body: CborItem, reg: Registry): Payload =
        if (subclaim == reg.subclaimFor(PayloadKind.DVC_MIN)) {
            val dvc = when (val r = HcertDvc.fromItem(body)) {
                is WitResult.Ok -> r.value
                is WitResult.Err -> throw Fail(Step.PAYLOAD, FailureReason.PAYLOAD_INVALID, r.error.joinToString("; ") { "${it.path}: ${it.message}" })
            }
            HcertDvc.validate(dvc).firstOrNull { it.severity == Severity.BLOCKING }
                ?.let { throw Fail(Step.PAYLOAD, FailureReason.PAYLOAD_INVALID, "${it.path}: ${it.message}") }
            Payload.DvcMin(dvc)
        } else {
            Payload.Raw(RawPayload(subclaim, CborEncoder.encode(body)))
        }

    private fun run(cose: ByteArray, store: TrustStore, reg: Registry, now: NumericDate, options: VerifierOptions): VerifiedHcert {
        val limits = options.limits
        val diagnostics = mutableListOf<Diagnostic>()

        // COSE parse and header validation [STT-VER-003]
        val s = parse(cose, limits)
        if (!s.tagged) diagnostics += Diagnostic(Step.COSE, Severity.WARNING, "COSE_Sign1 is not tagged 18")
        val kid = s.kid ?: throw Fail(Step.HEADER, FailureReason.HEADER_INVALID, "no kid")
        if (!s.kidInProtected) diagnostics += Diagnostic(Step.HEADER, Severity.WARNING, "kid is in the unprotected header")

        // Trust lookup, then every candidate until one verifies [STT-KID-002]
        val candidates = store.candidates(kid)
        if (candidates.isEmpty()) throw Fail(Step.TRUST_LOOKUP, FailureReason.KID_NOT_FOUND, "no trusted DSC has kid ${kid.toHex()}")
        val dsc: TrustedDsc = candidates.firstOrNull { HcertCose.verifySign1(s, it.der) is WitResult.Ok }
            ?: throw Fail(Step.SIGNATURE, FailureReason.SIGNATURE_INVALID, "no DSC with kid ${kid.toHex()} verifies the signature")
        // [STT-VER-006] presence on the trust list is the validity check; the X.509 period is reported.
        if (now < dsc.notBefore || now > dsc.notAfter) diagnostics += Diagnostic(Step.TRUST_LOOKUP, Severity.WARNING, "the DSC is outside its X.509 validity period")

        // CWT, decoded only now that the signature has verified.
        val claims = decodeClaims(s.payload, limits)
        val hcert = claims[Claim.HCERT] as? CborItem.Map
        val entries = hcert?.entries.orEmpty()

        // EKU against the certificate type [STT-VER-004]
        val dscHcertEkus = dsc.extendedKeyUsages.filter { it in hcertEkus }
        val subclaimForEku = (entries.singleOrNull()?.first as? CborItem.Int)?.value?.toInt()
        if (dscHcertEkus.isNotEmpty() && subclaimForEku != null) {
            val needed = ekuFor(subclaimForEku, entries.single().second)
            if (needed == null) diagnostics += Diagnostic(Step.KEY_USAGE, Severity.WARNING, "no certificate type is known for subclaim $subclaimForEku")
            else if (needed !in dscHcertEkus) throw Fail(Step.KEY_USAGE, FailureReason.KEY_USAGE_MISMATCH, "the DSC's extended key usage does not allow this certificate type")
        }

        // CWT claims and time [STT-CWT-002, STT-CWT-003, STT-VER-005]
        fun int(key: Long, name: String): Long? = when (val v = claims[key]) {
            null -> null
            is CborItem.Int -> v.value
            else -> throw Fail(Step.CWT, FailureReason.COSE_MALFORMED, "$name is not an integer NumericDate")
        }
        val exp = int(Claim.EXP, "exp") ?: throw Fail(Step.CWT, FailureReason.COSE_MALFORMED, "exp is missing")
        val iat = int(Claim.IAT, "iat") ?: throw Fail(Step.CWT, FailureReason.COSE_MALFORMED, "iat is missing")
        val iss = when (val v = claims[Claim.ISS]) {
            null -> null
            is CborItem.Text -> v.value
            else -> throw Fail(Step.CWT, FailureReason.COSE_MALFORMED, "iss is not a text string")
        }
        val skew = options.clockSkewSeconds.toLong()
        if (now - skew > exp) throw Fail(Step.TIME, FailureReason.EXPIRED, "exp has passed")
        if (iat > now + skew) throw Fail(Step.TIME, FailureReason.NOT_YET_VALID, "iat is in the future")

        // Subclaim dispatch [STT-SEL-004, STT-VER-007]
        if (entries.isEmpty()) throw Fail(Step.HCERT, FailureReason.HCERT_MISSING, "no hcert (-260) entry")
        if (entries.size > 1) throw Fail(Step.HCERT, FailureReason.MULTIPLE_SUBCLAIMS, "hcert has ${entries.size} entries")
        val (keyItem, body) = entries.single()
        val subclaim = (keyItem as? CborItem.Int)?.value?.toInt() ?: throw Fail(Step.HCERT, FailureReason.HCERT_MISSING, "hcert key is not an integer")
        if (reg.lookup(subclaim) == null) diagnostics += Diagnostic(Step.HCERT, Severity.WARNING, "subclaim $subclaim is not in the registry")
        val payload = payloadOf(subclaim, body, reg)

        // Claims this version does not know are kept. [STT-CWT-006]
        val known = setOf(Claim.ISS, Claim.EXP, Claim.IAT, Claim.HCERT)
        val other = claims.entries.filter { (k, _) -> (k as? CborItem.Int)?.value !in known }
            .map { (k, v) -> CborEncoder.encode(k) to CborEncoder.encode(v) }

        return VerifiedHcert(iss, iat, exp, subclaim, payload, dsc, other, diagnostics)
    }
}

/** The whole contract, implemented. */
object Hcert : HcertWorld {
    override val codec = HcertCodec
    override val cose = HcertCose
    override val subclaims = HcertSubclaims
    override val dvc = HcertDvc
    override val trust = HcertTrust
    override val issuer = HcertIssuer
    override val verifier = HcertVerifier
}
