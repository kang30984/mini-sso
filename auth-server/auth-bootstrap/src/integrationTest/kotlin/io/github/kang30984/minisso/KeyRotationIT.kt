package io.github.kang30984.minisso

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.kang30984.minisso.sdk.core.MiniSsoTokenVerifier
import io.github.kang30984.minisso.sdk.core.MiniSsoVerifierSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.Base64

/**
 * 서명 키 무중단 교체 시나리오 (ADR-008).
 * 인증 서버의 실제 JWKS 엔드포인트를 SDK(auth-sdk-core)로 조회해, 서비스 입장에서 검증이 끊기지 않는지 확인한다.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    // 키 교체 직후의 동작을 바로 확인하기 위해 인스턴스 캐시를 끈다 (운영 기본값 30초)
    properties = ["minisso.signing-key.cache-ttl=0s"],
)
@AutoConfigureMockMvc
@Import(TestContainersConfig::class)
class KeyRotationIT {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jwtEncoder: JwtEncoder

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `키를 교체해도 기존 토큰은 계속 검증되고 새 토큰은 새 키로 서명된다`() {
        val verifier = MiniSsoTokenVerifier.create(
            MiniSsoVerifierSettings.forIssuer(ISSUER, setOf(CLIENT_ID))
                .withJwkSetUri(URI.create("http://localhost:$port/oauth2/jwks"))
                // 모르는 kid가 오면 바로 재조회하도록 (운영 기본값 30초)
                .withJwksRefreshMinInterval(Duration.ZERO),
        )

        // 1. 교체 전 키로 발급한 토큰
        val before = issueToken()
        assertThat(verifier.verify(before).user().accountId()).isEqualTo("42")

        // 2. 관리자가 키를 교체
        val rotated = mockMvc.post("/api/admin/v1/signing-keys/rotate") {
            with(jwt().authorities(SimpleGrantedAuthority("ROLE_ADMIN")))
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        val newKid = objectMapper.readTree(rotated)["kid"].asText()

        // 3. 새 토큰은 새 키로 서명된다
        val after = issueToken()
        assertThat(kidOf(after)).isEqualTo(newKid).isNotEqualTo(kidOf(before))

        // 4. SDK는 캐시에 없는 새 kid를 보고 JWKS를 다시 받아 검증한다
        assertThat(verifier.verify(after).user().accountId()).isEqualTo("42")

        // 5. 교체 전 토큰도 계속 검증된다 (이전 키가 보존 기간 동안 게시됨)
        assertThat(verifier.verify(before).user().accountId()).isEqualTo("42")

        // 6. JWKS에는 두 키가 모두 있고, 개인키 정보는 없다
        mockMvc.get("/oauth2/jwks").andExpect {
            status { isOk() }
            jsonPath("$.keys.length()") { value(2) }
            jsonPath("$.keys[0].d") { doesNotExist() }
            jsonPath("$.keys[1].d") { doesNotExist() }
        }

        // 7. 관리 API는 상태만 보여주고 키 내용은 보여주지 않는다
        mockMvc.get("/api/admin/v1/signing-keys") {
            with(jwt().authorities(SimpleGrantedAuthority("ROLE_ADMIN")))
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].kid") { value(newKid) }
            jsonPath("$[0].status") { value("ACTIVE") }
            jsonPath("$[1].status") { value("RETIRING") }
            jsonPath("$[1].published") { value(true) }
            jsonPath("$[0].publicJwk") { doesNotExist() }
        }
    }

    /** 인가 서버가 토큰을 발급할 때 쓰는 것과 같은 JwtEncoder 빈으로 서명한다 */
    private fun issueToken(): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject("42")
            .audience(listOf(CLIENT_ID))
            .issuedAt(now)
            .expiresAt(now.plus(Duration.ofMinutes(10)))
            .claim("roles", listOf("USER"))
            .build()
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).tokenValue
    }

    private fun kidOf(token: String): String =
        objectMapper.readTree(Base64.getUrlDecoder().decode(token.substringBefore('.')))["kid"].asText()

    companion object {
        private const val ISSUER = "http://localhost:9000"
        private const val CLIENT_ID = "sample-app-a"
    }
}
