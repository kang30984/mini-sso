package io.github.kang30984.minisso.domain.signingkey

import java.time.Duration
import java.time.Instant

@JvmInline
value class SigningKeyId(val value: Long)

enum class SigningKeyStatus {
    /** 새 토큰 서명에 쓰는 키. 항상 하나만 쓴다 (여러 개면 가장 최근에 활성화된 키). */
    ACTIVE,

    /** 더 이상 서명하지 않지만, 이미 발급된 토큰을 검증할 수 있도록 일정 기간 JWKS에 게시하는 키 */
    RETIRING,
}

/**
 * 토큰 서명 키 (ADR-008). 개인키는 암호화된 상태로만 보관하고, 도메인은 그 내용을 해석하지 않는다.
 */
class SigningKey private constructor(
    val id: SigningKeyId?,
    val kid: String,
    status: SigningKeyStatus,
    val publicJwk: String,
    val encryptedPrivateJwk: String,
    val activatedAt: Instant,
    rotatedAt: Instant?,
) {
    var status: SigningKeyStatus = status
        private set
    var rotatedAt: Instant? = rotatedAt
        private set

    val isActive: Boolean get() = status == SigningKeyStatus.ACTIVE

    /** 교체 시 호출. 이후로는 검증용으로만 게시된다. */
    fun retire(now: Instant) {
        check(isActive) { "활성 키만 교체할 수 있습니다: $kid" }
        status = SigningKeyStatus.RETIRING
        rotatedAt = now
    }

    /** JWKS에 게시할지 여부. 교체된 키는 보존 기간이 지나면 게시하지 않는다. */
    fun isPublished(now: Instant, policy: SigningKeyPolicy): Boolean {
        if (isActive) return true
        val rotated = rotatedAt ?: return false
        return rotated.plus(policy.retention).isAfter(now)
    }

    companion object {
        fun activate(kid: String, publicJwk: String, encryptedPrivateJwk: String, now: Instant): SigningKey {
            require(kid.isNotBlank()) { "kid는 비어 있을 수 없습니다." }
            return SigningKey(null, kid, SigningKeyStatus.ACTIVE, publicJwk, encryptedPrivateJwk, now, null)
        }

        fun reconstitute(
            id: SigningKeyId,
            kid: String,
            status: SigningKeyStatus,
            publicJwk: String,
            encryptedPrivateJwk: String,
            activatedAt: Instant,
            rotatedAt: Instant?,
        ): SigningKey = SigningKey(id, kid, status, publicJwk, encryptedPrivateJwk, activatedAt, rotatedAt)
    }
}

/**
 * @property retention 교체된 키를 JWKS에 계속 게시하는 기간.
 * 이 키로 서명된 토큰 중 가장 오래 사는 것(ID 토큰 30분)보다 길어야 교체 직후에도 기존 토큰이 검증된다.
 */
data class SigningKeyPolicy(
    val retention: Duration = Duration.ofHours(1),
) {
    init {
        require(retention > Duration.ZERO) { "retention은 0보다 커야 합니다." }
    }
}
