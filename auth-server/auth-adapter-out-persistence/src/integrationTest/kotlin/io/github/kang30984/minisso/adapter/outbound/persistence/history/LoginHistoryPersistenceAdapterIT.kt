package io.github.kang30984.minisso.adapter.outbound.persistence.history

import io.github.kang30984.minisso.adapter.outbound.persistence.MySqlContainerConfig
import io.github.kang30984.minisso.adapter.outbound.persistence.PersistenceTestApplication
import io.github.kang30984.minisso.application.history.port.inbound.LoginHistorySearchCondition
import io.github.kang30984.minisso.domain.account.AccountId
import io.github.kang30984.minisso.domain.history.LoginFailureReason
import io.github.kang30984.minisso.domain.history.LoginHistory
import io.github.kang30984.minisso.domain.history.LoginOutcome
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@SpringBootTest(classes = [PersistenceTestApplication::class])
@Import(MySqlContainerConfig::class)
@Transactional
class LoginHistoryPersistenceAdapterIT {

    @Autowired
    lateinit var adapter: LoginHistoryPersistenceAdapter

    private val base: Instant = Instant.parse("2026-10-01T00:00:00Z")

    @BeforeEach
    fun seed() {
        save("kim.cs", LoginOutcome.SUCCESS, null, "10.0.0.1", base.plusSeconds(10))
        save("kim.cs", LoginOutcome.FAILURE, LoginFailureReason.BAD_CREDENTIALS, "10.0.0.2", base.plusSeconds(20))
        save("lee.yh", LoginOutcome.FAILURE, LoginFailureReason.LOCKED, "10.0.0.2", base.plusSeconds(30))
        save("nobody", LoginOutcome.FAILURE, LoginFailureReason.UNKNOWN_ACCOUNT, "10.0.0.3", base.plusSeconds(40))
        save("kim.cs", LoginOutcome.SUCCESS, null, "10.0.0.1", base.minus(Duration.ofDays(1)))   // 기간 밖
    }

    @Test
    fun `기간 안의 이력만 최신순으로 조회한다`() {
        val result = adapter.search(condition(), cursor = null, limit = 10)

        assertThat(result.map { it.loginId }).containsExactly("nobody", "lee.yh", "kim.cs", "kim.cs")
    }

    @Test
    fun `결과와 IP 조건을 함께 적용한다`() {
        val result = adapter.search(condition(outcome = LoginOutcome.FAILURE, ip = "10.0.0.2"), cursor = null, limit = 10)

        assertThat(result.map { it.failureReason })
            .containsExactly(LoginFailureReason.LOCKED, LoginFailureReason.BAD_CREDENTIALS)
    }

    @Test
    fun `로그인 ID 접두어로 검색한다`() {
        val result = adapter.search(condition(loginIdPrefix = "kim"), cursor = null, limit = 10)

        assertThat(result).hasSize(2).allMatch { it.loginId == "kim.cs" }
    }

    @Test
    fun `커서보다 작은 id만 조회한다`() {
        val firstPage = adapter.search(condition(), cursor = null, limit = 2)
        val secondPage = adapter.search(condition(), cursor = firstPage.last().id, limit = 2)

        assertThat(secondPage.map { it.loginId }).containsExactly("kim.cs", "kim.cs")
        assertThat(secondPage.first().id).isLessThan(firstPage.last().id)
    }

    private fun condition(
        outcome: LoginOutcome? = null,
        ip: String? = null,
        loginIdPrefix: String? = null,
    ) = LoginHistorySearchCondition(
        from = base,
        to = base.plus(Duration.ofHours(1)),
        outcome = outcome,
        ip = ip,
        loginIdPrefix = loginIdPrefix,
    )

    private fun save(loginId: String, outcome: LoginOutcome, reason: LoginFailureReason?, ip: String, at: Instant) {
        val accountId = if (loginId == "nobody") null else AccountId(1L)
        adapter.save(LoginHistory.attempt(accountId, loginId, "sample-app-a", ip, "JUnit", outcome, reason, at))
    }
}
