package io.github.kang30984.minisso.adapter.inbound.web.account

import io.github.kang30984.minisso.application.account.port.inbound.AccountView
import io.github.kang30984.minisso.application.account.port.inbound.RegisterAccountCommand
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

/** 형식 검증(필수값, 길이)만 여기서 하고, 비즈니스 규칙(비밀번호 정책 등)은 도메인이 검증한다 */
data class RegisterAccountRequest(
    @field:NotBlank @field:Size(max = 30)
    val loginId: String?,
    @field:NotBlank @field:Email @field:Size(max = 255)
    val email: String?,
    @field:NotBlank @field:Size(max = 50)
    val name: String?,
    @field:NotBlank @field:Size(max = 100)
    val department: String?,
    @field:NotBlank @field:Size(max = 64)
    val initialPassword: String?,
) {
    fun toCommand() = RegisterAccountCommand(
        loginId = loginId!!,
        email = email!!,
        name = name!!,
        department = department!!,
        initialPassword = initialPassword!!,
    )

    override fun toString(): String =
        "RegisterAccountRequest(loginId=$loginId, email=$email, name=$name, department=$department, initialPassword=****)"
}

data class AccountIdResponse(val id: Long)

data class AccountResponse(
    val id: Long,
    val loginId: String,
    val email: String,
    val name: String,
    val department: String,
    val status: String,
    val locked: Boolean,
    val lockedUntil: Instant?,
    val lastLoginAt: Instant?,
    val passwordChangedAt: Instant,
) {
    companion object {
        fun from(view: AccountView) = AccountResponse(
            id = view.id,
            loginId = view.loginId,
            email = view.email,
            name = view.name,
            department = view.department,
            status = view.status,
            locked = view.locked,
            lockedUntil = view.lockedUntil,
            lastLoginAt = view.lastLoginAt,
            passwordChangedAt = view.passwordChangedAt,
        )
    }
}
