package io.github.kang30984.minisso.domain.account

import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode
import io.github.kang30984.minisso.domain.fixture.AccountFixture
import io.github.kang30984.minisso.domain.fixture.AccountFixture.NOW
import io.github.kang30984.minisso.domain.fixture.AccountFixture.PASSWORD
import io.github.kang30984.minisso.domain.fixture.FakePasswordHasher
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration

class AccountTest {

    private val hasher = FakePasswordHasher()
    private val policy = AccountPolicy()

    @Nested
    inner class 로그인 {

        @Test
        fun `비밀번호가 맞으면 성공하고 실패 횟수가 초기화된다`() {
            val account = AccountFixture.active(hasher = hasher)
            account.authenticate("wrong-password", hasher, policy, NOW)

            val result = account.authenticate(PASSWORD, hasher, policy, NOW)

            assertThat(result).isEqualTo(LoginResult.Success)
            assertThat(account.failedLoginCount).isZero()
            assertThat(account.lastLoginAt).isEqualTo(NOW)
        }

        @Test
        fun `최대 실패 횟수에 도달하면 잠기고 카운트는 초기화된다`() {
            val account = AccountFixture.active(hasher = hasher)

            repeat(policy.maxFailedAttempts - 1) {
                assertThat(account.authenticate("wrong", hasher, policy, NOW))
                    .isEqualTo(LoginResult.Rejected(RejectReason.BAD_CREDENTIALS))
            }
            val result = account.authenticate("wrong", hasher, policy, NOW)

            assertThat(result).isEqualTo(LoginResult.Rejected(RejectReason.LOCKED))
            assertThat(account.lockedUntil).isEqualTo(NOW.plus(policy.lockDuration))
            assertThat(account.failedLoginCount).isZero()
        }

        @Test
        fun `잠긴 동안에는 올바른 비밀번호도 거부하고 비밀번호 비교 자체를 하지 않는다`() {
            val account = lockedAccount()
            val matchCountBefore = hasher.matchCount

            val result = account.authenticate(PASSWORD, hasher, policy, NOW.plusSeconds(60))

            assertThat(result).isEqualTo(LoginResult.Rejected(RejectReason.LOCKED))
            assertThat(hasher.matchCount).isEqualTo(matchCountBefore)
        }

        @Test
        fun `잠금 시간이 지나면 다시 로그인할 수 있다`() {
            val account = lockedAccount()

            val result = account.authenticate(PASSWORD, hasher, policy, NOW.plus(policy.lockDuration))

            assertThat(result).isEqualTo(LoginResult.Success)
            assertThat(account.lockedUntil).isNull()
        }

        @Test
        fun `비밀번호 유효기간이 지나면 변경이 필요하다는 결과를 돌려준다`() {
            val account = AccountFixture.active(hasher = hasher, passwordChangedAt = NOW.minus(policy.passwordMaxAge))

            assertThat(account.authenticate(PASSWORD, hasher, policy, NOW))
                .isEqualTo(LoginResult.PasswordChangeRequired)
        }

        @Test
        fun `휴면 또는 탈퇴 계정은 비밀번호와 무관하게 거부한다`() {
            val dormant = AccountFixture.active(hasher = hasher, status = AccountStatus.DORMANT)
            val withdrawn = AccountFixture.active(hasher = hasher, status = AccountStatus.WITHDRAWN)

            assertThat(dormant.authenticate(PASSWORD, hasher, policy, NOW))
                .isEqualTo(LoginResult.Rejected(RejectReason.DORMANT))
            assertThat(withdrawn.authenticate(PASSWORD, hasher, policy, NOW))
                .isEqualTo(LoginResult.Rejected(RejectReason.WITHDRAWN))
        }

        @Test
        fun `관리자가 잠금을 해제하면 즉시 로그인할 수 있다`() {
            val account = lockedAccount()

            account.unlock()

            assertThat(account.isLocked(NOW)).isFalse()
            assertThat(account.authenticate(PASSWORD, hasher, policy, NOW)).isEqualTo(LoginResult.Success)
        }

        private fun lockedAccount(): Account {
            val account = AccountFixture.active(hasher = hasher)
            repeat(policy.maxFailedAttempts) { account.authenticate("wrong", hasher, policy, NOW) }
            assertThat(account.isLocked(NOW)).isTrue()
            return account
        }
    }

    @Nested
    inner class 비밀번호_변경 {

        @Test
        fun `현재 비밀번호가 맞고 정책을 만족하면 변경된다`() {
            val account = AccountFixture.active(hasher = hasher)
            val later = NOW.plus(Duration.ofDays(1))

            account.changePassword(PASSWORD, "New-Passw0rd!", hasher, policy, later)

            assertThat(hasher.matches("New-Passw0rd!", account.passwordHash)).isTrue()
            assertThat(account.passwordChangedAt).isEqualTo(later)
        }

        @Test
        fun `현재 비밀번호가 틀리면 변경할 수 없다`() {
            val account = AccountFixture.active(hasher = hasher)

            assertThatThrownBy { account.changePassword("wrong", "New-Passw0rd!", hasher, policy, NOW) }
                .isInstanceOfSatisfying(BusinessException::class.java) {
                    assertThat(it.errorCode).isEqualTo(ErrorCode.CURRENT_PASSWORD_MISMATCH)
                }
        }

        @Test
        fun `현재와 같은 비밀번호로는 변경할 수 없다`() {
            val account = AccountFixture.active(hasher = hasher)

            assertThatThrownBy { account.changePassword(PASSWORD, PASSWORD, hasher, policy, NOW) }
                .isInstanceOfSatisfying(BusinessException::class.java) {
                    assertThat(it.errorCode).isEqualTo(ErrorCode.PASSWORD_REUSED)
                }
        }
    }

    @Nested
    inner class 등록 {

        @Test
        fun `등록된 계정은 활성 상태이고 비밀번호는 해시로 저장된다`() {
            val account = Account.register(
                LoginId("kim.cs"), Email("kim.cs@example.com"), " 김철수 ", "보안팀",
                PASSWORD, hasher, policy, NOW,
            )

            assertThat(account.id).isNull()
            assertThat(account.status).isEqualTo(AccountStatus.ACTIVE)
            assertThat(account.name).isEqualTo("김철수")
            assertThat(account.passwordHash.value).isNotEqualTo(PASSWORD)
        }

        @Test
        fun `이름이 비어 있으면 등록할 수 없다`() {
            assertThatThrownBy {
                Account.register(LoginId("kim.cs"), Email("kim.cs@example.com"), "  ", "보안팀", PASSWORD, hasher, policy, NOW)
            }.isInstanceOfSatisfying(BusinessException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.INVALID_PROFILE)
            }
        }
    }
}
