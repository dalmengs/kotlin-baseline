package com.dalmeng.template.other

open class BaseException(
    val errorCode: Int,
    val errorMessage: String? = null
) : Exception()