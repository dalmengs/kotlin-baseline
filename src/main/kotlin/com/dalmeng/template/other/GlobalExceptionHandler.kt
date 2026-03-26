package com.dalmeng.template.other

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BaseException::class)
    fun handleBaseException(e: BaseException): ResponseEntity<BaseResponse<Nothing>> {
        return ResponseEntity
            .status(e.errorCode)
            .body(BaseResponse.failed(
                statusCode = e.errorCode,
                errorMessage = e.errorMessage
            ))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<BaseResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(BaseResponse.failed(
                statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                errorMessage = e.message
            ))
    }
}