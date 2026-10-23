package io.github.kang30984.minisso.adapter.outbound.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class VerifiedSecretCachingPasswordEncoderTest {

    private val delegate = CountingEncoder()
    private val clock = MutableClock(Instant.parse("2026-10-01T00:00:00Z"))
    private val encoder = VerifiedSecretCachingPasswordEncoder(delegate, Duration.ofMinutes(10), 100, clock)

    @Test
    fun `맞는 시크릿은 처음 한 번만 실제 해시 비교를 한다`() {
        repeat(5) { assertThat(encoder.matches("secret", "{hash}secret")).isTrue() }

        assertThat(delegate.matchCalls).isEqualTo(1)
    }

    @Test
    fun `틀린 시크릿은 캐시하지 않아 매번 실제 해시 비교를 한다`() {
        repeat(5) { assertThat(encoder.matches("wrong", "{hash}secret")).isFalse() }

        assertThat(delegate.matchCalls).isEqualTo(5)
    }

    @Test
    fun `저장된 해시가 바뀌면(시크릿 교체) 다시 비교한다`() {
        encoder.matches("secret", "{hash}secret")

        assertThat(encoder.matches("secret", "{hash}rotated")).isFalse()
        assertThat(delegate.matchCalls).isEqualTo(2)
    }

    @Test
    fun `TTL이 지나면 다시 비교한다`() {
        encoder.matches("secret", "{hash}secret")
        clock.advance(Duration.ofMinutes(10))

        encoder.matches("secret", "{hash}secret")

        assertThat(delegate.matchCalls).isEqualTo(2)
    }

    private class CountingEncoder : PasswordEncoder {
        var matchCalls = 0

        override fun encode(rawPassword: CharSequence?): String = "{hash}$rawPassword"

        override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean {
            matchCalls++
            return encodedPassword == "{hash}$rawPassword"
        }
    }

    private class MutableClock(private var now: Instant) : Clock() {
        fun advance(duration: Duration) {
            now = now.plus(duration)
        }

        override fun instant(): Instant = now

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?): Clock = this
    }
}
