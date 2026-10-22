package io.github.kang30984.minisso.application.account.port.inbound

interface RegisterAccountUseCase {
    /** @return 생성된 계정 ID */
    fun register(command: RegisterAccountCommand): Long
}

data class RegisterAccountCommand(
    val loginId: String,
    val email: String,
    val name: String,
    val department: String,
    val initialPassword: String,
) {
    override fun toString(): String =
        "RegisterAccountCommand(loginId=$loginId, email=$email, name=$name, department=$department, initialPassword=****)"
}
