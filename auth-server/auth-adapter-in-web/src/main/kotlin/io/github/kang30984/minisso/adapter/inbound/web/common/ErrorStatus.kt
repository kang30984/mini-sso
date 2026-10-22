package io.github.kang30984.minisso.adapter.inbound.web.common

import io.github.kang30984.minisso.domain.common.ErrorCode
import org.springframework.http.HttpStatus

/** 에러 코드와 HTTP 상태의 매핑은 웹 어댑터의 책임이다. when을 exhaustive하게 두어 코드 추가 시 컴파일 오류로 누락을 막는다. */
internal fun ErrorCode.httpStatus(): HttpStatus = when (this) {
    ErrorCode.INVALID_INPUT,
    ErrorCode.INVALID_LOGIN_ID,
    ErrorCode.INVALID_EMAIL,
    ErrorCode.PASSWORD_POLICY_VIOLATION,
    ErrorCode.CURRENT_PASSWORD_MISMATCH,
    ErrorCode.PASSWORD_REUSED,
    ErrorCode.INVALID_PROFILE,
    ErrorCode.INVALID_SEARCH_PERIOD,
    ErrorCode.INVALID_CLIENT_ID,
    ErrorCode.INVALID_CLIENT_SECRET,
    ErrorCode.INVALID_REDIRECT_URI,
    ErrorCode.INVALID_SCOPE,
    ErrorCode.INVALID_CLIENT_NAME,
    -> HttpStatus.BAD_REQUEST

    ErrorCode.ACCOUNT_NOT_FOUND -> HttpStatus.NOT_FOUND

    ErrorCode.ACCOUNT_NOT_ACTIVE,
    ErrorCode.DUPLICATE_LOGIN_ID,
    ErrorCode.DUPLICATE_EMAIL,
    ErrorCode.DUPLICATE_ACCOUNT,
    ErrorCode.DUPLICATE_CLIENT_ID,
    -> HttpStatus.CONFLICT
}
