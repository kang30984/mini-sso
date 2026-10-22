package io.github.kang30984.minisso.domain.account

import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode
import java.time.Instant

/**
 * 계정 애그리거트 루트.
 * 잠금, 비밀번호 변경 같은 규칙은 서비스가 아니라 이 클래스의 메서드로만 상태를 바꾼다.
 */
class Account private constructor(
    val id: AccountId?,
    val loginId: LoginId,
    val role: AccountRole,
    email: Email,
    name: String,
    department: String,
    passwordHash: PasswordHash,
    status: AccountStatus,
    failedLoginCount: Int,
    lockedUntil: Instant?,
    passwordChangedAt: Instant,
    lastLoginAt: Instant?,
) {
    var email: Email = email
        private set
    var name: String = name
        private set
    var department: String = department
        private set
    var passwordHash: PasswordHash = passwordHash
        private set
    var status: AccountStatus = status
        private set
    var failedLoginCount: Int = failedLoginCount
        private set
    var lockedUntil: Instant? = lockedUntil
        private set
    var passwordChangedAt: Instant = passwordChangedAt
        private set
    var lastLoginAt: Instant? = lastLoginAt
        private set

    fun isLocked(now: Instant): Boolean = lockedUntil?.isAfter(now) ?: false

    fun authenticate(
        rawPassword: String,
        hasher: PasswordHasher,
        policy: AccountPolicy,
        now: Instant,
    ): LoginResult {
        when (status) {
            AccountStatus.WITHDRAWN -> return LoginResult.Rejected(RejectReason.WITHDRAWN)
            AccountStatus.DORMANT -> return LoginResult.Rejected(RejectReason.DORMANT)
            AccountStatus.ACTIVE -> Unit
        }

        // 잠긴 동안에는 비밀번호를 검증하지 않는다. 잠금 중에도 검증하면 무차별 대입을 계속 허용하는 셈이 된다.
        if (isLocked(now)) return LoginResult.Rejected(RejectReason.LOCKED)

        if (!hasher.matches(rawPassword, passwordHash)) return registerFailure(policy, now)

        failedLoginCount = 0
        lockedUntil = null
        lastLoginAt = now
        return if (policy.isPasswordExpired(passwordChangedAt, now)) {
            LoginResult.PasswordChangeRequired
        } else {
            LoginResult.Success
        }
    }

    private fun registerFailure(policy: AccountPolicy, now: Instant): LoginResult {
        failedLoginCount += 1
        if (failedLoginCount < policy.maxFailedAttempts) {
            return LoginResult.Rejected(RejectReason.BAD_CREDENTIALS)
        }
        // 잠금 시 카운트를 초기화해, 잠금이 풀린 뒤 한 번의 실수로 바로 다시 잠기지 않게 한다.
        failedLoginCount = 0
        lockedUntil = now.plus(policy.lockDuration)
        return LoginResult.Rejected(RejectReason.LOCKED)
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        hasher: PasswordHasher,
        policy: AccountPolicy,
        now: Instant,
    ) {
        ensureActive()
        if (!hasher.matches(currentPassword, passwordHash)) {
            throw BusinessException(ErrorCode.CURRENT_PASSWORD_MISMATCH)
        }
        policy.validatePassword(newPassword, loginId)
        if (hasher.matches(newPassword, passwordHash)) {
            throw BusinessException(ErrorCode.PASSWORD_REUSED)
        }
        passwordHash = hasher.hash(newPassword)
        passwordChangedAt = now
    }

    /** 관리자에 의한 잠금 해제 */
    fun unlock() {
        failedLoginCount = 0
        lockedUntil = null
    }

    private fun ensureActive() {
        if (status != AccountStatus.ACTIVE) throw BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE)
    }

    companion object {
        private const val MAX_NAME_LENGTH = 50
        private const val MAX_DEPARTMENT_LENGTH = 100

        fun register(
            loginId: LoginId,
            email: Email,
            name: String,
            department: String,
            rawPassword: String,
            hasher: PasswordHasher,
            policy: AccountPolicy,
            now: Instant,
            role: AccountRole = AccountRole.USER,
        ): Account {
            val trimmedName = name.trim()
            val trimmedDepartment = department.trim()
            if (trimmedName.isEmpty() || trimmedName.length > MAX_NAME_LENGTH ||
                trimmedDepartment.isEmpty() || trimmedDepartment.length > MAX_DEPARTMENT_LENGTH
            ) {
                throw BusinessException(ErrorCode.INVALID_PROFILE)
            }
            policy.validatePassword(rawPassword, loginId)

            return Account(
                id = null,
                loginId = loginId,
                role = role,
                email = email,
                name = trimmedName,
                department = trimmedDepartment,
                passwordHash = hasher.hash(rawPassword),
                status = AccountStatus.ACTIVE,
                failedLoginCount = 0,
                lockedUntil = null,
                passwordChangedAt = now,
                lastLoginAt = null,
            )
        }

        /** 저장소에서 읽은 값으로 복원. 검증 규칙을 다시 적용하지 않는다. */
        fun reconstitute(
            id: AccountId,
            loginId: LoginId,
            role: AccountRole,
            email: Email,
            name: String,
            department: String,
            passwordHash: PasswordHash,
            status: AccountStatus,
            failedLoginCount: Int,
            lockedUntil: Instant?,
            passwordChangedAt: Instant,
            lastLoginAt: Instant?,
        ): Account = Account(
            id, loginId, role, email, name, department, passwordHash, status,
            failedLoginCount, lockedUntil, passwordChangedAt, lastLoginAt,
        )
    }
}
