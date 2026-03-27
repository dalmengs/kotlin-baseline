package com.dalmeng.template.idempotency

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "idempotency_record",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_idempotency_key_target",
            columnNames = ["idempotencyKey", "requestTarget"]
        )
    ],
    indexes = [
        Index(name = "idx_created_at", columnList = "createdAt") // 스케줄러 삭제 성능 향상
    ]
)
class IdempotencyRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 512)
    val idempotencyKey: String,

    @Column(nullable = false, length = 255)
    var requestTarget: String? = null,

    @Column(nullable = false, length = 64)
    var requestHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: IdempotencyStatus,

    @Lob
    var responseBody: String? = null,

    var responseStatus: Int? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class IdempotencyStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}