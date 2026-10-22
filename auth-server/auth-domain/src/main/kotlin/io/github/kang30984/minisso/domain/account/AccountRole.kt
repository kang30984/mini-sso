package io.github.kang30984.minisso.domain.account

/** 인증 서버 자체의 권한. 액세스 토큰의 roles 클레임으로 발급되어 관리자 API 접근 제어에 쓰인다. */
enum class AccountRole {
    USER,
    ADMIN,
}
