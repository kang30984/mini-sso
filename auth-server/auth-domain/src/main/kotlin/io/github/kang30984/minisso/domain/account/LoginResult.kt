package io.github.kang30984.minisso.domain.account

sealed interface LoginResult {
    data object Success : LoginResult

    /** 인증은 성공했지만 비밀번호 만료로 변경이 필요한 상태 */
    data object PasswordChangeRequired : LoginResult

    data class Rejected(val reason: RejectReason) : LoginResult
}

enum class RejectReason {
    BAD_CREDENTIALS,
    LOCKED,
    DORMANT,
    WITHDRAWN,
}
