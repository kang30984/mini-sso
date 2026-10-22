package io.github.kang30984.minisso.application.history.port.outbound

import io.github.kang30984.minisso.application.history.port.inbound.LoginHistorySearchCondition
import io.github.kang30984.minisso.domain.history.LoginHistory

interface SearchLoginHistoryPort {
    /** id 내림차순으로 cursor보다 작은 id를 최대 limit건 조회한다 */
    fun search(condition: LoginHistorySearchCondition, cursor: Long?, limit: Int): List<LoginHistory>
}
