package io.github.kang30984.minisso

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.kang30984.minisso.application.account.port.inbound.RegisterAccountCommand
import io.github.kang30984.minisso.application.account.port.inbound.RegisterAccountUseCase
import io.github.kang30984.minisso.application.client.port.inbound.RegisterOAuthClientCommand
import io.github.kang30984.minisso.application.client.port.inbound.RegisterOAuthClientUseCase
import io.github.kang30984.minisso.application.session.port.outbound.BackChannelLogoutNotice
import io.github.kang30984.minisso.application.session.port.outbound.BackChannelLogoutPort
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * 브라우저가 하는 일을 MockMvc로 그대로 재현한 OIDC 흐름 테스트.
 * 로그인 세션은 Spring Session(Redis)에 저장되므로, 실제 브라우저처럼 SESSION 쿠키를 주고받는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig::class, AuthorizationCodeFlowIT.RecordingBackChannelConfig::class)
class AuthorizationCodeFlowIT {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var registerAccountUseCase: RegisterAccountUseCase

    @Autowired
    lateinit var registerOAuthClientUseCase: RegisterOAuthClientUseCase

    @Autowired
    lateinit var backChannel: RecordingBackChannelLogoutPort

    private lateinit var loginId: String
    private var accountId: Long = 0

    @BeforeEach
    fun setUp() {
        registerOAuthClientUseCase.registerIfAbsent(
            RegisterOAuthClientCommand(
                clientId = CLIENT_ID,
                clientName = "Flow Test App",
                clientSecret = CLIENT_SECRET,
                redirectUris = setOf(REDIRECT_URI),
                postLogoutRedirectUris = setOf(POST_LOGOUT_REDIRECT_URI),
                scopes = setOf("openid", "profile", "email"),
                backchannelLogoutUri = BACK_CHANNEL_LOGOUT_URI,
            ),
        )
        loginId = "flow.user${SEQUENCE.incrementAndGet()}"
        accountId = registerAccountUseCase.register(
            RegisterAccountCommand(loginId, "$loginId@example.com", "흐름테스트", "플랫폼개발팀", PASSWORD),
        )
        backChannel.notices.clear()
    }

    @Test
    fun `인가 코드와 PKCE로 ID 토큰과 액세스 토큰을 발급받는다`() {
        val browser = Browser()
        val verifier = randomVerifier()

        // 1. 로그인하지 않은 상태의 인가 요청은 로그인 페이지로 이동한다
        val first = browser.get(authorizeUri(challenge = s256(verifier)))
        assertThat(first.status).isEqualTo(302)
        assertThat(first.redirectedUrl).endsWith("/login")

        // 2. 로그인
        assertThat(browser.login(loginId, PASSWORD).redirectedUrl).doesNotContain("error")

        // 3. 같은 브라우저(세션 쿠키)로 다시 인가 요청하면 인가 코드와 함께 클라이언트로 리다이렉트된다
        val code = browser.authorize(verifier)

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
        assertThat(idToken.has("sid")).describedAs("로그아웃 시 세션을 식별할 sid").isTrue()

        val accessToken = decodePayload(tokens["access_token"].asText())
        assertThat(firstValue(accessToken["roles"])).isEqualTo("USER")
        assertThat(accessToken.has("email")).describedAs("액세스 토큰에는 개인정보를 넣지 않는다").isFalse()
    }

    @Test
    fun `로그인 세션이 유지되는 동안에는 다시 로그인하지 않고 코드를 받는다 (SSO)`() {
        val browser = Browser()
        browser.login(loginId, PASSWORD)
        val firstVerifier = randomVerifier()
        exchange(browser.authorize(firstVerifier), firstVerifier)

        // 같은 브라우저로 두 번째 인가 요청: 로그인 페이지 없이 바로 코드 발급
        val secondVerifier = randomVerifier()
        val code = browser.authorize(secondVerifier)
        assertThat(exchange(code, secondVerifier).has("id_token")).isTrue()
    }

    @Test
    fun `code_verifier가 다르면 토큰을 발급하지 않는다`() {
        val browser = Browser()
        browser.login(loginId, PASSWORD)
        val code = browser.authorize(randomVerifier())

        val response = tokenRequest(
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to REDIRECT_URI,
            "code_verifier" to randomVerifier(),
        )

        assertThat(response.status).isEqualTo(400)
        assertThat(objectMapper.readTree(response.contentAsString)["error"].asText()).isEqualTo("invalid_grant")
    }

    @Test
    fun `리프레시 토큰은 사용할 때마다 새로 발급된다`() {
        val tokens = loginAndIssueTokens(Browser())
        val firstRefreshToken = tokens["refresh_token"].asText()

        val refreshed = refresh(firstRefreshToken)

        assertThat(refreshed.status).isEqualTo(200)
        val secondRefreshToken = objectMapper.readTree(refreshed.contentAsString)["refresh_token"].asText()
        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken)
    }

    @Test
    fun `이미 회전된 리프레시 토큰을 재사용하면 최신 토큰까지 함께 폐기된다`() {
        val tokens = loginAndIssueTokens(Browser())
        val stolenRefreshToken = tokens["refresh_token"].asText()
        val latestRefreshToken = objectMapper.readTree(refresh(stolenRefreshToken).contentAsString)["refresh_token"].asText()

        // 공격자가 탈취해 둔 이전 토큰으로 갱신 시도
        val reuse = refresh(stolenRefreshToken)
        assertThat(reuse.status).isEqualTo(400)

        // 누가 정상 사용자인지 알 수 없으므로 정상 클라이언트의 최신 토큰도 무효가 된다 (ADR-007)
        val afterReuse = refresh(latestRefreshToken)
        assertThat(afterReuse.status).isEqualTo(400)
        assertThat(objectMapper.readTree(afterReuse.contentAsString)["error"].asText()).isEqualTo("invalid_grant")
    }

    @Test
    fun `로그아웃하면 세션과 토큰이 무효화되고 참여 서비스에 Back-Channel Logout을 보낸다`() {
        val browser = Browser()
        val tokens = loginAndIssueTokens(browser)
        val idToken = tokens["id_token"].asText()
        val sid = decodePayload(idToken)["sid"].asText()

        val logout = browser.get(
            UriComponentsBuilder.fromPath("/connect/logout")
                .queryParam("id_token_hint", idToken)
                .queryParam("post_logout_redirect_uri", POST_LOGOUT_REDIRECT_URI)
                .queryParam("state", "logout-state")
                .encode()
                .build()
                .toUri(),
        )

        // 등록된 주소로 state와 함께 돌아간다
        assertThat(logout.status).isEqualTo(302)
        assertThat(logout.redirectedUrl).startsWith(POST_LOGOUT_REDIRECT_URI).contains("state=logout-state")

        // 해당 세션으로 발급된 리프레시 토큰은 더 이상 쓸 수 없다
        assertThat(refresh(tokens["refresh_token"].asText()).status).isEqualTo(400)

        // 참여한 서비스에 Logout Token 전송을 요청했다
        assertThat(backChannel.notices).hasSize(1)
        val notice = backChannel.notices.single()
        assertThat(notice.clientId).isEqualTo(CLIENT_ID)
        assertThat(notice.logoutUri).isEqualTo(BACK_CHANNEL_LOGOUT_URI)
        assertThat(notice.sessionId).isEqualTo(sid)
        assertThat(notice.subject).isEqualTo(accountId.toString())

        // 같은 브라우저로 다시 인가 요청하면 로그인 페이지로 간다 (SSO 세션 종료)
        val again = browser.get(authorizeUri(challenge = s256(randomVerifier())))
        assertThat(again.redirectedUrl).endsWith("/login")
    }

    @Test
    fun `PKCE 없이 인가 요청하면 코드를 발급하지 않는다`() {
        val browser = Browser()
        browser.login(loginId, PASSWORD)

        val response = browser.get(authorizeUri(challenge = null))
        val location = response.redirectedUrl.orEmpty()

        // redirect_uri는 유효하므로 클라이언트로 error=invalid_request를 돌려보내고, 코드는 발급하지 않는다
        assertThat(response.status).isEqualTo(302)
        assertThat(location).startsWith(REDIRECT_URI)
        assertThat(location).contains("error=invalid_request")
        assertThat(location).doesNotContain("code=")
    }

    @Test
    fun `등록되지 않은 redirect_uri로는 리다이렉트하지 않는다`() {
        val browser = Browser()
        browser.login(loginId, PASSWORD)

        val response = browser.get(authorizeUri(challenge = s256(randomVerifier()), redirectUri = "http://127.0.0.1:9999/evil"))

        assertThat(response.status).isEqualTo(400)
    }

    @Test
    fun `비밀번호가 틀리면 로그인 페이지로 돌아간다`() {
        val response = Browser().login(loginId, "wrong-password")

        assertThat(response.status).isEqualTo(302)
        assertThat(response.redirectedUrl).isEqualTo("/login?error")
    }

    // ---------------------------------------------------------------- helpers

    /** SESSION 쿠키를 저장하고 다음 요청에 실어 보내는 최소한의 브라우저 */
    private inner class Browser {
        private var sessionCookie: Cookie? = null

        fun get(uri: URI): MockHttpServletResponse = capture(
            mockMvc.get(uri) {
                accept = MediaType.TEXT_HTML
                sessionCookie?.let { cookie(it) }
            }.andReturn(),
        )

        fun login(loginId: String, password: String): MockHttpServletResponse = capture(
            mockMvc.post("/login") {
                param("username", loginId)
                param("password", password)
                with(csrf())
                sessionCookie?.let { cookie(it) }
            }.andReturn(),
        )

        fun authorize(verifier: String): String {
            val response = get(authorizeUri(challenge = s256(verifier)))
            val location = response.redirectedUrl.orEmpty()
            assertThat(location).describedAs("인가 응답 status=%s", response.status).startsWith(REDIRECT_URI)
            val query = UriComponentsBuilder.fromUriString(location).build().queryParams
            assertThat(query.getFirst("state")).isEqualTo(STATE)
            return checkNotNull(query.getFirst("code"))
        }

        private fun capture(result: MvcResult): MockHttpServletResponse {
            result.response.getCookie(SESSION_COOKIE)?.let { sessionCookie = if (it.maxAge == 0) null else it }
            return result.response
        }
    }

    private fun loginAndIssueTokens(browser: Browser): JsonNode {
        browser.login(loginId, PASSWORD)
        val verifier = randomVerifier()
        return exchange(browser.authorize(verifier), verifier)
    }

    private fun exchange(code: String, codeVerifier: String): JsonNode {
        val response = tokenRequest(
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to REDIRECT_URI,
            "code_verifier" to codeVerifier,
        )
        assertThat(response.status).describedAs(response.contentAsString).isEqualTo(200)
        return objectMapper.readTree(response.contentAsString)
    }

    private fun refresh(refreshToken: String): MockHttpServletResponse =
        tokenRequest("grant_type" to "refresh_token", "refresh_token" to refreshToken)

    private fun tokenRequest(vararg params: Pair<String, String>): MockHttpServletResponse =
        mockMvc.post("/oauth2/token") {
            with(httpBasic(CLIENT_ID, CLIENT_SECRET))
            params.forEach { (name, value) -> param(name, value) }
        }.andReturn().response

    /**
     * 인가 요청 파라미터는 반드시 쿼리 스트링에 담는다.
     * Spring Authorization Server는 GET 인가 요청에서 쿼리 스트링에 있는 파라미터만 읽는다.
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

    /** JWT 라이브러리는 값이 하나인 배열을 문자열로 직렬화하기도 하므로 두 형태를 모두 처리한다 */
    private fun firstValue(node: JsonNode): String = if (node.isArray) node[0].asText() else node.asText()

    private fun decodePayload(jwt: String): JsonNode =
        objectMapper.readTree(Base64.getUrlDecoder().decode(jwt.split(".")[1]))

    /** 실제 HTTP 전송 대신 Back-Channel Logout 요청을 기록한다. 전송 자체는 BackChannelLogoutNotifierTest에서 검증한다. */
    class RecordingBackChannelLogoutPort : BackChannelLogoutPort {
        val notices: MutableList<BackChannelLogoutNotice> = CopyOnWriteArrayList()

        override fun notify(notice: BackChannelLogoutNotice) {
            notices += notice
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    class RecordingBackChannelConfig {
        @Bean
        @Primary
        fun recordingBackChannelLogoutPort() = RecordingBackChannelLogoutPort()
    }

    companion object {
        private const val CLIENT_ID = "flow-test-app"
        private const val CLIENT_SECRET = "flow-test-app-secret-123"
        private const val REDIRECT_URI = "http://127.0.0.1:8081/login/oauth2/code/mini-sso"
        private const val POST_LOGOUT_REDIRECT_URI = "http://127.0.0.1:8081/logged-out"
        private const val BACK_CHANNEL_LOGOUT_URI = "http://127.0.0.1:8081/logout/connect/back-channel/mini-sso"
        private const val STATE = "state-123"
        private const val PASSWORD = "Sso-Passw0rd!"
        private const val SESSION_COOKIE = "SESSION"
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
