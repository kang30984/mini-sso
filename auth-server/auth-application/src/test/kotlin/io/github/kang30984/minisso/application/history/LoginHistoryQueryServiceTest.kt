package io.github.kang30984.minisso.application.history

import io.github.kang30984.minisso.application.fake.InMemoryLoginHistoryRepository
import io.github.kang30984.minisso.application.history.port.inbound.LoginHistorySearchCondition
import io.github.kang30984.minisso.application.history.service.LoginHistoryQueryService
import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode
import io.github.kang30984.minisso.domain.history.LoginHistory
import io.github.kang30984.minisso.domain.history.LoginOutcome
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class LoginHistoryQueryServiceTest {

    private val base = Instant.parse("2026-10-01T00:00:00Z")
    private val repository = InMemoryLoginHistoryRepository()
    private val service = LoginHistoryQueryService(repository)
    private val condition = LoginHistorySearchCondition(from = base, to = base.plus(Duration.ofDays(1)))

    @Test
    fun `size보다 많으면 다음 커서를 돌려주고 커서로 이어서 조회한다`() {
        repeat(5) { saveAt(base.plusSeconds(it.toLong())) }

        val first = service.search(condition, cursor = null, size = 3)
        val second = service.search(condition, cursor = first.nextCursor, size = 3)

        assertThat(first.items.map { it.id }).containsExactly(5L, 4L, 3L)
        assertThat(first.hasNext).isTrue()
        assertThat(second.items.map { it.id }).containsExactly(2L, 1L)
        assertThat(second.hasNext).isFalse()
    }

    @Test
    fun `조회 기간이 31일을 넘으면 거부한다`() {
        assertThatThrownBy { LoginHistorySearchCondition(from = base, to = base.plus(Duration.ofDays(32))) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SEARCH_PERIOD)
    }

    @Test
    fun `시작 시각이 종료 시각보다 늦으면 거부한다`() {
        assertThatThrownBy { LoginHistorySearchCondition(from = base, to = base.minusSeconds(1)) }
            .isInstanceOf(BusinessException::class.java)
    }

    private fun saveAt(at: Instant) {
        repository.save(
            LoginHistory.attempt(null, "user", null, "10.0.0.1", null, LoginOutcome.SUCCESS, null, at),
        )
    }
}
