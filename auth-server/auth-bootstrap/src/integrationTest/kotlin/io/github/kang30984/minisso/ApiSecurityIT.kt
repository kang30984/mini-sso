package io.github.kang30984.minisso

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig::class)
class ApiSecurityIT {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `토큰 없이 관리자 API를 호출하면 401`() {
        mockMvc.get("/api/admin/v1/accounts/1").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `USER 권한 토큰으로 관리자 API를 호출하면 403`() {
        mockMvc.get("/api/admin/v1/accounts/1") {
            with(jwt().authorities(SimpleGrantedAuthority("ROLE_USER")))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `헬스 체크는 인증 없이 호출할 수 있다`() {
        mockMvc.get("/actuator/health").andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `OIDC Discovery 문서를 공개한다`() {
        mockMvc.get("/.well-known/openid-configuration").andExpect {
            status { isOk() }
            jsonPath("$.authorization_endpoint") { exists() }
            jsonPath("$.jwks_uri") { exists() }
        }
    }

    @Test
    fun `인가 서버 메타데이터에 PKCE S256 지원을 명시한다`() {
        mockMvc.get("/.well-known/oauth-authorization-server").andExpect {
            status { isOk() }
            jsonPath("$.code_challenge_methods_supported[0]") { value("S256") }
        }
    }
}
