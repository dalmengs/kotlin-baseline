package com.dalmeng.template.idempotency

import jakarta.transaction.Transactional
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class IdempotencyCleanupScheduler(
    private val repository: IdempotencyRecordRepository
) {
    @Scheduled(fixedRate = 10 * 1000) // ms
    @Transactional
    fun cleanup() {
        // 5초 이상 된 거 삭제
        val expirationDate = LocalDateTime.now().minusSeconds(5)
        repository.deleteByCreatedAtBefore(expirationDate)
    }
}