package io.github.kang30984.minisso.adapter.inbound.oauth2.authentication

import java.io.Serializable
import java.security.Principal

/**
 * 로그인한 사용자. getName()은 토큰의 sub 클레임이 되므로 변하지 않는 계정 ID를 쓴다.
 * (로그인 ID나 이메일은 바뀔 수 있어 서비스 간 사용자 식별자로 부적합하다)
 */
data class MiniSsoPrincipal(
    val accountId: Long,
    val loginId: String,
) : Principal, Serializable {
    override fun getName(): String = accountId.toString()

    companion object {
        private const val serialVersionUID = 1L
    }
}
