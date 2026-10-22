package io.github.kang30984.minisso.domain.client

import io.github.kang30984.minisso.domain.account.PasswordHash
import io.github.kang30984.minisso.domain.account.PasswordHasher
import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode

@JvmInline
value class OAuthClientId(val value: Long)

/**
 * SSO에 참여하는 서비스(Relying Party).
 * 모든 클라이언트는 기밀 클라이언트이며, 인가 코드 흐름에서 PKCE를 필수로 사용한다 (ADR-010).
 */
class OAuthClient private constructor(
    val id: OAuthClientId?,
    val clientId: String,
    val clientName: String,
    val clientSecretHash: PasswordHash,
    val redirectUris: Set<String>,
    val postLogoutRedirectUris: Set<String>,
    val scopes: Set<String>,
    /** 로그아웃 시 Logout Token을 받을 주소. 없으면 Back-Channel Logout 대상에서 제외한다. */
    val backchannelLogoutUri: String?,
) {
    companion object {
        private val CLIENT_ID_PATTERN = Regex("^[a-z][a-z0-9-]{2,49}$")
        private const val MIN_SECRET_LENGTH = 16
        private const val MAX_NAME_LENGTH = 100
        const val OPENID = "openid"
        val SUPPORTED_SCOPES: Set<String> = setOf(OPENID, "profile", "email")

        fun register(
            clientId: String,
            clientName: String,
            rawSecret: String,
            redirectUris: Set<String>,
            postLogoutRedirectUris: Set<String>,
            scopes: Set<String>,
            hasher: PasswordHasher,
            backchannelLogoutUri: String? = null,
        ): OAuthClient {
            if (!CLIENT_ID_PATTERN.matches(clientId)) throw BusinessException(ErrorCode.INVALID_CLIENT_ID)
            val trimmedName = clientName.trim()
            if (trimmedName.isEmpty() || trimmedName.length > MAX_NAME_LENGTH) {
                throw BusinessException(ErrorCode.INVALID_CLIENT_NAME)
            }
            if (rawSecret.length < MIN_SECRET_LENGTH) throw BusinessException(ErrorCode.INVALID_CLIENT_SECRET)
            if (redirectUris.isEmpty()) throw BusinessException(ErrorCode.INVALID_REDIRECT_URI)
            (redirectUris + postLogoutRedirectUris + listOfNotNull(backchannelLogoutUri)).forEach(RedirectUriPolicy::validate)
            if (OPENID !in scopes || !SUPPORTED_SCOPES.containsAll(scopes)) throw BusinessException(ErrorCode.INVALID_SCOPE)

            return OAuthClient(
                id = null,
                clientId = clientId,
                clientName = trimmedName,
                clientSecretHash = hasher.hash(rawSecret),
                redirectUris = redirectUris.toSet(),
                postLogoutRedirectUris = postLogoutRedirectUris.toSet(),
                scopes = scopes.toSet(),
                backchannelLogoutUri = backchannelLogoutUri,
            )
        }

        fun reconstitute(
            id: OAuthClientId,
            clientId: String,
            clientName: String,
            clientSecretHash: PasswordHash,
            redirectUris: Set<String>,
            postLogoutRedirectUris: Set<String>,
            scopes: Set<String>,
            backchannelLogoutUri: String?,
        ): OAuthClient = OAuthClient(
            id, clientId, clientName, clientSecretHash, redirectUris, postLogoutRedirectUris, scopes, backchannelLogoutUri,
        )
    }
}
