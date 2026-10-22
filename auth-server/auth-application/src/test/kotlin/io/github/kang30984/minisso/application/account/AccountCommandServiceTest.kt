package io.github.kang30984.minisso.application.account

import io.github.kang30984.minisso.application.account.port.inbound.ChangePasswordCommand
import io.github.kang30984.minisso.application.account.port.inbound.RegisterAccountCommand
import io.github.kang30984.minisso.application.account.service.AccountCommandService
import io.github.kang30984.minisso.application.fake.InMemoryAccountRepository
import io.github.kang30984.minisso.application.fake.MutableClock
import io.github.kang30984.minisso.domain.account.AccountId
import io.github.kang30984.minisso.domain.account.AccountPolicy
import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode
import io.github.kang30984.minisso.domain.fixture.AccountFixture
import io.github.kang30984.minisso.domain.fixture.FakePasswordHasher
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class AccountCommandServiceTest {

    private val hasher = FakePasswordHasher()
    private val policy = AccountPolicy()
    private val clock = MutableClock(AccountFixture.NOW)
    private val accounts = InMemoryAccountRepository()
    private val service = AccountCommandService(accounts, accounts, hasher, policy, clock)

    @Test
    fun `계정을 등록하면 ID를 돌려준다`() {
        val id = service.register(registerCommand())

        assertThat(accounts.findById(AccountId(id))).isNotNull
    }

    @Test
    fun `이미 사용 중인 로그인 ID는 등록할 수 없다`() {
        service.register(registerCommand())

        assertThatThrownBy { service.register(registerCommand(email = "other@example.com")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID)
    }

    @Test
    fun `이미 사용 중인 이메일은 등록할 수 없다`() {
        service.register(registerCommand())

        assertThatThrownBy { service.register(registerCommand(loginId = "other.user")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_EMAIL)
    }

    @Test
    fun `없는 계정의 잠금 해제는 ACCOUNT_NOT_FOUND`() {
        assertThatThrownBy { service.unlock(999L) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND)
    }

    @Test
    fun `비밀번호를 변경하면 새 비밀번호로 인증된다`() {
        val id = service.register(registerCommand())

        service.changePassword(ChangePasswordCommand(id, AccountFixture.PASSWORD, "Changed-Passw0rd!"))

        val account = checkNotNull(accounts.findById(AccountId(id)))
        assertThat(hasher.matches("Changed-Passw0rd!", account.passwordHash)).isTrue()
    }

    @Test
    fun `최초 관리자는 한 번만 생성되고 ADMIN 권한을 가진다`() {
        assertThat(service.ensureAdmin(registerCommand(loginId = "root.admin", email = "root@example.com"))).isTrue()
        assertThat(service.ensureAdmin(registerCommand(loginId = "root.admin", email = "root@example.com"))).isFalse()

        val admin = checkNotNull(accounts.findByLoginId(io.github.kang30984.minisso.domain.account.LoginId("root.admin")))
        assertThat(admin.role).isEqualTo(io.github.kang30984.minisso.domain.account.AccountRole.ADMIN)
    }

    @Test
    fun `커맨드의 toString에는 비밀번호가 노출되지 않는다`() {
        assertThat(registerCommand().toString()).doesNotContain(AccountFixture.PASSWORD)
    }

    private fun registerCommand(
        loginId: String = "kim.cs",
        email: String = "kim.cs@example.com",
    ) = RegisterAccountCommand(
        loginId = loginId,
        email = email,
        name = "김철수",
        department = "보안팀",
        initialPassword = AccountFixture.PASSWORD,
    )
}
