package com.dalmeng.template.controller

import com.dalmeng.template.dto.request.PaymentRequest
import com.dalmeng.template.dto.response.TransactionResponse
import com.dalmeng.template.other.BasePagingResponse
import com.dalmeng.template.other.BaseResponse
import com.dalmeng.template.service.TransactionService
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/transaction")
@RestController
class TransactionController(
    private val transactionService: TransactionService
){
    @PostMapping
    fun payment(
        @RequestBody request: PaymentRequest,
        @RequestHeader("Idempotency-Key") idempotencyKey: String
    ): BaseResponse<TransactionResponse>
            = BaseResponse.ok(data = transactionService.payment(request, idempotencyKey))

    @GetMapping
    fun getTransactions(
        @RequestParam userId: Long?,
        pageable: Pageable
    ): BaseResponse<BasePagingResponse<TransactionResponse>> {
        return BaseResponse.ok(data = transactionService.getTransactions(userId, pageable))
    }
}