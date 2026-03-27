package com.dalmeng.template.dto.request

import com.dalmeng.template.exception.InvalidArgumentException

data class UserCreateRequest(
    val name: String,
    val email: String,
) {
    fun validate() {
        if(name.isBlank()){
            throw InvalidArgumentException(
                errorCode = 400,
                errorMessage = "Name can't be empty"
            )
        }
        if(name.length > 20){
            throw InvalidArgumentException(
                errorCode = 401,
                errorMessage = "Name is too long"
            )
        }
        if(!email.contains("@")) {
            throw InvalidArgumentException(
                errorCode = 400,
                errorMessage = "Invalid email format"
            )
        }
    }
}