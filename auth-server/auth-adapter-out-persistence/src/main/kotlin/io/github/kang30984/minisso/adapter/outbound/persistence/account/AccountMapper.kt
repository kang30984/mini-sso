package io.github.kang30984.minisso.adapter.outbound.persistence.account

import io.github.kang30984.minisso.domain.account.Account
import io.github.kang30984.minisso.domain.account.AccountId
import io.github.kang30984.minisso.domain.account.Email
import io.github.kang30984.minisso.domain.account.LoginId
import io.github.kang30984.minisso.domain.account.PasswordHash

internal fun AccountJpaEntity.toDomain(): Account = Account.reconstitute(
    id = AccountId(checkNotNull(id)),
    loginId = LoginId(loginId),
    role = role,
    email = Email(email),
    name = name,
    department = department,
    passwordHash = PasswordHash(passwordHash),
    status = status,
    failedLoginCount = failedLoginCount,
    lockedUntil = lockedUntil,
    passwordChangedAt = passwordChangedAt,
    lastLoginAt = lastLoginAt,
)

internal fun Account.toNewEntity(): AccountJpaEntity = AccountJpaEntity(
    loginId = loginId.value,
    role = role,
    email = email.value,
    name = name,
    department = department,
    passwordHash = passwordHash.value,
    status = status,
    failedLoginCount = failedLoginCount,
    lockedUntil = lockedUntil,
    passwordChangedAt = passwordChangedAt,
    lastLoginAt = lastLoginAt,
)

/** 관리 중인(managed) 엔티티에 도메인 상태를 반영한다. loginId와 role은 변경 불가 값이므로 반영하지 않는다. */
internal fun AccountJpaEntity.updateFrom(account: Account) {
    email = account.email.value
    name = account.name
    department = account.department
    passwordHash = account.passwordHash.value
    status = account.status
    failedLoginCount = account.failedLoginCount
    lockedUntil = account.lockedUntil
    passwordChangedAt = account.passwordChangedAt
    lastLoginAt = account.lastLoginAt
}
