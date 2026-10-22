package io.github.kang30984.minisso.adapter.outbound.persistence.account

import io.github.kang30984.minisso.adapter.outbound.persistence.MySqlContainerConfig
import io.github.kang30984.minisso.adapter.outbound.persistence.PersistenceTestApplication
import io.github.kang30984.minisso.domain.account.Account
import io.github.kang30984.minisso.domain.account.AccountPolicy
import io.github.kang30984.minisso.domain.account.Email
import io.github.kang30984.minisso.domain.account.LoginId
import io.github.kang30984.minisso.domain.account.PasswordHash
import io.github.kang30984.minisso.domain.account.PasswordHasher
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
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest(classes = [PersistenceTestApplication::class])
@Import(MySqlContainerConfig::class)
@Transactional
class AccountPersistenceAdapterIT {

    @Autowired
    lateinit var adapter: AccountPersistenceAdapter

    @Autowired
    lateinit var entityManager: EntityManager

    private val now: Instant = Instant.parse("2026-10-01T00:00:00Z")
    private val hasher = object : PasswordHasher {
        override fun hash(rawPassword: String) = PasswordHash("hashed:$rawPassword")
        override fun matches(rawPassword: String, hash: PasswordHash) = hash.value == "hashed:$rawPassword"
    }

    @Test
    fun `저장한 계정을 로그인 ID로 다시 읽으면 같은 상태로 복원된다`() {
        val saved = adapter.save(newAccount("kim.cs"))
        clearPersistenceContext()

        val found = checkNotNull(adapter.findByLoginId(LoginId("kim.cs")))

        assertThat(found.id).isEqualTo(saved.id)
        assertThat(found.email).isEqualTo(Email("kim.cs@example.com"))
        assertThat(found.passwordChangedAt).isEqualTo(now)
        assertThat(adapter.existsByLoginId(LoginId("kim.cs"))).isTrue()
    }

    @Test
    fun `로그인 실패로 바뀐 상태가 저장된다`() {
        val id = checkNotNull(adapter.save(newAccount("lee.yh")).id)
        clearPersistenceContext()

        val account = checkNotNull(adapter.findById(id))
        account.authenticate("wrong", hasher, AccountPolicy(), now)
        adapter.save(account)
        clearPersistenceContext()

        assertThat(adapter.findById(id)?.failedLoginCount).isEqualTo(1)
    }

    @Test
    fun `같은 로그인 ID를 중복 저장하면 DUPLICATE_ACCOUNT 예외로 변환한다`() {
        adapter.save(newAccount("park.js"))

        assertThatThrownBy { adapter.save(newAccount("park.js", email = "another@example.com")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_ACCOUNT)
    }

    @Test
    fun `시각은 마이크로초 정밀도까지 보존된다`() {
        val precise = now.plus(123_456, ChronoUnit.MICROS)
        val id = checkNotNull(adapter.save(newAccount("choi.mj", at = precise)).id)
        clearPersistenceContext()

        assertThat(adapter.findById(id)?.passwordChangedAt).isEqualTo(precise)
    }

    private fun newAccount(loginId: String, email: String = "$loginId@example.com", at: Instant = now): Account =
        Account.register(
            LoginId(loginId), Email(email), "테스트", "플랫폼개발팀",
            "Sso-Passw0rd!", hasher, AccountPolicy(), at,
        )

    private fun clearPersistenceContext() {
        entityManager.flush()
        entityManager.clear()
    }
}
