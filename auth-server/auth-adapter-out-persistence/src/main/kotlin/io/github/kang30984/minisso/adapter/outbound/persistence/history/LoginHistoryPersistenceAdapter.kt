package io.github.kang30984.minisso.adapter.outbound.persistence.history

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import io.github.kang30984.minisso.adapter.outbound.persistence.history.QLoginHistoryJpaEntity.loginHistoryJpaEntity
import io.github.kang30984.minisso.application.history.port.inbound.LoginHistorySearchCondition
import io.github.kang30984.minisso.application.history.port.outbound.SaveLoginHistoryPort
import io.github.kang30984.minisso.application.history.port.outbound.SearchLoginHistoryPort
import io.github.kang30984.minisso.domain.account.AccountId
import io.github.kang30984.minisso.domain.history.LoginHistory
import org.springframework.stereotype.Component

@Component
class LoginHistoryPersistenceAdapter(
    private val repository: LoginHistoryJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : SaveLoginHistoryPort, SearchLoginHistoryPort {

    override fun save(history: LoginHistory) {
        repository.save(
            LoginHistoryJpaEntity(
                accountId = history.accountId?.value,
                loginId = history.loginId,
                clientId = history.clientId,
                ip = history.ip,
                userAgent = history.userAgent,
                outcome = history.outcome,
                failureReason = history.failureReason,
                occurredAt = history.occurredAt,
            ),
        )
    }

    /**
     * 조건이 동적으로 조합되는 조회만 QueryDSL로 작성한다.
     * 정렬 기준은 서버에서 고정한다. 사용자 입력을 orderBy에 그대로 넘기지 않는다.
     */
    override fun search(condition: LoginHistorySearchCondition, cursor: Long?, limit: Int): List<LoginHistory> {
        val h = loginHistoryJpaEntity
        return queryFactory
            .selectFrom(h)
            .where(
                h.occurredAt.goe(condition.from),
                h.occurredAt.lt(condition.to),
                condition.outcome?.let { h.outcome.eq(it) },
                condition.clientId?.let { h.clientId.eq(it) },
                condition.ip?.let { h.ip.eq(it) },
                condition.loginIdPrefix?.let { h.loginId.startsWith(it) },
                cursorBefore(cursor),
            )
            .orderBy(h.id.desc())
            .limit(limit.toLong())
            .fetch()
            .map { it.toDomain() }
    }

    private fun cursorBefore(cursor: Long?): BooleanExpression? = cursor?.let { loginHistoryJpaEntity.id.lt(it) }

    private fun LoginHistoryJpaEntity.toDomain() = LoginHistory(
        id = id,
        accountId = accountId?.let(::AccountId),
        loginId = loginId,
        clientId = clientId,
        ip = ip,
        userAgent = userAgent,
        outcome = outcome,
        failureReason = failureReason,
        occurredAt = occurredAt,
    )
}
