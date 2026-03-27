package com.dalmeng.template.service

import com.dalmeng.template.dto.request.PaymentRequest
import com.dalmeng.template.dto.response.TransactionResponse
import com.dalmeng.template.entity.Transaction
import com.dalmeng.template.entity.TransactionStatus
import com.dalmeng.template.entity.TransactionType
import com.dalmeng.template.exception.WalletNotFoundException
import com.dalmeng.template.idempotency.Idempotent
import com.dalmeng.template.other.BaseException
import com.dalmeng.template.other.BasePagingResponse
import com.dalmeng.template.repository.TransactionRepository
import com.dalmeng.template.repository.WalletRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TransactionService(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository
) {

    @Idempotent
    @Transactional
    fun payment(paymentRequest: PaymentRequest): TransactionResponse {
        if (paymentRequest.amount.toInt() == 9) {
            Thread.sleep(3 * 1000)
        }

        // 잔액 조회 - 락
        val wallet = walletRepository.findByUserIdWithLock(paymentRequest.userId) ?: throw WalletNotFoundException()

        val balanceBefore = wallet.balance
        var status = TransactionStatus.SUCCESS

        // DEPOSIT - 입금, WITHDRAW - 출금
        val isWithdraw = paymentRequest.type == TransactionType.WITHDRAW
        val isInsufficient = isWithdraw && (balanceBefore < paymentRequest.amount)

        // 잔액 부족
        if (isInsufficient) {
            status = TransactionStatus.INSUFFICIENT_BALANCE
        } else { // 잔액 변경
            wallet.balance = when(paymentRequest.type) {
                TransactionType.DEPOSIT -> balanceBefore + paymentRequest.amount
                TransactionType.WITHDRAW -> balanceBefore - paymentRequest.amount
            }
        }

        // 멱등성 키로 이미 처리된 거 다시 처리하려고 하면 오류 남
        try {
            val transaction = Transaction.create(
                amount = paymentRequest.amount,
                type = paymentRequest.type,
                status = status,
                user = wallet.user
            )
            val saved = transactionRepository.save(transaction)

            return TransactionResponse.of(
                transaction = saved,
                balanceBeforeTransaction = balanceBefore,
                balanceAfterTransaction = wallet.balance
            )
        } catch (e: DataIntegrityViolationException) {
            throw BaseException(409, "Transaction is already being processed")
        }
    }

    @Transactional(readOnly = true)
    fun getTransactions(userId: Long?, pageable: Pageable): BasePagingResponse<TransactionResponse> {
        userId ?: throw BaseException(400, "User id not set")

        val transactions = transactionRepository.findTransactions(userId, pageable)
        return BasePagingResponse.of(transactions) { transaction -> TransactionResponse.of(transaction) }
    }
}