package com.dalmeng.template.dto.response

import com.dalmeng.template.entity.Transaction
import com.dalmeng.template.entity.TransactionStatus
import com.dalmeng.template.entity.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime

data class TransactionResponse(
    val id: Long,
    val amount: BigDecimal,
    val type: TransactionType,
    val status: TransactionStatus,
    val createdAt: LocalDateTime,
    val balanceBeforeTransaction: BigDecimal? = null,
    val balanceAfterTransaction: BigDecimal? = null,
) {
    companion object {
        fun of(
            transaction: Transaction,
            balanceBeforeTransaction: BigDecimal? = null,
            balanceAfterTransaction: BigDecimal? = null,
        ): TransactionResponse {
            return TransactionResponse(
                id = transaction.id!!,
                amount = transaction.amount,
                type = transaction.type,
                status = transaction.status,
                createdAt = transaction.createdAt,
                balanceBeforeTransaction = balanceBeforeTransaction,
                balanceAfterTransaction = balanceAfterTransaction,
            )
        }
    }
}