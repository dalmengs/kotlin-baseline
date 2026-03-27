package com.dalmeng.template.idempotency

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Idempotent(
    val headerName: String = "Idempotency-Key"
)
