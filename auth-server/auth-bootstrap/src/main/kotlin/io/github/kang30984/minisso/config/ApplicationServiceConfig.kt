package io.github.kang30984.minisso.config

import io.github.kang30984.minisso.application.account.port.outbound.LoadAccountPort
import io.github.kang30984.minisso.application.account.port.outbound.SaveAccountPort
import io.github.kang30984.minisso.application.account.service.AccountCommandService
import io.github.kang30984.minisso.application.account.service.AccountQueryService
import io.github.kang30984.minisso.application.account.service.LoginService
import io.github.kang30984.minisso.application.client.port.outbound.LoadOAuthClientPort
import io.github.kang30984.minisso.application.client.port.outbound.SaveOAuthClientPort
import io.github.kang30984.minisso.application.client.service.OAuthClientService
import io.github.kang30984.minisso.application.history.port.outbound.SaveLoginHistoryPort
import io.github.kang30984.minisso.application.history.port.outbound.SearchLoginHistoryPort
import io.github.kang30984.minisso.application.history.service.LoginHistoryQueryService
import io.github.kang30984.minisso.application.session.port.outbound.BackChannelLogoutPort
import io.github.kang30984.minisso.application.session.port.outbound.SsoSessionPort
import io.github.kang30984.minisso.application.session.service.LogoutService
import io.github.kang30984.minisso.application.signingkey.port.outbound.SigningKeyCryptoPort
import io.github.kang30984.minisso.application.signingkey.port.outbound.SigningKeyPort
import io.github.kang30984.minisso.application.signingkey.service.SigningKeyService
import io.github.kang30984.minisso.domain.signingkey.SigningKeyPolicy
import org.springframework.beans.factory.annotation.Value
import java.time.Duration
import io.github.kang30984.minisso.domain.account.AccountPolicy
import io.github.kang30984.minisso.domain.account.PasswordHasher
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * 애플리케이션 계층은 Spring 어노테이션(@Service 등)을 쓰지 않으므로 빈 조립을 여기서 한다.
 * 유스케이스가 어떤 포트에 의존하는지 이 파일 하나로 드러난다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AccountPolicyProperties::class, BootstrapDataProperties::class)
class ApplicationServiceConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun accountPolicy(properties: AccountPolicyProperties): AccountPolicy = properties.toPolicy()

    @Bean
    fun accountCommandService(
        loadAccountPort: LoadAccountPort,
        saveAccountPort: SaveAccountPort,
        passwordHasher: PasswordHasher,
        accountPolicy: AccountPolicy,
        clock: Clock,
    ) = AccountCommandService(loadAccountPort, saveAccountPort, passwordHasher, accountPolicy, clock)

    @Bean
    fun loginService(
        loadAccountPort: LoadAccountPort,
        saveAccountPort: SaveAccountPort,
        saveLoginHistoryPort: SaveLoginHistoryPort,
        passwordHasher: PasswordHasher,
        accountPolicy: AccountPolicy,
        clock: Clock,
    ) = LoginService(loadAccountPort, saveAccountPort, saveLoginHistoryPort, passwordHasher, accountPolicy, clock)

    @Bean
    fun accountQueryService(loadAccountPort: LoadAccountPort, clock: Clock) =
        AccountQueryService(loadAccountPort, clock)

    @Bean
    fun loginHistoryQueryService(searchLoginHistoryPort: SearchLoginHistoryPort) =
        LoginHistoryQueryService(searchLoginHistoryPort)

    @Bean
    fun oAuthClientService(
        loadOAuthClientPort: LoadOAuthClientPort,
        saveOAuthClientPort: SaveOAuthClientPort,
        passwordHasher: PasswordHasher,
    ) = OAuthClientService(loadOAuthClientPort, saveOAuthClientPort, passwordHasher)

    @Bean
    fun signingKeyService(
        signingKeyPort: SigningKeyPort,
        signingKeyCryptoPort: SigningKeyCryptoPort,
        @Value("\${minisso.signing-key.retention:1h}") retention: Duration,
        clock: Clock,
    ) = SigningKeyService(signingKeyPort, signingKeyCryptoPort, SigningKeyPolicy(retention), clock)

    @Bean
    fun logoutService(
        ssoSessionPort: SsoSessionPort,
        loadOAuthClientPort: LoadOAuthClientPort,
        backChannelLogoutPort: BackChannelLogoutPort,
    ) = LogoutService(ssoSessionPort, loadOAuthClientPort, backChannelLogoutPort)
}
