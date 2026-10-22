package io.github.kang30984.minisso.domain.account

import io.github.kang30984.minisso.domain.common.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LoginIdTest {

    @ParameterizedTest
    @ValueSource(strings = ["hong", "hong.gildong", "user_01", "a-b-c-d"])
    fun `허용되는 로그인 ID`(value: String) {
        assertThat(LoginId(value).value).isEqualTo(value)
    }

    @ParameterizedTest
    @ValueSource(strings = ["abc", "Hong", "1hong", "hong gildong", "hong@corp", "' OR 1=1 --"])
    fun `허용되지 않는 로그인 ID`(value: String) {
        assertThatThrownBy { LoginId(value) }.isInstanceOf(BusinessException::class.java)
        assertThat(LoginId.parseOrNull(value)).isNull()
    }
}
