package com.dalmeng.template.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "transactions",
    indexes = [
        Index(name = "idx_transaction_idempotency_key", columnList = "idempotencyKey", unique = true),
        Index(name = "idx_transaction_user_id", columnList = "user_id")
    ]
)
class Transaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val amount: BigDecimal,

    @Column(nullable = false, unique = true)
    val idempotencyKey: String = UUID.randomUUID().toString(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: TransactionType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: TransactionStatus,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    // =======================================================

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun create(amount: BigDecimal, type: TransactionType, status: TransactionStatus, idempotencyKey: String, user: User): Transaction {
            return Transaction(
                amount = amount,
                type = type,
                idempotencyKey = idempotencyKey,
                status = status,
                user = user,
            )
        }
    }
}