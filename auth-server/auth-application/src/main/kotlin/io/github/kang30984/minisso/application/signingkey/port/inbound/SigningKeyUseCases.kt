package io.github.kang30984.minisso.application.signingkey.port.inbound

import java.time.Instant

/** 인가 서버가 토큰 서명·검증에 쓸 키를 조회한다 */
interface GetSigningKeysQuery {
    /** 현재 서명 키 (개인키 포함 JWK JSON). 활성 키가 없으면 새로 만든다. */
    fun signingJwk(): String

    /** JWKS에 게시할 공개키 목록 (활성 키 + 보존 기간 내의 교체된 키) */
    fun publishedPublicJwks(): List<String>
}

interface EnsureSigningKeyUseCase {
    /** @return 새로 만들었으면 true */
    fun ensureActiveKey(): Boolean
}

interface RotateSigningKeyUseCase {
    /** @return 새로 활성화된 키의 kid */
    fun rotate(): String
}

interface ListSigningKeysQuery {
    fun list(): List<SigningKeyView>
}

/** 관리 화면용. 키 내용은 포함하지 않는다. */
data class SigningKeyView(
    val kid: String,
    val status: String,
    val published: Boolean,
    val activatedAt: Instant,
    val rotatedAt: Instant?,
)
