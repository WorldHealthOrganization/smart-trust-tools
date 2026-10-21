package who.smart.trust.core

import who.smart.trust.HcertResult
import who.smart.trust.trust.Trust
import who.smart.trust.trust.TrustError
import who.smart.trust.trust.TrustStore
import who.smart.trust.trust.TrustedDsc
import who.smart.trust.types.Kid

/**
 * DSCs the host has loaded. No I/O. Lookup returns every DSC with the kid;
 * kids are computed from the DER, never taken from a list's labels.
 * [STT-TRU-001, STT-TRU-002, STT-TRU-005, STT-KID-002, STT-GEN-012]
 */
class HcertTrustStore : TrustStore {
    private val dscs = mutableListOf<TrustedDsc>()

    private fun add(der: ByteArray, domain: String? = null, participant: String? = null): HcertResult<Int, TrustError> {
        if (dscs.any { it.der.contentEquals(der) }) return HcertResult.Ok(0)
        val info = try {
            Platform.certificate(der)
        } catch (e: Exception) {
            return HcertResult.Err(TrustError.Parse("not an X.509 certificate: ${e.message}"))
        }
        dscs += TrustedDsc(der, HcertCose.kidFromDsc(der), info.country, info.notBefore, info.notAfter, info.extendedKeyUsages, domain, participant)
        return HcertResult.Ok(1)
    }

    override fun addDer(der: ByteArray): HcertResult<Int, TrustError> = add(der)

    override fun addPem(pem: String): HcertResult<Int, TrustError> {
        val blocks = try {
            Pem.blocks(pem)
        } catch (e: IllegalArgumentException) {
            return HcertResult.Err(TrustError.Parse("bad base64 in a PEM block"))
        }
        if (blocks.isEmpty()) return HcertResult.Err(TrustError.Parse("no CERTIFICATE block"))
        var n = 0
        for (b in blocks) when (val r = add(b)) {
            is HcertResult.Ok -> n += r.value
            is HcertResult.Err -> return r
        }
        return HcertResult.Ok(n)
    }

    /**
     * GDHCN DID trustlist v2 (embedded or reference variant): each
     * verificationMethod object with publicKeyJwk.x5c contributes its first
     * certificate. DID URL references, as the reference variant lists, are
     * the host's to fetch and load. Trust domain and participant come from
     * the `trustlist:<domain>:<participant>:<usage>` DID path; "-" is the
     * wildcard.
     */
    override fun addDidDocument(json: String): HcertResult<Int, TrustError> {
        val doc = try {
            parseJson(json) as? Map<*, *>
        } catch (e: IllegalArgumentException) {
            return HcertResult.Err(TrustError.Parse(e.message ?: "invalid JSON"))
        } ?: return HcertResult.Err(TrustError.Parse("DID document is not a JSON object"))
        val methods = doc["verificationMethod"] as? List<*> ?: return HcertResult.Err(TrustError.Parse("no verificationMethod array"))
        var n = 0
        for (m in methods) {
            val vm = m as? Map<*, *> ?: continue
            val x5c = (vm["publicKeyJwk"] as? Map<*, *>)?.get("x5c") as? List<*> ?: continue
            val first = x5c.firstOrNull() as? String ?: return HcertResult.Err(TrustError.Parse("x5c entry is not a string"))
            val der = try {
                Base64Std.decode(first)
            } catch (e: IllegalArgumentException) {
                return HcertResult.Err(TrustError.Parse("x5c entry is not base64"))
            }
            val (domain, participant) = pathOf(vm["id"] as? String ?: doc["id"] as? String)
            when (val r = add(der, domain, participant)) {
                is HcertResult.Ok -> n += r.value
                is HcertResult.Err -> return r
            }
        }
        return HcertResult.Ok(n)
    }

    private fun pathOf(did: String?): Pair<String?, String?> {
        val parts = did?.substringBefore('#')?.split(':') ?: return null to null
        val at = parts.indexOf("trustlist")
        fun level(i: Int) = parts.getOrNull(at + i)?.takeIf { it != "-" && it.isNotEmpty() }
        return if (at < 0) null to null else level(1) to level(2)
    }

    override fun candidates(k: Kid): List<TrustedDsc> = dscs.filter { it.kid.contentEquals(k) }
    override fun size(): Int = dscs.size
    override fun clear() = dscs.clear()
    override fun close() {}
}

object HcertTrust : Trust {
    override fun newTrustStore(): TrustStore = HcertTrustStore()
}
