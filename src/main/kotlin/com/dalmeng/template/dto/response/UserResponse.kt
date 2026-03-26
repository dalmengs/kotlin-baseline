package com.dalmeng.template.dto.response

import com.dalmeng.template.entity.User
import java.math.BigDecimal

data class UserResponse(
    val id: Long,
    val name: String,
    val balance: BigDecimal
) {
    companion object {
        fun of(user: User): UserResponse {
            return UserResponse(
                id = user.id!!,
                name = user.name,
                balance = user.wallet?.balance ?: BigDecimal(0),
            )
        }
    }
}