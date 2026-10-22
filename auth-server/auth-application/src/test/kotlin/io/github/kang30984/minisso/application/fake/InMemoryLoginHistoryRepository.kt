package io.github.kang30984.minisso.application.fake

import io.github.kang30984.minisso.application.history.port.inbound.LoginHistorySearchCondition
import io.github.kang30984.minisso.application.history.port.outbound.SaveLoginHistoryPort
import io.github.kang30984.minisso.application.history.port.outbound.SearchLoginHistoryPort
import io.github.kang30984.minisso.domain.history.LoginHistory

class InMemoryLoginHistoryRepository : SaveLoginHistoryPort, SearchLoginHistoryPort {
    val histories = mutableListOf<LoginHistory>()

    override fun save(history: LoginHistory) {
        histories += history.copy(id = histories.size + 1L)
    }

    override fun search(condition: LoginHistorySearchCondition, cursor: Long?, limit: Int): List<LoginHistory> =
        histories
            .filter { !it.occurredAt.isBefore(condition.from) && it.occurredAt.isBefore(condition.to) }
            .filter { condition.outcome == null || it.outcome == condition.outcome }
            .filter { cursor == null || it.id!! < cursor }
            .sortedByDescending { it.id }
            .take(limit)
}
