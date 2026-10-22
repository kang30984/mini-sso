package io.github.kang30984.minisso.adapter.outbound.persistence.account

import io.github.kang30984.minisso.application.account.port.outbound.LoadAccountPort
import io.github.kang30984.minisso.application.account.port.outbound.SaveAccountPort
import io.github.kang30984.minisso.domain.account.Account
import io.github.kang30984.minisso.domain.account.AccountId
import io.github.kang30984.minisso.domain.account.Email
import io.github.kang30984.minisso.domain.account.LoginId
import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class AccountPersistenceAdapter(
    private val repository: AccountJpaRepository,
) : LoadAccountPort, SaveAccountPort {

    override fun findById(id: AccountId): Account? =
        repository.findById(id.value).map { it.toDomain() }.orElse(null)

    override fun findByLoginId(loginId: LoginId): Account? =
        repository.findByLoginId(loginId.value)?.toDomain()

    override fun existsByLoginId(loginId: LoginId): Boolean = repository.existsByLoginId(loginId.value)

    override fun existsByEmail(email: Email): Boolean = repository.existsByEmail(email.value)

    override fun save(account: Account): Account {
        val id = account.id ?: return insert(account)

        // 같은 트랜잭션에서 이미 조회한 엔티티는 1차 캐시에서 꺼내므로 추가 쿼리가 없다.
        // 조회 시점의 version으로 갱신하므로 동시 수정은 낙관적 락 예외로 감지된다.
        val entity = repository.findById(id.value)
            .orElseThrow { BusinessException(ErrorCode.ACCOUNT_NOT_FOUND) }
        entity.updateFrom(account)
        return entity.toDomain()
    }

    private fun insert(account: Account): Account = try {
        repository.saveAndFlush(account.toNewEntity()).toDomain()
    } catch (e: DataIntegrityViolationException) {
        // 중복 검사와 저장 사이에 다른 요청이 같은 값으로 가입한 경우
        throw BusinessException(ErrorCode.DUPLICATE_ACCOUNT)
    }
}
