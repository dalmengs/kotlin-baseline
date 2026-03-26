package com.dalmeng.template.exception

import com.dalmeng.template.other.BaseException

class WalletNotFoundException(
    errorCode: Int = 404,
    errorMessage: String? = "Wallet not found"
) : BaseException(
    errorCode = errorCode,
    errorMessage = errorMessage
)