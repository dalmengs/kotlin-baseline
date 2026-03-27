package com.dalmeng.template.idempotency

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface IdempotencyRecordRepository : JpaRepository<IdempotencyRecord, Long> {

    fun findByIdempotencyKey(idempotencyKey: String): IdempotencyRecord?

    fun deleteByCreatedAtBefore(time: LocalDateTime)

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from IdempotencyRecord r where r.idempotencyKey = :key and r.requestTarget = :target")
    fun findByKeyAndTargetWithLock(key: String, target: String): IdempotencyRecord?
}