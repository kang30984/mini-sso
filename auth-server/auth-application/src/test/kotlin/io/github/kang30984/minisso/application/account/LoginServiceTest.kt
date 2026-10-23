package io.github.kang30984.minisso.application.account

import io.github.kang30984.minisso.application.account.port.inbound.AuthenticationResult
import io.github.kang30984.minisso.application.account.port.inbound.LoginCommand
import io.github.kang30984.minisso.application.account.service.LoginService
import io.github.kang30984.minisso.application.fake.InMemoryAccountRepository
import io.github.kang30984.minisso.application.fake.InMemoryLoginHistoryRepository
import io.github.kang30984.minisso.application.fake.MutableClock
import io.github.kang30984.minisso.domain.account.AccountPolicy
import io.github.kang30984.minisso.domain.fixture.AccountFixture
import io.github.kang30984.minisso.domain.fixture.FakePasswordHasher
import io.github.kang30984.minisso.domain.history.LoginFailureReason
import io.github.kang30984.minisso.domain.history.LoginOutcome
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LoginServiceTest {

    private val hasher = FakePasswordHasher()
    private val policy = AccountPolicy()
    private val clock = MutableClock(AccountFixture.NOW)
    private val accounts = InMemoryAccountRepository()
    private val histories = InMemoryLoginHistoryRepository()
    private lateinit var service: LoginService

    @BeforeEach
    fun setUp() {
        accounts.save(AccountFixture.active(id = 1L, hasher = hasher))
        service = LoginService(accounts, accounts, histories, hasher, policy, clock)
    }

    @Test
    fun `로그인에 성공하면 인증 결과와 성공 이력을 남긴다`() {
        val result = service.login(command(password = AccountFixture.PASSWORD))

        assertThat(result).isEqualTo(
            AuthenticationResult.Authenticated(
                accountId = 1L,
                loginId = AccountFixture.LOGIN_ID,
                role = "USER",
                passwordChangeRequired = false,
            ),
        )
        assertThat(histories.histories.single().outcome).isEqualTo(LoginOutcome.SUCCESS)
    }

    @Test
    fun `연속 실패로 잠기면 이후에는 LOCKED로 거부되고 모든 시도가 이력에 남는다`() {
        repeat(policy.maxFailedAttempts) { service.login(command(password = "wrong")) }

        val result = service.login(command(password = AccountFixture.PASSWORD))

        assertThat(result).isEqualTo(AuthenticationResult.Failed(LoginFailureReason.LOCKED))
        assertThat(histories.histories).hasSize(policy.maxFailedAttempts + 1)
        assertThat(histories.histories.map { it.failureReason })
            .containsSequence(LoginFailureReason.BAD_CREDENTIALS, LoginFailureReason.LOCKED)
    }

    @Test
    fun `잠금 시간이 지나면 다시 로그인할 수 있다`() {
        repeat(policy.maxFailedAttempts) { service.login(command(password = "wrong")) }
        clock.advance(policy.lockDuration)

        val result = service.login(command(password = AccountFixture.PASSWORD))

        assertThat(result).isInstanceOf(AuthenticationResult.Authenticated::class.java)
    }

    @Test
    fun `존재하지 않는 계정도 해시 비교를 수행해 응답 시간 차이를 줄인다`() {
        val matchCountBefore = hasher.matchCount

        val result = service.login(command(loginId = "nobody", password = "whatever"))

        assertThat(result).isEqualTo(AuthenticationResult.Failed(LoginFailureReason.UNKNOWN_ACCOUNT))
        assertThat(hasher.matchCount).isEqualTo(matchCountBefore + 1)
        assertThat(histories.histories.single().accountId).isNull()
    }

    @Test
    fun `형식이 잘못된 로그인 ID는 예외 없이 UNKNOWN_ACCOUNT로 처리한다`() {
        val result = service.login(command(loginId = "' OR 1=1 --", password = "whatever"))

        assertThat(result).isEqualTo(AuthenticationResult.Failed(LoginFailureReason.UNKNOWN_ACCOUNT))
    }

    @Test
    fun `같은 IP의 시도가 한도를 넘으면 비밀번호를 확인하지 않고 거부한다`() {
        val limited = LoginService(
            accounts, accounts, histories, hasher, policy, clock,
            loginRateLimitPort = CountingRateLimiter(),
            throttlePolicy = io.github.kang30984.minisso.application.account.service.LoginThrottlePolicy(maxAttemptsPerIp = 2),
        )
        limited.login(command(password = "wrong"))
        limited.login(command(password = "wrong"))
        val matchCountBefore = hasher.matchCount
        val historyCountBefore = histories.histories.size

        val result = limited.login(command(password = AccountFixture.PASSWORD))

        assertThat(result).isEqualTo(AuthenticationResult.Failed(LoginFailureReason.RATE_LIMITED))
        assertThat(hasher.matchCount).isEqualTo(matchCountBefore)
        assertThat(histories.histories).hasSize(historyCountBefore)
    }

    private class CountingRateLimiter : io.github.kang30984.minisso.application.account.port.outbound.LoginRateLimitPort {
        private val counts = mutableMapOf<String, Int>()

        override fun tryAcquire(key: String, limit: Int, window: java.time.Duration): Boolean {
            val count = (counts[key] ?: 0) + 1
            counts[key] = count
            return count <= limit
        }
    }

    private fun command(loginId: String = AccountFixture.LOGIN_ID, password: String) = LoginCommand(
        loginId = loginId,
        password = password,
        clientId = "sample-app-a",
        ip = "10.0.0.1",
        userAgent = "JUnit",
    )
}
