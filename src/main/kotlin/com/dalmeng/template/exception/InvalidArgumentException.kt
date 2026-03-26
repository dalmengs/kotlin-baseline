package com.dalmeng.template.exception

import com.dalmeng.template.other.BaseException

class InvalidArgumentException(
    errorCode: Int,
    errorMessage: String? = null
) : BaseException(
    errorCode = errorCode,
    errorMessage = errorMessage
)