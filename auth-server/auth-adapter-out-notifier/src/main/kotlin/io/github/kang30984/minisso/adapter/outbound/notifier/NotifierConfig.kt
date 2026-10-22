package io.github.kang30984.minisso.adapter.outbound.notifier

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.web.client.RestClient
import java.time.Clock
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class NotifierConfig {

    /** 응답하지 않는 서비스 때문에 스레드가 묶이지 않도록 짧은 타임아웃을 둔다 */
    @Bean
    fun backChannelLogoutNotifier(jwtEncoder: JwtEncoder, restClientBuilder: RestClient.Builder, clock: Clock) =
        BackChannelLogoutNotifier(
            jwtEncoder = jwtEncoder,
            restClient = restClientBuilder
                .requestFactory(
                    SimpleClientHttpRequestFactory().apply {
                        setConnectTimeout(CONNECT_TIMEOUT)
                        setReadTimeout(READ_TIMEOUT)
                    },
                )
                .build(),
            clock = clock,
        )

    companion object {
        private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(2)
        private val READ_TIMEOUT: Duration = Duration.ofSeconds(3)
    }
}
