package com.dalmeng.template.idempotency

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import tools.jackson.databind.ObjectMapper

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class IdempotencyAspect(
    private val idempotencyService: IdempotencyService,
    private val objectMapper: ObjectMapper
) {
    @Around("@annotation(idempotent)")
    fun around(joinPoint: ProceedingJoinPoint, idempotent: Idempotent): Any? {
        val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request

        // 1. [400 Bad Request] 헤더 검증
        val key = request.getHeader(idempotent.headerName)
            ?: throw BadRequestIdempotencyKey()

        // 2. [422 Unprocessable] 비교를 위한 파라미터 해싱
        // Service 메서드의 인자값들을 해싱하여 페이로드 변조 여부를 체크합니다.
        val method = request.method
        val path = request.requestURI
        val requestTarget = "$method $path"
        val requestHash = idempotencyService.hash(joinPoint.args)

        // 3. 멱등성 레코드 생성/조회 (409 Conflict 등 발생 지점)
        val beginResult = idempotencyService.begin(key, requestTarget, requestHash)

        if (beginResult is IdempotencyBeginResult.AlreadyCompleted) {
            // 이미 성공한 기록이 있다면 이미 처리 되었다는 응답
            // throw IdempotencyExceptions.AlreadyProcessedIdempotencyKey()

            // 이미 성공한 기록이 있다면 로직을 타지 않고 바로 결과 반환
            return deserializeResponse(beginResult.record.responseBody, joinPoint)
        }

        val record = (beginResult as? IdempotencyBeginResult.Proceed)?.record
            ?: (beginResult as IdempotencyBeginResult.Reprocessable).record

        return try {
            // 4. 실제 비즈니스 로직 실행 (Transaction 시작점)
            val result = joinPoint.proceed()

            // 5. 성공 시 결과 저장 및 상태 변경
            val responseBody = objectMapper.writeValueAsString(result)
            idempotencyService.complete(record.id!!, 200, responseBody)
            result
        } catch (e: Exception) {
            // 만약 여기서 터진 에러가 '이미 내가 던진 Idempotency 관련 예외'라면?
            if (e is IdempotencyException) throw e

            // 진짜 비즈니스 로직 실패일 때만 FAILED로 변경
            idempotencyService.fail(record.id!!)
            throw e
        }
    }

    private fun deserializeResponse(body: String?, joinPoint: ProceedingJoinPoint): Any? {
        val methodSignature = joinPoint.signature as org.aspectj.lang.reflect.MethodSignature
        val returnType = methodSignature.returnType
        return objectMapper.readValue(body ?: "", returnType)
    }
}