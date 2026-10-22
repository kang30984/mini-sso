package io.github.kang30984.minisso.adapter.inbound.web.common

import io.github.kang30984.minisso.domain.common.BusinessException
import io.github.kang30984.minisso.domain.common.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.URI

/**
 * 모든 오류를 RFC 9457 ProblemDetail + 자체 code 필드로 응답한다.
 * - Spring MVC 표준 예외(잘못된 JSON, 파라미터 누락, 405 등)는 상위 클래스가 처리하고 code만 덧붙인다.
 * - 예상하지 못한 예외는 내부 정보(스택, SQL)를 노출하지 않고 일반 메시지로 응답한다.
 */
@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(e: BusinessException): ProblemDetail =
        problem(e.errorCode.httpStatus(), e.errorCode, e.message ?: e.errorCode.message)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ProblemDetail {
        log.error("Unhandled exception", e)
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다.").apply {
            setProperty("code", "COMMON-500")
        }
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val body = problem(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_INPUT).apply {
            setProperty(
                "errors",
                ex.bindingResult.fieldErrors.map { mapOf("field" to it.field, "message" to it.defaultMessage) },
            )
        }
        return handleExceptionInternal(ex, body, headers, status, request)
    }

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        if (body is ProblemDetail && body.properties?.containsKey("code") != true) {
            val code = if (statusCode.value() == HttpStatus.BAD_REQUEST.value()) {
                ErrorCode.INVALID_INPUT.code
            } else {
                "COMMON-${statusCode.value()}"
            }
            body.setProperty("code", code)
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request)
    }

    private fun problem(status: HttpStatus, errorCode: ErrorCode, detail: String = errorCode.message): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            type = ERROR_DOC
            title = errorCode.name
            setProperty("code", errorCode.code)
        }

    companion object {
        private val ERROR_DOC: URI = URI.create("https://github.com/kang30984/mini-sso/blob/main/docs/error-codes.md")
    }
}
