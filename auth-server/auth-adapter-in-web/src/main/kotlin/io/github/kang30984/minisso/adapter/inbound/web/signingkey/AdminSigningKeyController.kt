package io.github.kang30984.minisso.adapter.inbound.web.signingkey

import io.github.kang30984.minisso.application.signingkey.port.inbound.ListSigningKeysQuery
import io.github.kang30984.minisso.application.signingkey.port.inbound.RotateSigningKeyUseCase
import io.github.kang30984.minisso.application.signingkey.port.inbound.SigningKeyView
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/v1/signing-keys")
class AdminSigningKeyController(
    private val rotateSigningKeyUseCase: RotateSigningKeyUseCase,
    private val listSigningKeysQuery: ListSigningKeysQuery,
) {

    /** 키 목록과 상태. 키 내용(공개키 포함)은 응답하지 않는다. 공개키는 /oauth2/jwks에서 조회한다. */
    @GetMapping
    fun list(): List<SigningKeyView> = listSigningKeysQuery.list()

    /** 즉시 새 키로 교체한다. 이전 키는 보존 기간 동안 JWKS에 남아 기존 토큰 검증에 쓰인다. */
    @PostMapping("/rotate")
    fun rotate(): RotateResponse = RotateResponse(rotateSigningKeyUseCase.rotate())

    data class RotateResponse(val kid: String)
}
