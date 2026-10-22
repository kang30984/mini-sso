package io.github.kang30984.minisso.adapter.outbound.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import io.github.kang30984.minisso.application.signingkey.port.outbound.GeneratedSigningKey
import io.github.kang30984.minisso.application.signingkey.port.outbound.SigningKeyCryptoPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * RSA 2048 서명 키를 생성하고, 개인키 JWK를 AES-256-GCM으로 암호화한다.
 *
 * - 마스터 키는 환경 변수(MINISSO_KEY_ENCRYPTION_KEY)로만 주입한다. DB가 유출되어도 개인키는 복호화할 수 없다.
 * - kid를 AAD(추가 인증 데이터)로 묶어, 다른 키의 암호문을 바꿔치기하면 복호화가 실패한다.
 * - 암호문 형식: Base64( IV(12바이트) || 암호문+인증태그 )
 */
@Component
class RsaSigningKeyCrypto(
    @Value("\${minisso.signing-key.encryption-key:}") encodedEncryptionKey: String,
) : SigningKeyCryptoPort {

    private val encryptionKey = SecretKeySpec(decodeKey(encodedEncryptionKey), "AES")
    private val random = SecureRandom()

    override fun generate(): GeneratedSigningKey {
        val rsaKey = RSAKeyGenerator(RSA_KEY_SIZE)
            .keyID(UUID.randomUUID().toString())
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .generate()
        return GeneratedSigningKey(
            kid = rsaKey.keyID,
            publicJwk = rsaKey.toPublicJWK().toJSONString(),
            encryptedPrivateJwk = encrypt(rsaKey.keyID, rsaKey.toJSONString()),
        )
    }

    override fun decryptPrivateJwk(kid: String, encryptedPrivateJwk: String): String {
        val bytes = Base64.getDecoder().decode(encryptedPrivateJwk)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
        cipher.updateAAD(kid.toByteArray())
        return String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
    }

    private fun encrypt(kid: String, plaintext: String): String {
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(kid.toByteArray())
        return Base64.getEncoder().encodeToString(iv + cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8)))
    }

    companion object {
        private const val RSA_KEY_SIZE = 2048
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private const val AES_256_KEY_BYTES = 32

        private fun decodeKey(encoded: String): ByteArray {
            val key = runCatching { Base64.getDecoder().decode(encoded.trim()) }.getOrNull()
            check(key != null && key.size == AES_256_KEY_BYTES) {
                "minisso.signing-key.encryption-key(MINISSO_KEY_ENCRYPTION_KEY)에 Base64로 인코딩한 32바이트 키를 지정해야 합니다."
            }
            return key
        }
    }
}
