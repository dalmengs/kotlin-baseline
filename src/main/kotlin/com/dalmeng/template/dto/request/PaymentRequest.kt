package com.dalmeng.template.dto.request

import com.dalmeng.template.entity.TransactionType
import com.dalmeng.template.exception.InvalidArgumentException
import java.math.BigDecimal

data class PaymentRequest(
    val userId: Long,
    val amount: BigDecimal,
    val type: TransactionType
) {
    fun validate() {
        if(amount <= BigDecimal.ZERO) {
            throw InvalidArgumentException(
                errorCode = 400,
                errorMessage = "Amount must be positive"
            )
        }
    }
}