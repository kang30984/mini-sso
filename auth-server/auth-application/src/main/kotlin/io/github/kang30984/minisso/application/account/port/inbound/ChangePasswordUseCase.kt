package io.github.kang30984.minisso.application.account.port.inbound

interface ChangePasswordUseCase {
    fun changePassword(command: ChangePasswordCommand)
}

data class ChangePasswordCommand(
    val accountId: Long,
    val currentPassword: String,
    val newPassword: String,
) {
    override fun toString(): String = "ChangePasswordCommand(accountId=$accountId, currentPassword=****, newPassword=****)"
}
