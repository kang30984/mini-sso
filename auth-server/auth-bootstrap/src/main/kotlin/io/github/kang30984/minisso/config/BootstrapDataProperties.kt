package io.github.kang30984.minisso.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 기동 시 등록할 초기 데이터. 값이 없으면 아무것도 등록하지 않는다.
 * 비밀번호와 시크릿은 환경 변수로 주입하고, 로컬 기본값은 application-local.yml에만 둔다.
 */
@ConfigurationProperties(prefix = "minisso.bootstrap")
data class BootstrapDataProperties(
    val admin: Admin? = null,
    val clients: List<Client> = emptyList(),
) {
    data class Admin(
        val loginId: String,
        val email: String,
        val password: String,
        val name: String = "관리자",
        val department: String = "시스템",
    ) {
        override fun toString() = "Admin(loginId=$loginId, email=$email, password=****)"
    }

    data class Client(
        val clientId: String,
        val clientName: String,
        val clientSecret: String,
        val redirectUris: Set<String>,
        val postLogoutRedirectUris: Set<String> = emptySet(),
        val scopes: Set<String> = setOf("openid", "profile", "email"),
        val backchannelLogoutUri: String? = null,
    ) {
        override fun toString() = "Client(clientId=$clientId, clientSecret=****, redirectUris=$redirectUris)"
    }
}
