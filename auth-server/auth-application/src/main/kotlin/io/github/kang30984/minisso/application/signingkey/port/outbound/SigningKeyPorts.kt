package io.github.kang30984.minisso.application.signingkey.port.outbound

import io.github.kang30984.minisso.domain.signingkey.SigningKey

interface SigningKeyPort {
    fun findAll(): List<SigningKey>

    fun save(key: SigningKey): SigningKey
}

/** 키 생성과 개인키 암호화는 기술 선택이므로 어댑터가 담당한다 */
interface SigningKeyCryptoPort {
    fun generate(): GeneratedSigningKey

    /** @return 개인키를 포함한 JWK JSON */
    fun decryptPrivateJwk(kid: String, encryptedPrivateJwk: String): String
}

data class GeneratedSigningKey(
    val kid: String,
    val publicJwk: String,
    val encryptedPrivateJwk: String,
) {
    override fun toString(): String = "GeneratedSigningKey(kid=$kid, publicJwk=$publicJwk, encryptedPrivateJwk=****)"
}
