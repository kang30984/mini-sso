package io.github.kang30984.minisso.adapter.outbound.redis

import io.github.kang30984.minisso.application.account.port.outbound.LoginRateLimitPort
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration

/**
 * 고정 윈도 카운터. 키에 윈도 번호를 넣어, 만료 설정이 누락되어도 다음 윈도에는 새 키를 쓰므로 영구 차단이 생기지 않는다.
 * 키: minisso:ratelimit:login:{key}:{윈도 번호}
 */
@Component
class RedisLoginRateLimiter(
    connectionFactory: RedisConnectionFactory,
    private val clock: Clock,
) : LoginRateLimitPort {

    private val redis = StringRedisTemplate(connectionFactory)

    override fun tryAcquire(key: String, limit: Int, window: Duration): Boolean {
        val windowIndex = clock.millis() / window.toMillis()
        val redisKey = "minisso:ratelimit:login:$key:$windowIndex"
        val count = redis.opsForValue().increment(redisKey) ?: return true
        if (count == 1L) redis.expire(redisKey, window)
        return count <= limit
    }
}
