package io.github.kang30984.minisso.adapter.outbound.persistence.client

import io.github.kang30984.minisso.adapter.outbound.persistence.MySqlContainerConfig
import io.github.kang30984.minisso.adapter.outbound.persistence.PersistenceTestApplication
import io.github.kang30984.minisso.domain.account.PasswordHash
import io.github.kang30984.minisso.domain.account.PasswordHasher
import io.github.kang30984.minisso.domain.client.OAuthClient
import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [PersistenceTestApplication::class])
@Import(MySqlContainerConfig::class)
@Transactional
class OAuthClientPersistenceAdapterIT {

    @Autowired
    lateinit var adapter: OAuthClientPersistenceAdapter

    @Autowired
    lateinit var entityManager: EntityManager

    private val hasher = object : PasswordHasher {
        override fun hash(rawPassword: String) = PasswordHash("hashed:$rawPassword")
        override fun matches(rawPassword: String, hash: PasswordHash) = hash.value == "hashed:$rawPassword"
    }

    @Test
    fun `여러 개의 리다이렉트 URI와 scope가 그대로 복원된다`() {
        adapter.save(client("app-one"))
        entityManager.flush()
        entityManager.clear()

        val found = checkNotNull(adapter.findByClientId("app-one"))

        assertThat(found.redirectUris).containsExactlyInAnyOrder(
            "http://127.0.0.1:8081/login/oauth2/code/mini-sso",
            "https://app-one.example.com/login/oauth2/code/mini-sso",
        )
        assertThat(found.scopes).containsExactlyInAnyOrder("openid", "profile", "email")
        assertThat(found.postLogoutRedirectUris).isEmpty()
    }

    @Test
    fun `같은 clientId는 DUPLICATE_CLIENT_ID로 변환한다`() {
        adapter.save(client("app-two"))

        assertThatThrownBy { adapter.save(client("app-two")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_CLIENT_ID)
    }

    private fun client(clientId: String) = OAuthClient.register(
        clientId = clientId,
        clientName = clientId,
        rawSecret = "local-secret-1234567890",
        redirectUris = setOf(
            "http://127.0.0.1:8081/login/oauth2/code/mini-sso",
            "https://$clientId.example.com/login/oauth2/code/mini-sso",
        ),
        postLogoutRedirectUris = emptySet(),
        scopes = setOf("openid", "profile", "email"),
        hasher = hasher,
    )
}
