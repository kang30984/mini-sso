package io.github.kang30984.minisso.domain.account

import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode

@JvmInline
value class AccountId(val value: Long)

@JvmInline
value class LoginId(val value: String) {
    init {
        if (!PATTERN.matches(value)) throw BusinessException(ErrorCode.INVALID_LOGIN_ID)
    }

    companion object {
        private val PATTERN = Regex("^[a-z][a-z0-9._-]{3,29}$")

        /** 로그인 시도처럼 형식이 틀려도 예외 대신 "존재하지 않는 계정"으로 다뤄야 할 때 사용 */
        fun parseOrNull(raw: String): LoginId? = raw.takeIf(PATTERN::matches)?.let(::LoginId)
    }
}

@JvmInline
value class Email(val value: String) {
    init {
        if (value.length > MAX_LENGTH || !PATTERN.matches(value)) throw BusinessException(ErrorCode.INVALID_EMAIL)
    }

    companion object {
        private const val MAX_LENGTH = 255
        private val PATTERN = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}

/** 해시값이 로그나 예외 메시지에 노출되지 않도록 toString을 마스킹한다 */
@JvmInline
value class PasswordHash(val value: String) {
    override fun toString(): String = "PasswordHash(****)"
}
