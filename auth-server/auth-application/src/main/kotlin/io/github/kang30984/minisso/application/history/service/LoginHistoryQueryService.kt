package io.github.kang30984.minisso.application.history.service

import io.github.kang30984.minisso.application.common.CursorPage
import io.github.kang30984.minisso.application.history.port.inbound.LoginHistorySearchCondition
import io.github.kang30984.minisso.application.history.port.inbound.LoginHistoryView
import io.github.kang30984.minisso.application.history.port.inbound.SearchLoginHistoryQuery
import io.github.kang30984.minisso.application.history.port.outbound.SearchLoginHistoryPort
import io.github.kang30984.minisso.domain.history.LoginHistory
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
class LoginHistoryQueryService(
    private val searchLoginHistoryPort: SearchLoginHistoryPort,
) : SearchLoginHistoryQuery {

    override fun search(condition: LoginHistorySearchCondition, cursor: Long?, size: Int): CursorPage<LoginHistoryView> {
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        // 한 건 더 조회해 다음 페이지 존재 여부를 COUNT 쿼리 없이 판단한다
        val rows = searchLoginHistoryPort.search(condition, cursor, pageSize + 1)
        val items = rows.take(pageSize)
        val nextCursor = if (rows.size > pageSize) items.last().id else null
        return CursorPage(items.map { it.toView() }, nextCursor)
    }

    private fun LoginHistory.toView() = LoginHistoryView(
        id = checkNotNull(id),
        accountId = accountId?.value,
        loginId = loginId,
        clientId = clientId,
        ip = ip,
        userAgent = userAgent,
        outcome = outcome.name,
        failureReason = failureReason?.name,
        occurredAt = occurredAt,
    )

    companion object {
        const val MAX_PAGE_SIZE = 100
    }
}
