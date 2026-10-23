package io.github.kang30984.minisso.application.account.port.outbound

import java.time.Duration

/** 로그인 시도 횟수 제한. 여러 인스턴스가 같은 카운터를 공유해야 하므로 외부 저장소(Redis)로 구현한다. */
interface LoginRateLimitPort {
    /** @return window 안에서 key의 시도가 limit 이하이면 true (이번 시도를 포함해 센다) */
    fun tryAcquire(key: String, limit: Int, window: Duration): Boolean

    companion object {
        /** 제한하지 않는 구현 (단위 테스트용) */
        val UNLIMITED: LoginRateLimitPort = object : LoginRateLimitPort {
            override fun tryAcquire(key: String, limit: Int, window: Duration): Boolean = true
        }
    }
}
