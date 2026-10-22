package io.github.kang30984.minisso.sample.b

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

/**
 * SDK 자동 구성이 붙은 실제 애플리케이션 컨텍스트로 검증한다.
 * 토큰 서명 검증 자체는 SDK core 테스트와 인증 서버의 KeyRotationIT에서 다룬다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `공개 경로는 토큰 없이 호출된다`() {
        mockMvc.get("/api/public/status").andExpect { status { isOk() } }
    }

    @Test
    fun `토큰이 없으면 SDK가 401 ProblemDetail로 응답한다`() {
        mockMvc.get("/api/me").andExpect {
            status { isUnauthorized() }
            header { string("WWW-Authenticate", "Bearer") }
            jsonPath("$.code") { value("MINISSO-401") }
        }
    }

    @Test
    fun `@CurrentUser로 토큰의 사용자 정보를 주입받는다`() {
        mockMvc.get("/api/me") {
            with(jwt().jwt { it.subject("42").claim("roles", listOf("USER")).audience(listOf("sample-app-a")) })
        }.andExpect {
            status { isOk() }
            jsonPath("$.accountId") { value("42") }
            jsonPath("$.roles[0]") { value("USER") }
            jsonPath("$.calledBy") { value("sample-app-a") }
        }
    }

    @Test
    fun `@RequiresScope의 scope가 없으면 403`() {
        mockMvc.get("/api/team-reports") {
            with(jwt().jwt { it.subject("42").claim("scope", listOf("openid")) })
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("MINISSO-403") }
        }
    }

    @Test
    fun `@RequiresScope의 scope가 있으면 호출된다`() {
        mockMvc.get("/api/team-reports") {
            with(jwt().jwt { it.subject("42").claim("scope", listOf("openid", "profile")) })
        }.andExpect {
            status { isOk() }
            jsonPath("$.requestedBy") { value("42") }
        }
    }
}
