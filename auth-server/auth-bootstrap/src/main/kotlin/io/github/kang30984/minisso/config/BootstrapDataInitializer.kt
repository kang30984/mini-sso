package io.github.kang30984.minisso.config

import io.github.kang30984.minisso.application.account.port.inbound.EnsureAdminAccountUseCase
import io.github.kang30984.minisso.application.account.port.inbound.RegisterAccountCommand
import io.github.kang30984.minisso.application.client.port.inbound.RegisterOAuthClientCommand
import io.github.kang30984.minisso.application.client.port.inbound.RegisterOAuthClientUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/** 최초 관리자와 OAuth 클라이언트를 등록한다. 이미 있으면 건너뛰므로 여러 번 기동해도 안전하다. */
@Component
class BootstrapDataInitializer(
    private val properties: BootstrapDataProperties,
    private val ensureAdminAccountUseCase: EnsureAdminAccountUseCase,
    private val registerOAuthClientUseCase: RegisterOAuthClientUseCase,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        properties.admin?.let { admin ->
            val created = ensureAdminAccountUseCase.ensureAdmin(
                RegisterAccountCommand(
                    loginId = admin.loginId,
                    email = admin.email,
                    name = admin.name,
                    department = admin.department,
                    initialPassword = admin.password,
                ),
            )
            if (created) log.info("최초 관리자 계정을 생성했습니다: {}", admin.loginId)
        }

        properties.clients.forEach { client ->
            val created = registerOAuthClientUseCase.registerIfAbsent(
                RegisterOAuthClientCommand(
                    clientId = client.clientId,
                    clientName = client.clientName,
                    clientSecret = client.clientSecret,
                    redirectUris = client.redirectUris,
                    postLogoutRedirectUris = client.postLogoutRedirectUris,
                    scopes = client.scopes,
                    backchannelLogoutUri = client.backchannelLogoutUri,
                ),
            )
            if (created) log.info("OAuth 클라이언트를 등록했습니다: {}", client.clientId)
        }
    }
}
