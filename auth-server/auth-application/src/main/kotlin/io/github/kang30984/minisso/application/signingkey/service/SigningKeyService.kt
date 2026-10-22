package io.github.kang30984.minisso.application.signingkey.service

import io.github.kang30984.minisso.application.signingkey.port.inbound.EnsureSigningKeyUseCase
import io.github.kang30984.minisso.application.signingkey.port.inbound.GetSigningKeysQuery
import io.github.kang30984.minisso.application.signingkey.port.inbound.ListSigningKeysQuery
import io.github.kang30984.minisso.application.signingkey.port.inbound.RotateSigningKeyUseCase
import io.github.kang30984.minisso.application.signingkey.port.inbound.SigningKeyView
import io.github.kang30984.minisso.application.signingkey.port.outbound.SigningKeyCryptoPort
import io.github.kang30984.minisso.application.signingkey.port.outbound.SigningKeyPort
import io.github.kang30984.minisso.domain.signingkey.SigningKey
import io.github.kang30984.minisso.domain.signingkey.SigningKeyPolicy
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Transactional
class SigningKeyService(
    private val signingKeyPort: SigningKeyPort,
    private val cryptoPort: SigningKeyCryptoPort,
    private val policy: SigningKeyPolicy,
    private val clock: Clock,
) : GetSigningKeysQuery, EnsureSigningKeyUseCase, RotateSigningKeyUseCase, ListSigningKeysQuery {

    override fun ensureActiveKey(): Boolean {
        if (activeKey() != null) return false
        signingKeyPort.save(newKey(clock.instant()))
        return true
    }

    /**
     * 새 키를 즉시 활성화하고 이전 키는 보존 기간 동안 게시한다.
     * 이미 발급된 토큰은 이전 키로, 새 토큰은 새 키로 검증되므로 교체 중에도 검증 실패가 없다.
     */
    override fun rotate(): String {
        val now = clock.instant()
        activeKeys().forEach { key ->
            key.retire(now)
            signingKeyPort.save(key)
        }
        return signingKeyPort.save(newKey(now)).kid
    }

    override fun signingJwk(): String {
        val key = activeKey() ?: signingKeyPort.save(newKey(clock.instant()))
        return cryptoPort.decryptPrivateJwk(key.kid, key.encryptedPrivateJwk)
    }

    @Transactional(readOnly = true)
    override fun publishedPublicJwks(): List<String> {
        val now = clock.instant()
        return signingKeyPort.findAll().filter { it.isPublished(now, policy) }.map { it.publicJwk }
    }

    @Transactional(readOnly = true)
    override fun list(): List<SigningKeyView> {
        val now = clock.instant()
        return signingKeyPort.findAll()
            .sortedWith(compareBy<SigningKey> { !it.isActive }.thenByDescending { it.activatedAt })
            .map { SigningKeyView(it.kid, it.status.name, it.isPublished(now, policy), it.activatedAt, it.rotatedAt) }
    }

    private fun activeKeys(): List<SigningKey> = signingKeyPort.findAll().filter { it.isActive }

    private fun activeKey(): SigningKey? = activeKeys().maxByOrNull { it.activatedAt }

    private fun newKey(now: Instant): SigningKey {
        val generated = cryptoPort.generate()
        return SigningKey.activate(generated.kid, generated.publicJwk, generated.encryptedPrivateJwk, now)
    }
}
