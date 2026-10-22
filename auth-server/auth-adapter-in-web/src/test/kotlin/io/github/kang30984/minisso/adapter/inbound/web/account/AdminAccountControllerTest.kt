package io.github.kang30984.minisso.adapter.inbound.web.account

import io.github.kang30984.minisso.application.account.port.inbound.AccountView
import io.github.kang30984.minisso.application.account.port.inbound.GetAccountQuery
import io.github.kang30984.minisso.application.account.port.inbound.RegisterAccountCommand
import io.github.kang30984.minisso.application.account.port.inbound.RegisterAccountUseCase
import io.github.kang30984.minisso.application.account.port.inbound.UnlockAccountUseCase
import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant

@WebMvcTest(AdminAccountController::class)
@Import(AdminAccountControllerTest.FakeUseCases::class)
class AdminAccountControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `계정을 생성하면 201과 Location 헤더를 돌려준다`() {
        mockMvc.post("/api/admin/v1/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"loginId":"kim.cs","email":"kim.cs@example.com","name":"김철수",
                 "department":"보안팀","initialPassword":"Sso-Passw0rd!"}
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            header { string("Location", "http://localhost/api/admin/v1/accounts/1") }
            jsonPath("$.id") { value(1) }
        }
    }

    @Test
    fun `필수값이 없으면 400과 필드별 오류를 ProblemDetail로 돌려준다`() {
        mockMvc.post("/api/admin/v1/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"loginId":"kim.cs"}"""
        }.andExpect {
            status { isBadRequest() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.code") { value("COMMON-001") }
            jsonPath("$.errors.length()") { value(4) }
        }
    }

    @Test
    fun `도메인 예외는 에러 코드에 맞는 상태로 변환한다`() {
        mockMvc.post("/api/admin/v1/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"loginId":"dup.user","email":"dup@example.com","name":"중복",
                 "department":"보안팀","initialPassword":"Sso-Passw0rd!"}
            """.trimIndent()
        }.andExpect {
            status { isConflict() }
            jsonPath("$.code") { value(ErrorCode.DUPLICATE_LOGIN_ID.code) }
        }
    }

    @Test
    fun `없는 계정을 조회하면 404`() {
        mockMvc.get("/api/admin/v1/accounts/999").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value(ErrorCode.ACCOUNT_NOT_FOUND.code) }
        }
    }

    @Test
    fun `잠금 해제는 204`() {
        mockMvc.post("/api/admin/v1/accounts/1/unlock").andExpect {
            status { isNoContent() }
        }
    }

    @TestConfiguration
    class FakeUseCases {
        @Bean
        fun registerAccountUseCase() = object : RegisterAccountUseCase {
            override fun register(command: RegisterAccountCommand): Long =
                if (command.loginId == "dup.user") throw BusinessException(ErrorCode.DUPLICATE_LOGIN_ID) else 1L
        }

        @Bean
        fun unlockAccountUseCase() = object : UnlockAccountUseCase {
            override fun unlock(accountId: Long) = Unit
        }

        @Bean
        fun getAccountQuery() = object : GetAccountQuery {
            override fun getAccount(accountId: Long): AccountView {
                if (accountId != 1L) throw BusinessException(ErrorCode.ACCOUNT_NOT_FOUND)
                return AccountView(
                    1L, "kim.cs", "USER", "kim.cs@example.com", "김철수", "보안팀", "ACTIVE",
                    false, null, null, Instant.parse("2026-10-01T00:00:00Z"),
                )
            }
        }
    }
}
