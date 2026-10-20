@file:OptIn(ExperimentalJsExport::class)

package who.smart.trust.core

import who.smart.trust.payloads.Payload
import who.smart.trust.verifier.VerificationResult
import who.smart.trust.verifier.VerifierOptions

/**
 * A minimal JavaScript entry point to the verifier, for Node. The full
 * JavaScript surface comes with the hcert-js facade. [STT-GEN-011]
 */
@JsExport
object HcertNode {
    /** Verifies [hc1] against the PEM DSCs in [trustPem] at [now] (Unix seconds); returns a JSON summary without payload content. */
    fun verifySummary(hc1: String, trustPem: String, now: Double): String {
        val store = HcertTrustStore()
        store.addPem(trustPem)
        val r = HcertVerifier.verify(hc1, store, SubclaimRegistry(), now.toLong(), VerifierOptions(DEFAULT_LIMITS, 300))
        return when (r) {
            is VerificationResult.Verified -> {
                val v = r.value
                val kind = if (v.payload is Payload.DvcMin) "dvc-min" else "raw"
                """{"status":"verified","subclaim":${v.subclaim},"iss":${v.iss?.let { "\"$it\"" } ?: "null"},"payload":"$kind","kid":"${v.dsc.kid.toHex()}"}"""
            }
            is VerificationResult.Failed -> """{"status":"failed","step":"${r.value.step}","reason":"${r.value.reason}"}"""
        }
    }
}
