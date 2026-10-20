package who.smart.trust.core

import who.smart.trust.types.CoseAlgorithm

/** What a DSC certificate says, read by the platform's X.509 parser. */
class CertificateInfo(
    val notBefore: Long,
    val notAfter: Long,
    /** Subject C attribute. */
    val country: String?,
    val extendedKeyUsages: List<String>,
)

/**
 * The few things taken from the platform rather than written here: hashing,
 * DEFLATE, X.509 parsing and signature verification. JVM and Android use the
 * JDK (java.security, java.util.zip); Node uses node:crypto and node:zlib.
 * No third-party code. [STT-SEC-002]
 */
internal expect object Platform {
    fun sha256(data: ByteArray): ByteArray
    fun deflate(data: ByteArray, level: Int): ByteArray
    /** Inflates a ZLIB stream; throws when it is invalid or would exceed [maxOut] bytes. */
    fun inflate(data: ByteArray, maxOut: Int): ByteArray
    fun certificate(der: ByteArray): CertificateInfo
    /**
     * Verifies [signature] over [data] with the DSC's public key. For ES256 the
     * signature is the 64-byte r||s; for PS256 it is RSASSA-PSS with SHA-256,
     * MGF1-SHA-256 and a 32-byte salt.
     */
    fun verify(alg: CoseAlgorithm, dscDer: ByteArray, data: ByteArray, signature: ByteArray): Boolean
    /** Bytes from the platform's cryptographically secure random generator. */
    fun randomBytes(n: Int): ByteArray
    /** AES-256-GCM with a 128-bit tag; returns ciphertext followed by the tag. */
    fun aesGcmEncrypt(key: ByteArray, iv: ByteArray, aad: ByteArray, plaintext: ByteArray): ByteArray
    /** Inverse of [aesGcmEncrypt]; throws IllegalArgumentException when the tag does not verify. */
    fun aesGcmDecrypt(key: ByteArray, iv: ByteArray, aad: ByteArray, ciphertextAndTag: ByteArray): ByteArray
    /** Inflates raw DEFLATE (RFC 1951, no ZLIB header), as JWE "zip": "DEF" uses. */
    fun inflateRaw(data: ByteArray, maxOut: Int): ByteArray
}
