package io.github.kang30984.minisso.adapter.outbound.redis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

class RedisOAuth2AuthorizationServiceIT {

    private val service = RedisOAuth2AuthorizationService(connectionFactory, Clock.systemUTC())
    private val now: Instant = Instant.now()

    @Test
    fun `저장한 인가를 각 토큰으로 찾을 수 있다`() {
        val authorization = authorization(accessToken = "at-1", refreshToken = "rt-1", sid = "sid-find")

        service.save(authorization)

        assertThat(service.findByToken("at-1", OAuth2TokenType.ACCESS_TOKEN)?.id).isEqualTo(authorization.id)
        assertThat(service.findByToken("rt-1", OAuth2TokenType.REFRESH_TOKEN)?.id).isEqualTo(authorization.id)
        assertThat(service.findByToken("rt-1", null)?.id).isEqualTo(authorization.id)
        assertThat(service.findById(authorization.id)?.principalName).isEqualTo("42")
    }

    @Test
    fun `리프레시 토큰이 회전되면 이전 토큰은 더 이상 찾을 수 없다`() {
        val original = authorization(accessToken = "at-2", refreshToken = "rt-2a", sid = "sid-rotate")
        service.save(original)

        service.save(rotate(original, accessToken = "at-2b", refreshToken = "rt-2b"))

        assertThat(service.findByToken("at-2", OAuth2TokenType.ACCESS_TOKEN)).isNull()
        assertThat(service.findByToken("rt-2b", OAuth2TokenType.REFRESH_TOKEN)).isNotNull
    }

    @Test
    fun `회전된 리프레시 토큰을 재사용하면 인가 전체가 폐기된다`() {
        val original = authorization(accessToken = "at-3", refreshToken = "rt-3a", sid = "sid-reuse")
        service.save(original)
        service.save(rotate(original, accessToken = "at-3b", refreshToken = "rt-3b"))

        // 탈취된 이전 토큰으로 갱신 시도
        assertThat(service.findByToken("rt-3a", OAuth2TokenType.REFRESH_TOKEN)).isNull()

        // 정상 클라이언트가 가진 최신 토큰도 함께 무효화된다
        assertThat(service.findByToken("rt-3b", OAuth2TokenType.REFRESH_TOKEN)).isNull()
        assertThat(service.findByToken("at-3b", OAuth2TokenType.ACCESS_TOKEN)).isNull()
        assertThat(service.findById(original.id)).isNull()
    }

    @Test
    fun `SSO 세션 단위로 인가를 일괄 폐기하고 참여한 클라이언트를 돌려준다`() {
        service.save(authorization(accessToken = "at-4a", refreshToken = "rt-4a", sid = "sid-logout", clientId = "7"))
        service.save(authorization(accessToken = "at-4b", refreshToken = "rt-4b", sid = "sid-logout", clientId = "8"))
        service.save(authorization(accessToken = "at-4c", refreshToken = "rt-4c", sid = "sid-other", clientId = "9"))

        val clients = service.revokeBySessionId("sid-logout")

        assertThat(clients.map { it.value }).containsExactlyInAnyOrder(7L, 8L)
        assertThat(service.findByToken("rt-4a", OAuth2TokenType.REFRESH_TOKEN)).isNull()
        assertThat(service.findByToken("rt-4b", OAuth2TokenType.REFRESH_TOKEN)).isNull()
        assertThat(service.findByToken("rt-4c", OAuth2TokenType.REFRESH_TOKEN)).isNotNull
    }

    private fun authorization(
        accessToken: String,
        refreshToken: String,
        sid: String,
        clientId: String = "1",
    ): OAuth2Authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient(clientId))
            .id(UUID.randomUUID().toString())
            .principalName("42")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(setOf("openid"))
            .token(OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, accessToken, now, now.plus(Duration.ofMinutes(10))))
            .refreshToken(OAuth2RefreshToken(refreshToken, now, now.plus(Duration.ofDays(14))))
            .token(idToken(sid))
            .build()

    private fun rotate(authorization: OAuth2Authorization, accessToken: String, refreshToken: String) =
        OAuth2Authorization.from(authorization)
            .token(OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, accessToken, now, now.plus(Duration.ofMinutes(10))))
            .refreshToken(OAuth2RefreshToken(refreshToken, now, now.plus(Duration.ofDays(14))))
            .build()

    private fun idToken(sid: String) = OidcIdToken.withTokenValue("id-token-${UUID.randomUUID()}")
        .subject("42")
        .issuedAt(now)
        .expiresAt(now.plus(Duration.ofMinutes(30)))
        .claim("sid", sid)
        .build()

    private fun registeredClient(id: String) = RegisteredClient.withId(id)
        .clientId("client-$id")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("https://client-$id.example.com/callback")
        .build()

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
