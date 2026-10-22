package io.github.kang30984.minisso

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.kang30984.minisso.application.account.port.inbound.RegisterAccountCommand
import io.github.kang30984.minisso.application.account.port.inbound.RegisterAccountUseCase
import io.github.kang30984.minisso.application.client.port.inbound.RegisterOAuthClientCommand
import io.github.kang30984.minisso.application.client.port.inbound.RegisterOAuthClientUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger

/**
 * 브라우저가 하는 일을 MockMvc로 그대로 재현한 OIDC 인가 코드 + PKCE 흐름 테스트.
 * 인가 요청 → 로그인 페이지 → 로그인 → 인가 코드 → 토큰 교환 → ID/Access 토큰 클레임 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig::class)
class AuthorizationCodeFlowIT {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var registerAccountUseCase: RegisterAccountUseCase

    @Autowired
    lateinit var registerOAuthClientUseCase: RegisterOAuthClientUseCase

    private lateinit var loginId: String
    private var accountId: Long = 0
    private val verifier = randomVerifier()

    @BeforeEach
    fun setUp() {
        registerOAuthClientUseCase.registerIfAbsent(
            RegisterOAuthClientCommand(
                clientId = CLIENT_ID,
                clientName = "Flow Test App",
                clientSecret = CLIENT_SECRET,
                redirectUris = setOf(REDIRECT_URI),
                postLogoutRedirectUris = emptySet(),
                scopes = setOf("openid", "profile", "email"),
            ),
        )
        loginId = "flow.user${SEQUENCE.incrementAndGet()}"
        accountId = registerAccountUseCase.register(
            RegisterAccountCommand(loginId, "$loginId@example.com", "흐름테스트", "플랫폼개발팀", PASSWORD),
        )
    }

    @Test
    fun `인가 코드와 PKCE로 ID 토큰과 액세스 토큰을 발급받는다`() {
        val session = MockHttpSession()

        // 1. 로그인하지 않은 상태의 인가 요청은 로그인 페이지로 이동한다
        mockMvc.get(authorizeUri(challenge = s256(verifier))) {
            this.session = session
            accept = MediaType.TEXT_HTML
        }.andExpect {
            status { is3xxRedirection() }
            redirectedUrlPattern("**/login")
        }

        // 2. 로그인
        login(session, PASSWORD).andExpect {
            status { is3xxRedirection() }
        }

        // 3. 같은 세션으로 다시 인가 요청하면 인가 코드와 함께 클라이언트로 리다이렉트된다
        val code = authorize(session)

        // 4. 인가 코드 + code_verifier로 토큰 교환
        val tokens = exchange(code, verifier)
        assertThat(tokens["token_type"].asText()).isEqualTo("Bearer")
        assertThat(tokens.has("refresh_token")).isTrue()

        val idToken = decodePayload(tokens["id_token"].asText())
        assertThat(idToken["sub"].asText()).isEqualTo(accountId.toString())
        assertThat(firstValue(idToken["aud"])).isEqualTo(CLIENT_ID)
        assertThat(idToken["preferred_username"].asText()).isEqualTo(loginId)
        assertThat(idToken["email"].asText()).isEqualTo("$loginId@example.com")
        assertThat(idToken["department"].asText()).isEqualTo("플랫폼개발팀")

        val accessToken = decodePayload(tokens["access_token"].asText())
        assertThat(firstValue(accessToken["roles"])).isEqualTo("USER")
        assertThat(accessToken.has("email")).describedAs("액세스 토큰에는 개인정보를 넣지 않는다").isFalse()
    }

    @Test
    fun `code_verifier가 다르면 토큰을 발급하지 않는다`() {
        val session = MockHttpSession()
        login(session, PASSWORD)
        val code = authorize(session)

        mockMvc.post("/oauth2/token") {
            tokenRequest(code, randomVerifier())
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("invalid_grant") }
        }
    }

    @Test
    fun `PKCE 없이 인가 요청하면 코드를 발급하지 않는다`() {
        val session = MockHttpSession()
        login(session, PASSWORD)

        val response = mockMvc.get(authorizeUri(challenge = null)) {
            this.session = session
            accept = MediaType.TEXT_HTML
        }.andReturn().response
        val location = response.redirectedUrl.orEmpty()

        // redirect_uri는 유효하므로 클라이언트로 error=invalid_request를 돌려보내고, 코드는 발급하지 않는다
        assertThat(response.status).isEqualTo(302)
        assertThat(location).startsWith(REDIRECT_URI)
        assertThat(location).contains("error=invalid_request")
        assertThat(location).doesNotContain("code=")
    }

    @Test
    fun `등록되지 않은 redirect_uri로는 리다이렉트하지 않는다`() {
        val session = MockHttpSession()
        login(session, PASSWORD)

        mockMvc.get(authorizeUri(challenge = s256(verifier), redirectUri = "http://127.0.0.1:9999/evil")) {
            this.session = session
            accept = MediaType.TEXT_HTML
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `비밀번호가 틀리면 로그인 페이지로 돌아간다`() {
        login(MockHttpSession(), "wrong-password").andExpect {
            status { is3xxRedirection() }
            redirectedUrl("/login?error")
        }
    }

    private fun login(session: MockHttpSession, password: String) =
        mockMvc.post("/login") {
            this.session = session
            param("username", loginId)
            param("password", password)
            with(csrf())
        }

    private fun authorize(session: MockHttpSession): String {
        val location = mockMvc.get(authorizeUri(challenge = s256(verifier))) {
            this.session = session
            accept = MediaType.TEXT_HTML
        }.andExpect {
            status { is3xxRedirection() }
        }.andReturn().response.redirectedUrl.orEmpty()

        assertThat(location).startsWith(REDIRECT_URI)
        val query = UriComponentsBuilder.fromUriString(location).build().queryParams
        assertThat(query.getFirst("state")).isEqualTo(STATE)
        return checkNotNull(query.getFirst("code"))
    }

    private fun exchange(code: String, codeVerifier: String): JsonNode {
        val body = mockMvc.post("/oauth2/token") {
            tokenRequest(code, codeVerifier)
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsString
        return objectMapper.readTree(body)
    }

    /**
     * 인가 요청 파라미터는 반드시 쿼리 스트링에 담는다.
     * Spring Authorization Server는 GET 인가 요청에서 쿼리 스트링에 있는 파라미터만 읽기 때문에,
     * MockMvc의 param()으로 넣으면 파라미터가 없는 요청으로 처리되어 400이 된다.
     */
    private fun authorizeUri(challenge: String?, redirectUri: String = REDIRECT_URI): URI =
        UriComponentsBuilder.fromPath("/oauth2/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", CLIENT_ID)
            .queryParam("scope", "openid profile email")
            .queryParam("redirect_uri", redirectUri)
            .queryParam("state", STATE)
            .apply {
                if (challenge != null) {
                    queryParam("code_challenge", challenge)
                    queryParam("code_challenge_method", "S256")
                }
            }
            .encode()
            .build()
            .toUri()

    private fun MockHttpServletRequestDsl.tokenRequest(code: String, codeVerifier: String) {
        with(httpBasic(CLIENT_ID, CLIENT_SECRET))
        param("grant_type", "authorization_code")
        param("code", code)
        param("redirect_uri", REDIRECT_URI)
        param("code_verifier", codeVerifier)
    }

    /** JWT 라이브러리는 값이 하나인 배열을 문자열로 직렬화하기도 하므로 두 형태를 모두 처리한다 */
    private fun firstValue(node: JsonNode): String = if (node.isArray) node[0].asText() else node.asText()

    private fun decodePayload(jwt: String): JsonNode =
        objectMapper.readTree(Base64.getUrlDecoder().decode(jwt.split(".")[1]))

    companion object {
        private const val CLIENT_ID = "flow-test-app"
        private const val CLIENT_SECRET = "flow-test-app-secret-123"
        private const val REDIRECT_URI = "http://127.0.0.1:8081/login/oauth2/code/mini-sso"
        private const val STATE = "state-123"
        private const val PASSWORD = "Sso-Passw0rd!"
        private val SEQUENCE = AtomicInteger()

        private fun randomVerifier(): String {
            val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }

        private fun s256(verifier: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        }
    }
}
