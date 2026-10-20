package who.smart.trust.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PSSParameterSpec
import java.util.zip.Deflater
import java.util.zip.Inflater
import who.smart.trust.types.CoseAlgorithm

internal actual object Platform {
    actual fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

    actual fun deflate(data: ByteArray, level: Int): ByteArray {
        val d = Deflater(level)
        try {
            d.setInput(data)
            d.finish()
            val out = ByteArrayOutputStream(data.size / 2 + 64)
            val buf = ByteArray(4096)
            while (!d.finished()) out.write(buf, 0, d.deflate(buf))
            return out.toByteArray()
        } finally {
            d.end()
        }
    }

    actual fun inflate(data: ByteArray, maxOut: Int): ByteArray {
        val inf = Inflater()
        try {
            inf.setInput(data)
            val out = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            while (!inf.finished()) {
                val n = inf.inflate(buf)
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) throw IllegalArgumentException("truncated ZLIB stream")
                if (out.size() + n > maxOut) throw LimitException("inflated data exceeds $maxOut bytes")
                out.write(buf, 0, n)
            }
            if (inf.remaining != 0) throw IllegalArgumentException("${inf.remaining} byte(s) after the ZLIB stream")
            return out.toByteArray()
        } catch (e: java.util.zip.DataFormatException) {
            throw IllegalArgumentException("invalid ZLIB data: ${e.message}")
        } finally {
            inf.end()
        }
    }

    private fun x509(der: ByteArray): X509Certificate =
        CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(der)) as X509Certificate

    actual fun certificate(der: ByteArray): CertificateInfo {
        val c = x509(der)
        val country = Regex("(?:^|,)C=([^,]+)").find(c.subjectX500Principal.getName("RFC2253"))?.groupValues?.get(1)
        return CertificateInfo(c.notBefore.time / 1000, c.notAfter.time / 1000, country, c.extendedKeyUsage ?: emptyList())
    }

    actual fun verify(alg: CoseAlgorithm, dscDer: ByteArray, data: ByteArray, signature: ByteArray): Boolean {
        val key = x509(dscDer).publicKey
        return try {
            when (alg) {
                CoseAlgorithm.ES256 -> {
                    if (key !is ECPublicKey || signature.size != 64) return false
                    Signature.getInstance("SHA256withECDSA").run {
                        initVerify(key); update(data); verify(EcdsaSignature.rawToDer(signature))
                    }
                }
                CoseAlgorithm.PS256 -> {
                    if (key !is RSAPublicKey) return false
                    pss().run { initVerify(key); update(data); verify(signature) }
                }
            }
        } catch (e: java.security.GeneralSecurityException) {
            false
        }
    }

    /** RSASSA-PSS, SHA-256, MGF1-SHA-256, salt 32 (RFC 8230). Android names it SHA256withRSA/PSS. */
    private fun pss(): Signature = try {
        Signature.getInstance("RSASSA-PSS").apply {
            setParameter(PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1))
        }
    } catch (e: java.security.NoSuchAlgorithmException) {
        Signature.getInstance("SHA256withRSA/PSS")
    }
}
