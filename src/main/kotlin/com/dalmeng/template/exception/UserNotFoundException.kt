package com.dalmeng.template.exception

import com.dalmeng.template.other.BaseException

class UserNotFoundException(
    errorCode: Int = 404,
    errorMessage: String? = "User not found"
) : BaseException(
    errorCode = errorCode,
    errorMessage = errorMessage
)