package io.github.kang30984.minisso.config

import io.github.kang30984.minisso.domain.account.AccountPolicy
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "minisso.account")
data class AccountPolicyProperties(
    val maxFailedAttempts: Int = 5,
    val lockDuration: Duration = Duration.ofMinutes(30),
    val passwordMaxAge: Duration = Duration.ofDays(90),
) {
    fun toPolicy() = AccountPolicy(
        maxFailedAttempts = maxFailedAttempts,
        lockDuration = lockDuration,
        passwordMaxAge = passwordMaxAge,
    )
}
