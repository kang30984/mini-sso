package io.github.kang30984.minisso.sample.b

import io.github.kang30984.minisso.sdk.core.AuthUser
import io.github.kang30984.minisso.sdk.spring.api.CurrentUser
import io.github.kang30984.minisso.sdk.spring.api.RequiresScope
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 보안 관련 코드는 하나도 없다. 토큰 검증, 401/403 응답, 사용자 주입은 모두 SDK가 처리한다.
 */
@RestController
@RequestMapping("/api")
class ApiController {

    @GetMapping("/public/status")
    fun status() = mapOf("service" to "sample-app-b", "status" to "UP")

    @GetMapping("/me")
    fun me(@CurrentUser user: AuthUser) = mapOf(
        "service" to "sample-app-b",
        "accountId" to user.accountId(),
        "roles" to user.roles(),
        "calledBy" to user.clientId(),
    )

    @RequiresScope("profile")
    @GetMapping("/team-reports")
    fun teamReports(@CurrentUser user: AuthUser) = mapOf(
        "requestedBy" to user.accountId(),
        "reports" to listOf("10월 접속 통계", "보안 점검 결과"),
    )
}
