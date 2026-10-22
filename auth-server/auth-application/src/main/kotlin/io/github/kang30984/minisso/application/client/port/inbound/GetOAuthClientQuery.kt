package io.github.kang30984.minisso.application.client.port.inbound

/** 인가 서버가 요청마다 클라이언트 정보를 조회할 때 사용한다 */
interface GetOAuthClientQuery {
    fun findByClientId(clientId: String): OAuthClientView?

    fun findById(id: Long): OAuthClientView?
}

data class OAuthClientView(
    val id: Long,
    val clientId: String,
    val clientName: String,
    /** 인가 서버가 클라이언트 인증 시 비교할 해시값. 원문 시크릿은 어디에도 저장하지 않는다. */
    val clientSecretHash: String,
    val redirectUris: Set<String>,
    val postLogoutRedirectUris: Set<String>,
    val scopes: Set<String>,
) {
    override fun toString(): String =
        "OAuthClientView(id=$id, clientId=$clientId, clientName=$clientName, clientSecretHash=****, " +
            "redirectUris=$redirectUris, postLogoutRedirectUris=$postLogoutRedirectUris, scopes=$scopes)"
}
