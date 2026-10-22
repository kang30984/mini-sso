package io.github.kang30984.minisso.domain.account

/**
 * 계정의 생명주기 상태.
 * 로그인 실패로 인한 잠금은 시간이 지나면 풀리는 일시적 상태이므로 status가 아닌 lockedUntil로 표현한다.
 */
enum class AccountStatus {
    ACTIVE,
    DORMANT,
    WITHDRAWN,
}
