package io.github.kang30984.minisso.adapter.outbound.security

import org.springframework.security.crypto.password.PasswordEncoder
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 검증에 성공한 (원문, 저장된 해시) 조합을 잠시 기억해, 같은 클라이언트 시크릿의 반복 검증에서 Argon2를 생략한다 (ADR-012).
 *
 * - 저장소의 해시는 그대로 Argon2다. DB가 유출되었을 때의 보호 수준은 변하지 않는다.
 * - 틀린 시크릿은 절대 캐시하지 않는다. 무차별 대입은 매번 Argon2 비용을 그대로 치른다.
 * - 캐시 키는 원문이 아니라 SHA-256(원문 + 저장된 해시)다. 메모리에 시크릿 원문이 남지 않는다.
 * - 시크릿을 바꾸면 저장된 해시가 바뀌어 키가 달라지므로 이전 시크릿은 자동으로 무효가 된다.
 *
 * 클라이언트 인증 전용이다. 계정 비밀번호는 이 캐시를 거치지 않는다.
 */
class VerifiedSecretCachingPasswordEncoder(
    private val delegate: PasswordEncoder,
    private val ttl: Duration,
    private val maxEntries: Int,
    private val clock: Clock = Clock.systemUTC(),
) : PasswordEncoder {

    private val verified = ConcurrentHashMap<String, Instant>()

    override fun encode(rawPassword: CharSequence?): String? = delegate.encode(rawPassword)

    override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean {
        if (rawPassword == null || encodedPassword == null) return delegate.matches(rawPassword, encodedPassword)

        val key = cacheKey(rawPassword, encodedPassword)
        val now = clock.instant()
        verified[key]?.let { expiresAt ->
            if (expiresAt.isAfter(now)) return true
            verified.remove(key)
        }

        val matches = delegate.matches(rawPassword, encodedPassword)
        if (matches) remember(key, now)
        return matches
    }

    override fun upgradeEncoding(encodedPassword: String?): Boolean = delegate.upgradeEncoding(encodedPassword)

    private fun remember(key: String, now: Instant) {
        if (verified.size >= maxEntries) {
            verified.entries.removeIf { !it.value.isAfter(now) }
            // 만료 항목을 지워도 가득 차 있으면 캐시하지 않는다 (클라이언트 수만큼만 쓰이므로 정상적으로는 일어나지 않음)
            if (verified.size >= maxEntries) return
        }
        verified[key] = now.plus(ttl)
    }

    private fun cacheKey(rawPassword: CharSequence, encodedPassword: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(rawPassword.toString().toByteArray(Charsets.UTF_8))
        digest.update(0)
        digest.update(encodedPassword.toByteArray(Charsets.UTF_8))
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
