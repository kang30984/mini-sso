package io.github.kang30984.minisso.domain.history

import io.github.kang30984.minisso.domain.account.AccountId
import io.github.kang30984.minisso.domain.account.RejectReason
import java.time.Instant

enum class LoginOutcome {
    SUCCESS,
    PASSWORD_CHANGE_REQUIRED,
    FAILURE,
}

enum class LoginFailureReason {
    BAD_CREDENTIALS,
    LOCKED,
    DORMANT,
    WITHDRAWN,
    UNKNOWN_ACCOUNT,

    /** 같은 IP의 시도가 너무 많아 비밀번호를 확인하지 않고 거부함 */
    RATE_LIMITED,
    ;

    companion object {
        fun from(reason: RejectReason): LoginFailureReason = when (reason) {
            RejectReason.BAD_CREDENTIALS -> BAD_CREDENTIALS
            RejectReason.LOCKED -> LOCKED
            RejectReason.DORMANT -> DORMANT
            RejectReason.WITHDRAWN -> WITHDRAWN
        }
    }
}

/**
 * 로그인 시도 1건. 존재하지 않는 계정에 대한 시도도 기록해 무차별 대입 탐지에 활용한다.
 * loginId는 사용자가 입력한 원문이므로 길이를 제한해 저장한다.
 */
data class LoginHistory(
    val id: Long? = null,
    val accountId: AccountId?,
    val loginId: String,
    val clientId: String?,
    val ip: String,
    val userAgent: String?,
    val outcome: LoginOutcome,
    val failureReason: LoginFailureReason?,
    val occurredAt: Instant,
) {
    companion object {
        const val MAX_LOGIN_ID_LENGTH = 100
        const val MAX_CLIENT_ID_LENGTH = 100
        const val MAX_IP_LENGTH = 45
        const val MAX_USER_AGENT_LENGTH = 512

        fun attempt(
            accountId: AccountId?,
            loginId: String,
            clientId: String?,
            ip: String,
            userAgent: String?,
            outcome: LoginOutcome,
            failureReason: LoginFailureReason?,
            occurredAt: Instant,
        ): LoginHistory = LoginHistory(
            accountId = accountId,
            loginId = loginId.take(MAX_LOGIN_ID_LENGTH),
            clientId = clientId?.take(MAX_CLIENT_ID_LENGTH),
            ip = ip.take(MAX_IP_LENGTH),
            userAgent = userAgent?.take(MAX_USER_AGENT_LENGTH),
            outcome = outcome,
            failureReason = failureReason,
            occurredAt = occurredAt,
        )
    }
}
