package io.github.kang30984.minisso.application.client.port.outbound

import io.github.kang30984.minisso.domain.client.OAuthClient

interface SaveOAuthClientPort {
    /** 동시 등록으로 유니크 제약을 위반하면 DUPLICATE_CLIENT_ID 예외를 던진다 */
    fun save(client: OAuthClient): OAuthClient
}
