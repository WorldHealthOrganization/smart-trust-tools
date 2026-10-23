package who.smart.trust.core

import who.smart.trust.HcertResult
import who.smart.trust.trust.TrustError

/**
 * The JsonWebSignature2020 proof of a GDHCN trust list DID document, as
 * WorldHealthOrganization/tng-key-distribution makes it (ld-signatures-java
 * 1.11.0, URDNA2015 by titanium). [STT-TRU-006]
 *
 * Not a JSON-LD processor: the document shapes that service emits are mapped
 * to RDF here directly, with the IRIs of the pinned contexts it signs with
 * (did/v1, jws-2020/v1, the tng-context for production, UAT or DEV, and
 * security v3 for the proof options). Neither graph has more than one blank
 * node, so URDNA2015 reduces to serialising, de-duplicating and sorting the
 * quads. Anything outside those shapes (another key, another type, another
 * context, a number in a JWK, a relative IRI) is refused, never dropped: a
 * term that a processor would map and this code would not must not be able
 * to change the signed bytes unnoticed.
 *
 * The signature is over ASCII(header) || "." || SHA-256(options N-Quads) ||
 * SHA-256(document N-Quads), a detached b64:false ES256 JWS; the service
 * writes it DER-encoded rather than as RFC 7518's r||s, so both are taken.
 */
internal object DidProof {
    private class Invalid(message: String) : Exception(message)

    private fun fail(message: String): Nothing = throw Invalid(message)

    private const val SEC = "https://w3id.org/security#"
    private const val RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
    private const val RDF_JSON = "http://www.w3.org/1999/02/22-rdf-syntax-ns#JSON"
    private const val XSD_DATETIME = "http://www.w3.org/2001/XMLSchema#dateTime"
    private const val DC_CREATED = "http://purl.org/dc/terms/created"

    private const val DID_V1 = "https://www.w3.org/ns/did/v1"
    private const val JWS_2020_V1 = "https://w3id.org/security/suites/jws-2020/v1"
    private const val TNG_CONTEXT = "https://smart.who.int/trust/tng-context/"

    /** The tng-context per environment: its URL's file name and the suffix of its value set IRIs. */
    private val TNG_ENVIRONMENTS = mapOf("v1.jsonld" to "", "v1-UAT.jsonld" to "-UAT", "v1-DEV.jsonld" to "-DEV")

    /** tng-context term to value set name; the IRI is https://smart.who.int/trust/ValueSet-<name><env>.jsonld. */
    private val VALUE_SETS = linkedMapOf("domain" to "Domains", "participant" to "Participants", "keyusage" to "KeyUsage")

    private val DOC_KEYS = setOf("@context", "id", "controller", "verificationMethod")
    private val VM_KEYS = setOf("id", "type", "controller", "publicKeyJwk") + VALUE_SETS.keys
    private val PROOF_KEYS = setOf("type", "created", "nonce", "proofPurpose", "verificationMethod", "domain", "challenge", "jws")

    /** Removed from the proof before its options are canonicalised, as ld-signatures does. */
    private val PROOF_VALUES = setOf("jws", "proofValue", "signatureValue")

    /** The only protected header the profile uses. */
    private val JWS_HEADER = mapOf("b64" to false, "crit" to listOf("b64"), "alg" to "ES256")

    fun verify(document: String, signer: String): HcertResult<Map<*, *>, TrustError> = try {
        val doc = parseObject(document, "DID document")
        verifyParsed(doc, parseObject(signer, "signer DID document"))
        HcertResult.Ok(doc)
    } catch (e: Invalid) {
        HcertResult.Err(TrustError.ProofInvalid(e.message ?: "proof invalid"))
    }

    private fun parseObject(json: String, what: String): Map<*, *> = try {
        parseJson(json, rejectDuplicates = true) as? Map<*, *>
    } catch (e: IllegalArgumentException) {
        fail("$what: ${e.message}")
    } ?: fail("$what is not a JSON object")

    private fun verifyParsed(doc: Map<*, *>, signer: Map<*, *>) {
        val proof = doc["proof"] as? Map<*, *> ?: fail("no proof object")
        val jws = proof["jws"] as? String ?: fail("proof has no jws string")
        val parts = jws.split('.')
        if (parts.size != 3 || parts[1].isNotEmpty()) fail("jws is not a detached JWS (header..signature)")
        val header = parts[0]
        val headerJson = try {
            parseJson(b64url(header, "JWS header").decodeToString(throwOnInvalidSequence = true), rejectDuplicates = true)
        } catch (e: IllegalArgumentException) {
            fail("JWS header: ${e.message}")
        } catch (e: CharacterCodingException) {
            fail("JWS header is not UTF-8")
        }
        if (headerJson != JWS_HEADER) fail("JWS header is not {\"b64\":false,\"crit\":[\"b64\"],\"alg\":\"ES256\"}")

        val docHash = Platform.sha256(documentNQuads(doc.filterKeys { it != "proof" }).encodeToByteArray())
        val optsHash = Platform.sha256(proofOptionsNQuads(proof.filterKeys { it !in PROOF_VALUES }).encodeToByteArray())
        val signingInput = header.encodeToByteArray() + '.'.code.toByte() + optsHash + docHash

        val (x, y) = signerKey(signer, proof["verificationMethod"] as String)
        val sig = b64url(parts[2], "JWS signature")
        val ok = when {
            sig.size == 64 -> Platform.verifyEs256Point(x, y, signingInput, sig) ||
                (sig[0] == 0x30.toByte() && derVerifies(x, y, signingInput, sig))
            else -> derVerifies(x, y, signingInput, sig)
        }
        if (!ok) fail("the signature does not verify with the signer's key")
    }

    private fun derVerifies(x: ByteArray, y: ByteArray, data: ByteArray, der: ByteArray): Boolean {
        val raw = try {
            EcdsaSignature.derToRaw(der)
        } catch (e: IllegalArgumentException) {
            if (der.size == 64) return false // was taken as r||s already
            fail("signature is neither DER nor 64-byte r||s: ${e.message}")
        }
        return Platform.verifyEs256Point(x, y, data, raw)
    }

    /** The P-256 point of the signer's verification method whose id is exactly [vmId]. */
    private fun signerKey(signer: Map<*, *>, vmId: String): Pair<ByteArray, ByteArray> {
        if (signer["id"] != vmId.substringBefore('#')) fail("the signer document is not $vmId's DID")
        val methods = (signer["verificationMethod"] as? List<*> ?: fail("signer has no verificationMethod array"))
            .filter { (it as? Map<*, *>)?.get("id") == vmId }
        val vm = methods.singleOrNull() as? Map<*, *> ?: fail("signer has ${methods.size} verification methods with id $vmId")
        if (vm["type"] != "JsonWebKey2020") fail("signer key is not a JsonWebKey2020")
        val jwk = vm["publicKeyJwk"] as? Map<*, *> ?: fail("signer key has no publicKeyJwk")
        if (jwk["kty"] != "EC" || jwk["crv"] != "P-256") fail("signer key is not an EC P-256 JWK")
        if ("alg" in jwk && jwk["alg"] != "ES256") fail("signer key is not for ES256")
        fun coordinate(name: String): ByteArray {
            val b = b64url(jwk[name] as? String ?: fail("signer JWK has no $name"), "signer JWK $name")
            return when {
                b.size == 32 -> b
                b.size == 33 && b[0] == 0.toByte() -> b.copyOfRange(1, 33)
                else -> fail("signer JWK $name is ${b.size} bytes, not 32")
            }
        }
        return coordinate("x") to coordinate("y")
    }

    private fun b64url(s: String, what: String): ByteArray {
        if (s.isEmpty() || !s.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '-' || it == '_' } || s.length % 4 == 1)
            fail("$what is not unpadded base64url")
        return try {
            Base64Std.decode(s)
        } catch (e: IllegalArgumentException) {
            fail("$what is not canonical base64url: ${e.message}") // e.g. non-zero pad bits
        }
    }

    // ---- canonical N-Quads --------------------------------------------------

    /** URDNA2015 N-Quads of a trust list DID document without its proof. */
    fun documentNQuads(doc: Map<*, *>): String {
        exactKeys(doc, DOC_KEYS, setOf("@context", "id"), "DID document")
        val ctx = doc["@context"] as? List<*> ?: fail("@context is not an array")
        val tng = (ctx.getOrNull(2) as? String)?.takeIf { it.startsWith(TNG_CONTEXT) }?.removePrefix(TNG_CONTEXT)
        val env = TNG_ENVIRONMENTS[tng]
        if (ctx.size != 3 || ctx[0] != DID_V1 || ctx[1] != JWS_2020_V1 || env == null)
            fail("@context is not [did/v1, jws-2020/v1, tng-context v1, v1-UAT or v1-DEV]")
        val q = mutableListOf<String>()
        val id = did(doc["id"], "id")
        when (val c = doc["controller"]) {
            null -> {}
            is List<*> -> c.forEach { q += quad(id, SEC + "controller", did(it, "controller")) }
            else -> q += quad(id, SEC + "controller", did(c, "controller"))
        }
        if ("verificationMethod" in doc) {
            val methods = doc["verificationMethod"] as? List<*> ?: fail("verificationMethod is not an array")
            for (m in methods) {
                if (m is String) {
                    q += quad(id, SEC + "verificationMethod", did(m, "verificationMethod"))
                    continue
                }
                val vm = m as? Map<*, *> ?: fail("verificationMethod entry is neither a DID URL nor an object")
                exactKeys(vm, VM_KEYS, setOf("id", "type"), "verificationMethod")
                if (vm["type"] != "JsonWebKey2020") fail("verificationMethod type is not JsonWebKey2020")
                val v = did(vm["id"], "verificationMethod id")
                q += quad(id, SEC + "verificationMethod", v)
                q += quad(v, RDF_TYPE, iri(SEC + "JsonWebKey2020"))
                if ("controller" in vm) q += quad(v, SEC + "controller", did(vm["controller"], "verificationMethod controller"))
                if ("publicKeyJwk" in vm) {
                    val jwk = vm["publicKeyJwk"] as? Map<*, *> ?: fail("publicKeyJwk is not an object")
                    q += quad(v, SEC + "publicKeyJwk", literal(jcs(jwk)) + "^^" + iri(RDF_JSON))
                }
                for ((term, name) in VALUE_SETS) {
                    if (term !in vm) continue
                    val set = vm[term] as? Map<*, *> ?: fail("$term is not an object")
                    exactKeys(set, setOf("code"), setOf("code"), term)
                    val code = set["code"] as? String ?: fail("$term code is not a string")
                    // "#CODE" resolved against the scoped @base ".../ValueSet-<name><env>.jsonld#".
                    if (!code.startsWith("#") || code.length < 2 || code.indexOf('#', 1) >= 0) fail("$term code is not a #fragment")
                    val base = "https://smart.who.int/trust/ValueSet-$name$env.jsonld"
                    q += quad(v, base, iri(base + code))
                }
            }
        }
        return canonical(q)
    }

    /** URDNA2015 N-Quads of the proof options: the proof without jws, in the security v3 context. */
    fun proofOptionsNQuads(proof: Map<*, *>): String {
        exactKeys(proof, PROOF_KEYS - PROOF_VALUES, setOf("type", "created", "proofPurpose", "verificationMethod"), "proof")
        if (proof["type"] != "JsonWebSignature2020") fail("proof type is not JsonWebSignature2020")
        if (proof["proofPurpose"] != "assertionMethod") fail("proofPurpose is not assertionMethod")
        val b = "_:c14n0"
        val q = mutableListOf(
            "$b ${iri(RDF_TYPE)} ${iri(SEC + "JsonWebSignature2020")}",
            // The lexical form as given: a processor does not normalise a typed literal.
            "$b ${iri(DC_CREATED)} ${literal(string(proof["created"], "created"))}^^${iri(XSD_DATETIME)}",
            "$b ${iri(SEC + "proofPurpose")} ${iri(SEC + "assertionMethod")}",
            "$b ${iri(SEC + "verificationMethod")} ${did(proof["verificationMethod"], "proof verificationMethod")}",
        )
        for (term in listOf("nonce", "domain", "challenge")) {
            if (term in proof) q += "$b ${iri(SEC + term)} ${literal(string(proof[term], term))}"
        }
        return canonical(q)
    }

    private fun exactKeys(m: Map<*, *>, allowed: Set<String>, required: Set<String>, what: String) {
        val extra = m.keys.filter { it !in allowed }
        if (extra.isNotEmpty()) fail("$what has a member the profile does not model: ${extra.joinToString()}")
        val missing = required.filter { it !in m }
        if (missing.isNotEmpty()) fail("$what lacks ${missing.joinToString()}")
    }

    private fun string(v: Any?, what: String): String = v as? String ?: fail("$what is not a string")

    private fun quad(s: String, p: String, o: String) = "$s ${iri(p)} $o"

    /**
     * Sorted, de-duplicated, each line ending " .\n". URDNA2015 sorts by code
     * point; Java and JavaScript processors sort by UTF-16 code unit. The two
     * orders differ only where a supplementary character meets one at or above
     * U+E000; that case is refused rather than guessed.
     */
    private fun canonical(quads: List<String>): String {
        val lines = quads.map { "$it .\n" }.distinct()
        val byCodeUnit = lines.sorted()
        if (byCodeUnit != lines.sortedWith(::compareCodePoints)) fail("N-Quads order depends on the code point convention")
        return byCodeUnit.joinToString("")
    }

    private fun compareCodePoints(a: String, b: String): Int {
        val n = minOf(a.length, b.length)
        for (i in 0 until n) {
            if (a[i] != b[i]) {
                fun rank(c: Char) = if (c.isSurrogate()) c.code + 0x10000 else c.code
                return rank(a[i]) - rank(b[i])
            }
        }
        return a.length - b.length
    }

    /** An absolute DID or DID URL as an N-Quads IRI; anything else is refused, never resolved. */
    private fun did(v: Any?, what: String): String {
        val s = v as? String ?: fail("$what is not a string")
        if (!s.startsWith("did:")) fail("$what is not a DID")
        return iri(s)
    }

    private fun iri(s: String): String {
        if (s.any { it <= ' ' || it in "<>\"{}|^`\\" || it == '\u007f' } || hasLoneSurrogate(s)) fail("not a plain IRI: $s")
        return "<$s>"
    }

    /** An N-Quads string literal; control characters are refused, as processors escape them differently. */
    private fun literal(s: String): String {
        if (s.any { it < ' ' || it == '\u007f' } || hasLoneSurrogate(s)) fail("literal with a control character or lone surrogate")
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }

    private fun hasLoneSurrogate(s: String): Boolean {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c.isHighSurrogate() && i + 1 < s.length && s[i + 1].isLowSurrogate()) i += 2
            else if (c.isSurrogate()) return true
            else i++
        }
        return false
    }

    // ---- RFC 8785 ------------------------------------------------------------

    /**
     * JSON Canonicalization Scheme for strings, booleans, null, arrays and
     * objects; numbers are refused (no JWK member is one). Members sort by
     * UTF-16 code unit, which is Kotlin's String order.
     */
    fun jcs(v: Any?): String = when (v) {
        null -> "null"
        is Boolean -> v.toString()
        is String -> jcsString(v)
        is List<*> -> v.joinToString(",", "[", "]") { jcs(it) }
        is Map<*, *> -> v.keys.map { it as String }.sorted().joinToString(",", "{", "}") { jcsString(it) + ":" + jcs(v[it]) }
        else -> fail("a JSON number in publicKeyJwk, which the profile does not canonicalise")
    }

    private fun jcsString(s: String): String {
        if (hasLoneSurrogate(s)) fail("lone surrogate in publicKeyJwk")
        val sb = StringBuilder("\"")
        for (c in s) when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\b' -> sb.append("\\b")
            '\u000c' -> sb.append("\\f")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (c < ' ') sb.append("\\u").append(c.code.toString(16).padStart(4, '0')) else sb.append(c)
        }
        return sb.append('"').toString()
    }
}
