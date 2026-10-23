package io.github.kang30984.minisso

import io.github.kang30984.minisso.adapter.inbound.oauth2.authentication.MiniSsoPrincipal
import io.github.kang30984.minisso.application.account.port.inbound.RegisterAccountCommand
import io.github.kang30984.minisso.application.account.port.inbound.RegisterAccountUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * 부하 테스트에서 발견한 문제의 회귀 테스트 (ADR-011, docs/troubleshooting.md 8번).
 * 같은 계정으로 동시에 로그인해도 500(낙관적 락 예외)이 나지 않아야 한다.
 */
@SpringBootTest
@Import(TestContainersConfig::class)
class ConcurrentLoginIT {

    @Autowired
    lateinit var authenticationProvider: AuthenticationProvider

    @Autowired
    lateinit var registerAccountUseCase: RegisterAccountUseCase

    @Test
    fun `같은 계정으로 동시에 로그인해도 모두 성공한다`() {
        register("concurrent.ok")

        val results = runConcurrently(THREADS) { login("concurrent.ok", PASSWORD) }

        assertThat(results).describedAs("결과: %s", results).allMatch { it is MiniSsoPrincipal }
    }

    @Test
    fun `같은 계정으로 동시에 틀린 비밀번호를 보내도 인증 실패로만 응답한다`() {
        register("concurrent.bad")

        val results = runConcurrently(THREADS) { login("concurrent.bad", "wrong-password") }

        assertThat(results).describedAs("결과: %s", results).allMatch { it is AuthenticationException }
    }

    private fun login(loginId: String, password: String): Any = try {
        authenticationProvider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(loginId, password)).principal
    } catch (e: AuthenticationException) {
        e
    }

    /** 모든 스레드가 준비된 뒤 동시에 출발시킨다. 예상하지 못한 예외는 결과로 돌려 테스트가 실패하게 한다. */
    private fun runConcurrently(threads: Int, task: () -> Any): List<Any> {
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(threads)
        try {
            val futures = (1..threads).map {
                executor.submit(Callable { start.await(); runCatching(task).getOrElse { e -> e } })
            }
            start.countDown()
            return futures.map { it.get() }
        } finally {
            executor.shutdown()
        }
    }

    private fun register(loginId: String) {
        registerAccountUseCase.register(
            RegisterAccountCommand(loginId, "$loginId@example.com", "동시성", "플랫폼개발팀", PASSWORD),
        )
    }

    companion object {
        private const val PASSWORD = "Sso-Passw0rd!"
        private const val THREADS = 12
    }
}
