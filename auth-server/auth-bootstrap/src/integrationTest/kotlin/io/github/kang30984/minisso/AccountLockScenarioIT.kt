package io.github.kang30984.minisso

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.kang30984.minisso.application.account.port.inbound.AuthenticationResult
import io.github.kang30984.minisso.application.account.port.inbound.LoginCommand
import io.github.kang30984.minisso.application.account.port.inbound.LoginUseCase
import io.github.kang30984.minisso.domain.history.LoginFailureReason
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * 모든 계층을 실제로 연결한 시나리오 테스트.
 * 웹 → 애플리케이션 → 도메인 → JPA(MySQL) → Argon2 해시까지 한 흐름으로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig::class)
class AccountLockScenarioIT {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var loginUseCase: LoginUseCase

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `가입 후 5회 실패하면 잠기고, 관리자가 해제하면 다시 로그인된다`() {
        val accountId = register("scenario.user")

        repeat(4) { assertThat(login("scenario.user", "wrong")).isEqualTo(failed(LoginFailureReason.BAD_CREDENTIALS)) }
        assertThat(login("scenario.user", "wrong")).isEqualTo(failed(LoginFailureReason.LOCKED))
        assertThat(login("scenario.user", PASSWORD)).isEqualTo(failed(LoginFailureReason.LOCKED))

        mockMvc.get("/api/admin/v1/accounts/$accountId").andExpect {
            jsonPath("$.locked") { value(true) }
        }

        mockMvc.post("/api/admin/v1/accounts/$accountId/unlock").andExpect { status { isNoContent() } }

        assertThat(login("scenario.user", PASSWORD)).isInstanceOf(AuthenticationResult.Authenticated::class.java)
    }

    @Test
    fun `로그인 시도는 관리자 이력 API로 조회된다`() {
        register("history.user")
        login("history.user", "wrong")
        login("history.user", PASSWORD)

        val now = Instant.now()
        mockMvc.get("/api/admin/v1/login-histories") {
            param("from", now.minus(1, ChronoUnit.HOURS).toString())
            param("to", now.plus(1, ChronoUnit.HOURS).toString())
            param("loginIdPrefix", "history.user")
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(2) }
            jsonPath("$.items[0].outcome") { value("SUCCESS") }
            jsonPath("$.items[1].failureReason") { value("BAD_CREDENTIALS") }
        }
    }

    private fun register(loginId: String): Long {
        val response = mockMvc.post("/api/admin/v1/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "loginId" to loginId,
                    "email" to "$loginId@example.com",
                    "name" to "시나리오",
                    "department" to "플랫폼개발팀",
                    "initialPassword" to PASSWORD,
                ),
            )
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString

        return objectMapper.readTree(response)["id"].asLong()
    }

    private fun login(loginId: String, password: String) =
        loginUseCase.login(LoginCommand(loginId, password, "sample-app-a", "127.0.0.1", "IT"))

    private fun failed(reason: LoginFailureReason) = AuthenticationResult.Failed(reason)

    companion object {
        private const val PASSWORD = "Sso-Passw0rd!"
    }
}
