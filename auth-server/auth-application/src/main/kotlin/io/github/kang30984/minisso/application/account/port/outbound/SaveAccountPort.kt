package io.github.kang30984.minisso.application.account.port.outbound

import io.github.kang30984.minisso.domain.account.Account

interface SaveAccountPort {
    /**
     * 신규 계정이면 생성하고 ID가 채워진 계정을 돌려준다.
     * 동시 가입으로 유니크 제약을 위반하면 DUPLICATE_ACCOUNT 예외를 던진다.
     */
    fun save(account: Account): Account
}
