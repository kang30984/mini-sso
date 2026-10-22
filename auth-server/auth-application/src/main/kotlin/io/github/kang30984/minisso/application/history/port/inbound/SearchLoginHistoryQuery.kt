package io.github.kang30984.minisso.application.history.port.inbound

import io.github.kang30984.minisso.application.common.CursorPage
import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode
import io.github.kang30984.minisso.domain.history.LoginOutcome
import java.time.Duration
import java.time.Instant

interface SearchLoginHistoryQuery {
    fun search(condition: LoginHistorySearchCondition, cursor: Long?, size: Int): CursorPage<LoginHistoryView>
}

/**
 * 조회 기간을 필수로 받고 최대 31일로 제한한다.
 * 이력 테이블은 계속 커지므로 기간 없는 조회가 전체 스캔으로 이어지는 것을 막기 위함이다.
 */
data class LoginHistorySearchCondition(
    val from: Instant,
    val to: Instant,
    val outcome: LoginOutcome? = null,
    val clientId: String? = null,
    val ip: String? = null,
    val loginIdPrefix: String? = null,
) {
    init {
        if (!from.isBefore(to) || Duration.between(from, to) > MAX_PERIOD) {
            throw BusinessException(ErrorCode.INVALID_SEARCH_PERIOD)
        }
    }

    companion object {
        val MAX_PERIOD: Duration = Duration.ofDays(31)
    }
}

data class LoginHistoryView(
    val id: Long,
    val accountId: Long?,
    val loginId: String,
    val clientId: String?,
    val ip: String,
    val userAgent: String?,
    val outcome: String,
    val failureReason: String?,
    val occurredAt: Instant,
)
