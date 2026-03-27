package com.dalmeng.template.idempotency

import com.dalmeng.template.other.BaseException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.security.MessageDigest

@Service
class IdempotencyService(
    private val repository: IdempotencyRecordRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun begin(key: String, requestTarget: String, requestHash: String): IdempotencyBeginResult {
        validateKey(key)

        val existing = repository.findByKeyAndTargetWithLock(key, requestTarget)

        if (existing != null) {
            validateSameRequest(existing, requestHash)

            return when (existing.status) {
                IdempotencyStatus.PROCESSING -> {
                    throw ConflictIdempotencyKey()
                }

                IdempotencyStatus.COMPLETED -> {
                    IdempotencyBeginResult.AlreadyCompleted(existing)
                }

                IdempotencyStatus.FAILED -> {
                    existing.status = IdempotencyStatus.PROCESSING
                    existing.responseStatus = null
                    existing.responseBody = null
                    IdempotencyBeginResult.Reprocessable(existing)
                }
            }
        }

        return tryCreateProcessingRecord(key, requestTarget, requestHash)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun complete(recordId: Long, responseStatus: Int, responseBody: String) {
        val record = repository.findById(recordId)
            .orElseThrow { IdempotencyRecordNotFound() }

        record.status = IdempotencyStatus.COMPLETED
        record.responseStatus = responseStatus
        record.responseBody = responseBody
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun fail(recordId: Long) {
        val record = repository.findById(recordId)
            .orElseThrow { IdempotencyRecordNotFound() }

        record.status = IdempotencyStatus.FAILED
    }

    fun hash(payload: Any?): String {
        val raw = objectMapper.writeValueAsString(payload ?: emptyMap<String, Any>())
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun tryCreateProcessingRecord(
        key: String,
        requestTarget: String,
        requestHash: String
    ): IdempotencyBeginResult {
        return try {
            val saved = repository.save(
                IdempotencyRecord(
                    idempotencyKey = key,
                    requestHash = requestHash,
                    requestTarget = requestTarget,
                    status = IdempotencyStatus.PROCESSING
                )
            )
            IdempotencyBeginResult.Proceed(saved)
        } catch (e: DataIntegrityViolationException) {
            val existing = repository.findByKeyAndTargetWithLock(key, requestTarget)
                ?: throw e

            validateSameRequest(existing, requestHash)

            when (existing.status) {
                IdempotencyStatus.PROCESSING -> {
                    throw ConflictIdempotencyKey()
                }

                IdempotencyStatus.COMPLETED -> {
                    IdempotencyBeginResult.AlreadyCompleted(existing)
                }

                IdempotencyStatus.FAILED -> {
                    existing.status = IdempotencyStatus.PROCESSING
                    existing.responseStatus = null
                    existing.requestTarget = null
                    existing.responseBody = null
                    IdempotencyBeginResult.Reprocessable(existing)
                }
            }
        }
    }

    private fun validateSameRequest(
        record: IdempotencyRecord,
        requestHash: String
    ) {
        if (record.requestHash != requestHash) {
            throw UnprocessableIdempotencyKey()
        }
    }

    private fun validateKey(key: String) {
        if (key.isBlank()) {
            throw BadRequestIdempotencyKey()
        }

        if (key.length > 300) {
            throw BadRequestIdempotencyKey()
        }
    }
}

sealed class IdempotencyBeginResult {
    data class Proceed(val record: IdempotencyRecord) : IdempotencyBeginResult()
    data class AlreadyCompleted(val record: IdempotencyRecord) : IdempotencyBeginResult()
    data class Reprocessable(val record: IdempotencyRecord) : IdempotencyBeginResult()
}

sealed class IdempotencyException(errorCode: Int, errorMessage: String) : BaseException(
    errorCode,
    errorMessage,
)

class BadRequestIdempotencyKey : IdempotencyException(
    errorCode = 400,
    errorMessage = "유효하지 않은 멱등 키입니다."
)

class ConflictIdempotencyKey : IdempotencyException(
    errorCode = 409,
    errorMessage = "동일한 멱등 키의 요청이 이미 처리 중입니다."
)

class AlreadyProcessedIdempotencyKey : IdempotencyException(
    errorCode = 409,
    errorMessage = "이미 처리된 요청입니다."
)

class UnprocessableIdempotencyKey : IdempotencyException(
    errorCode = 422,
    errorMessage = "동일한 멱등 키로 서로 다른 요청을 보낼 수 없습니다."
)

class IdempotencyRecordNotFound : IdempotencyException(
    errorCode = 500,
    errorMessage = "멱등성 레코드를 찾을 수 없습니다."
)