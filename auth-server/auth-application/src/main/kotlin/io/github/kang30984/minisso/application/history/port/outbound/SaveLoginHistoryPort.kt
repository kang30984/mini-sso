package io.github.kang30984.minisso.application.history.port.outbound

import io.github.kang30984.minisso.domain.history.LoginHistory

interface SaveLoginHistoryPort {
    fun save(history: LoginHistory)
}
