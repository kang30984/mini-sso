package io.github.kang30984.minisso.adapter.outbound.redis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class RedisLoginRateLimiterIT {

    @Test
    fun `한도까지는 허용하고 넘으면 거부한다`() {
        val limiter = RedisLoginRateLimiter(connectionFactory, fixedClock("2026-10-01T00:00:10Z"))

        val results = (1..4).map { limiter.tryAcquire("ip:10.0.0.1", 3, Duration.ofMinutes(1)) }

        assertThat(results).containsExactly(true, true, true, false)
    }

    @Test
    fun `키가 다르면 따로 센다`() {
        val limiter = RedisLoginRateLimiter(connectionFactory, fixedClock("2026-10-01T00:00:10Z"))
        repeat(3) { limiter.tryAcquire("ip:10.0.0.2", 3, Duration.ofMinutes(1)) }

        assertThat(limiter.tryAcquire("ip:10.0.0.3", 3, Duration.ofMinutes(1))).isTrue()
    }

    @Test
    fun `다음 윈도가 되면 다시 허용한다`() {
        repeat(3) { RedisLoginRateLimiter(connectionFactory, fixedClock("2026-10-01T00:05:10Z")).tryAcquire("ip:10.0.0.4", 3, Duration.ofMinutes(1)) }
        val nextWindow = RedisLoginRateLimiter(connectionFactory, fixedClock("2026-10-01T00:06:10Z"))

        assertThat(nextWindow.tryAcquire("ip:10.0.0.4", 3, Duration.ofMinutes(1))).isTrue()
    }

    private fun fixedClock(instant: String) = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)

    companion object {
        private val redis = GenericContainer<Nothing>(DockerImageName.parse("redis:7.4-alpine")).apply {
            withExposedPorts(6379)
        }
        private lateinit var connectionFactory: LettuceConnectionFactory

        @JvmStatic
        @BeforeAll
        fun startRedis() {
            redis.start()
            connectionFactory = LettuceConnectionFactory(redis.host, redis.getMappedPort(6379)).apply {
                afterPropertiesSet()
                start()
            }
        }

        @JvmStatic
        @AfterAll
        fun stopRedis() {
            connectionFactory.destroy()
            redis.stop()
        }
    }
}
