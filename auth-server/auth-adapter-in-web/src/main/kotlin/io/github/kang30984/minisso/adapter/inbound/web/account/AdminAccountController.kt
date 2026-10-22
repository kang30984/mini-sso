package io.github.kang30984.minisso.adapter.inbound.web.account

import io.github.kang30984.minisso.application.account.port.inbound.GetAccountQuery
import io.github.kang30984.minisso.application.account.port.inbound.RegisterAccountUseCase
import io.github.kang30984.minisso.application.account.port.inbound.UnlockAccountUseCase
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * 관리자 계정 API.
 * 3주차에 Spring Security를 적용해 ADMIN 권한만 접근하도록 제한한다.
 */
@RestController
@RequestMapping("/api/admin/v1/accounts")
class AdminAccountController(
    private val registerAccountUseCase: RegisterAccountUseCase,
    private val unlockAccountUseCase: UnlockAccountUseCase,
    private val getAccountQuery: GetAccountQuery,
) {

    @PostMapping
    fun register(@Valid @RequestBody request: RegisterAccountRequest): ResponseEntity<AccountIdResponse> {
        val id = registerAccountUseCase.register(request.toCommand())
        val location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri()
        return ResponseEntity.created(location).body(AccountIdResponse(id))
    }

    @GetMapping("/{accountId}")
    fun get(@PathVariable accountId: Long): AccountResponse =
        AccountResponse.from(getAccountQuery.getAccount(accountId))

    @PostMapping("/{accountId}/unlock")
    fun unlock(@PathVariable accountId: Long): ResponseEntity<Unit> {
        unlockAccountUseCase.unlock(accountId)
        return ResponseEntity.noContent().build()
    }
}
