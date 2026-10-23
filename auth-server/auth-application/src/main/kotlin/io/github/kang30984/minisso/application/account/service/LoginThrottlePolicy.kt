package io.github.kang30984.minisso.application.account.service

import java.time.Duration

/**
 * IP 단위 로그인 시도 제한.
 * 계정 잠금(5회 실패)은 한 계정을 노리는 공격을 막고, 이 제한은 여러 계정을 돌아가며 시도하는 공격(credential stuffing)과
 * 비싼 Argon2 연산을 이용한 CPU 고갈 공격을 막는다.
 */
data class LoginThrottlePolicy(
    val maxAttemptsPerIp: Int = 20,
    val window: Duration = Duration.ofMinutes(1),
) {
    init {
        require(maxAttemptsPerIp > 0) { "maxAttemptsPerIp는 1 이상이어야 합니다." }
        require(window > Duration.ZERO) { "window는 0보다 커야 합니다." }
    }
}
