package io.github.kang30984.minisso.adapter.outbound.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * 계정 비밀번호와 OAuth 클라이언트 시크릿에 같은 인코더를 쓴다.
 * Spring Authorization Server는 이 빈으로 클라이언트 시크릿을 검증한다.
 * 빈이 없으면 bcrypt 기본 인코더가 쓰여, argon2 해시를 "업그레이드 대상"으로 판단하고 클라이언트를 다시 저장하려 한다.
 */
@Configuration(proxyBeanMethods = false)
class PasswordEncoderConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = create()

    companion object {
        private const val ARGON2_ID = "argon2"

        fun create(): PasswordEncoder = DelegatingPasswordEncoder(
            ARGON2_ID,
            mapOf<String, PasswordEncoder>(ARGON2_ID to Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()),
        ).apply {
            // 접두어가 없거나 알 수 없는 형식의 해시는 예외(500) 대신 불일치로 처리한다
            setDefaultPasswordEncoderForMatches(RejectAllPasswordEncoder)
        }
    }

    private object RejectAllPasswordEncoder : PasswordEncoder {
        override fun encode(rawPassword: CharSequence?): String =
            throw UnsupportedOperationException("인코딩에는 사용하지 않습니다.")

        override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean = false
    }
}
