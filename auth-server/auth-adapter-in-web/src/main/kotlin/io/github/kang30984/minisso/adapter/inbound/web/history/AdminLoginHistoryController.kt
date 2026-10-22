package io.github.kang30984.minisso.adapter.inbound.web.history

import io.github.kang30984.minisso.application.common.CursorPage
import io.github.kang30984.minisso.application.history.port.inbound.LoginHistorySearchCondition
import io.github.kang30984.minisso.application.history.port.inbound.LoginHistoryView
import io.github.kang30984.minisso.application.history.port.inbound.SearchLoginHistoryQuery
import io.github.kang30984.minisso.domain.history.LoginOutcome
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/admin/v1/login-histories")
class AdminLoginHistoryController(
    private val searchLoginHistoryQuery: SearchLoginHistoryQuery,
) {

    /**
     * 예: GET /api/admin/v1/login-histories?from=2026-10-01T00:00:00Z&to=2026-10-02T00:00:00Z&outcome=FAILURE&size=50
     * 다음 페이지는 응답의 nextCursor를 cursor 파라미터로 넘긴다.
     */
    @GetMapping
    fun search(
        @RequestParam from: Instant,
        @RequestParam to: Instant,
        @RequestParam(required = false) outcome: LoginOutcome?,
        @RequestParam(required = false) clientId: String?,
        @RequestParam(required = false) ip: String?,
        @RequestParam(required = false) loginIdPrefix: String?,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") size: Int,
    ): CursorPage<LoginHistoryView> =
        searchLoginHistoryQuery.search(
            LoginHistorySearchCondition(from, to, outcome, clientId, ip, loginIdPrefix),
            cursor,
            size,
        )
}
