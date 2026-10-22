package io.github.kang30984.minisso.application.account.port.inbound

/**
 * 최초 관리자 계정 생성. 기동 시 설정값으로 호출되며, 같은 로그인 ID가 이미 있으면 아무것도 하지 않는다.
 * 관리자 API 자체가 관리자 토큰을 요구하므로, 첫 관리자는 이 경로로만 만들 수 있다.
 */
interface EnsureAdminAccountUseCase {
    /** @return 새로 생성했으면 true */
    fun ensureAdmin(command: RegisterAccountCommand): Boolean
}
