package io.github.kang30984.minisso.application.client.port.inbound

interface RegisterOAuthClientUseCase {
    /** 같은 clientId가 없을 때만 등록한다. @return 새로 등록했으면 true */
    fun registerIfAbsent(command: RegisterOAuthClientCommand): Boolean
}

data class RegisterOAuthClientCommand(
    val clientId: String,
    val clientName: String,
    val clientSecret: String,
    val redirectUris: Set<String>,
    val postLogoutRedirectUris: Set<String>,
    val scopes: Set<String>,
) {
    override fun toString(): String =
        "RegisterOAuthClientCommand(clientId=$clientId, clientName=$clientName, clientSecret=****, " +
            "redirectUris=$redirectUris, postLogoutRedirectUris=$postLogoutRedirectUris, scopes=$scopes)"
}
