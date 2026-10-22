package io.github.kang30984.minisso.domain.account

import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode
import java.time.Duration
import java.time.Instant

data class AccountPolicy(
    val maxFailedAttempts: Int = 5,
    val lockDuration: Duration = Duration.ofMinutes(30),
    val passwordMaxAge: Duration = Duration.ofDays(90),
    val passwordMinLength: Int = 10,
    val passwordMaxLength: Int = 64,
) {
    init {
        require(maxFailedAttempts > 0) { "maxFailedAttempts는 1 이상이어야 합니다." }
        require(lockDuration > Duration.ZERO) { "lockDuration은 0보다 커야 합니다." }
        require(passwordMaxAge > Duration.ZERO) { "passwordMaxAge는 0보다 커야 합니다." }
        require(passwordMinLength in 8..passwordMaxLength) { "비밀번호 길이 정책이 올바르지 않습니다." }
    }

    fun validatePassword(rawPassword: String, loginId: LoginId) {
        val valid = rawPassword.length in passwordMinLength..passwordMaxLength &&
            rawPassword.any { it.isLetter() } &&
            rawPassword.any { it.isDigit() } &&
            rawPassword.any { !it.isLetterOrDigit() && !it.isWhitespace() } &&
            rawPassword.none { it.isWhitespace() } &&
            !rawPassword.contains(loginId.value, ignoreCase = true)

        if (!valid) throw BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION)
    }

    fun isPasswordExpired(passwordChangedAt: Instant, now: Instant): Boolean =
        !passwordChangedAt.plus(passwordMaxAge).isAfter(now)
}
