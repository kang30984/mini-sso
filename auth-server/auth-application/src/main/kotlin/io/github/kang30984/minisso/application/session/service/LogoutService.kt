package io.github.kang30984.minisso.application.session.service

import io.github.kang30984.minisso.application.client.port.outbound.LoadOAuthClientPort
import io.github.kang30984.minisso.application.session.port.inbound.LogoutCommand
import io.github.kang30984.minisso.application.session.port.inbound.LogoutUseCase
import io.github.kang30984.minisso.application.session.port.outbound.BackChannelLogoutNotice
import io.github.kang30984.minisso.application.session.port.outbound.BackChannelLogoutPort
import io.github.kang30984.minisso.application.session.port.outbound.SsoSessionPort
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
class LogoutService(
    private val ssoSessionPort: SsoSessionPort,
    private val loadOAuthClientPort: LoadOAuthClientPort,
    private val backChannelLogoutPort: BackChannelLogoutPort,
) : LogoutUseCase {

    override fun logout(command: LogoutCommand) {
        // 토큰 폐기를 먼저 한다. 알림 전송이 실패해도 해당 세션의 토큰으로는 더 이상 갱신할 수 없다.
        val participatingClients = ssoSessionPort.revokeBySessionId(command.sessionId).distinct()

        participatingClients
            .mapNotNull(loadOAuthClientPort::findById)
            .forEach { client ->
                val logoutUri = client.backchannelLogoutUri ?: return@forEach
                backChannelLogoutPort.notify(
                    BackChannelLogoutNotice(
                        clientId = client.clientId,
                        logoutUri = logoutUri,
                        issuer = command.issuer,
                        subject = command.subject,
                        sessionId = command.sessionId,
                    ),
                )
            }
    }
}
