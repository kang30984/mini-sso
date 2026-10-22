package io.github.kang30984.minisso.domain.account

import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class AccountPolicyTest {

    private val policy = AccountPolicy()
    private val loginId = LoginId("hong.gildong")

    @ParameterizedTest
    @ValueSource(strings = ["Sso-Passw0rd!", "a1!a1!a1!a1!", "비밀번호는Strong1!"])
    fun `정책을 만족하는 비밀번호`(password: String) {
        assertThatCode { policy.validatePassword(password, loginId) }.doesNotThrowAnyException()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "Short1!",                 // 10자 미만
            "NoDigitsHere!!",          // 숫자 없음
            "NoSpecial1234",           // 특수문자 없음
            "1234567890!!",            // 문자 없음
            "With Space1!",            // 공백 포함
            "hong.gildong1!",          // 로그인 ID 포함
            "HONG.GILDONG1!",          // 대소문자 무시하고 로그인 ID 포함
        ],
    )
    fun `정책을 위반하는 비밀번호`(password: String) {
        assertThatThrownBy { policy.validatePassword(password, loginId) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.PASSWORD_POLICY_VIOLATION)
    }
}
