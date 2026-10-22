package io.github.kang30984.minisso.domain.common

/**
 * 서비스 전체에서 공유하는 에러 코드. HTTP 상태 매핑은 웹 어댑터가 담당한다.
 * 목록은 docs/error-codes.md와 동기화한다.
 */
enum class ErrorCode(val code: String, val message: String) {
    INVALID_INPUT("COMMON-001", "요청 값이 올바르지 않습니다."),

    INVALID_LOGIN_ID("ACCOUNT-001", "로그인 ID는 영문 소문자로 시작하고 영문 소문자, 숫자, '.', '_', '-'로 이루어진 4~30자여야 합니다."),
    INVALID_EMAIL("ACCOUNT-002", "이메일 형식이 올바르지 않습니다."),
    PASSWORD_POLICY_VIOLATION("ACCOUNT-003", "비밀번호는 10~64자로 영문, 숫자, 특수문자를 모두 포함해야 하며 공백과 로그인 ID를 포함할 수 없습니다."),
    CURRENT_PASSWORD_MISMATCH("ACCOUNT-004", "현재 비밀번호가 일치하지 않습니다."),
    PASSWORD_REUSED("ACCOUNT-005", "현재 비밀번호와 다른 비밀번호를 사용해야 합니다."),
    INVALID_PROFILE("ACCOUNT-006", "이름은 1~50자, 부서는 1~100자여야 합니다."),

    ACCOUNT_NOT_FOUND("ACCOUNT-101", "계정을 찾을 수 없습니다."),
    ACCOUNT_NOT_ACTIVE("ACCOUNT-102", "활성 상태가 아닌 계정입니다."),

    DUPLICATE_LOGIN_ID("ACCOUNT-201", "이미 사용 중인 로그인 ID입니다."),
    DUPLICATE_EMAIL("ACCOUNT-202", "이미 사용 중인 이메일입니다."),
    DUPLICATE_ACCOUNT("ACCOUNT-203", "이미 존재하는 계정입니다."),

    INVALID_SEARCH_PERIOD("HISTORY-001", "조회 기간은 시작 시각이 종료 시각보다 앞서야 하며 최대 31일입니다."),
}
