package com.dalmeng.template.service

import com.dalmeng.template.dto.request.UserCreateRequest
import com.dalmeng.template.dto.response.UserResponse
import com.dalmeng.template.entity.User
import com.dalmeng.template.exception.UserNotFoundException
import com.dalmeng.template.idempotency.Idempotent
import com.dalmeng.template.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun findUserByName(name: String): UserResponse {
        val user = userRepository.findByNameWithWallet(name) ?: throw UserNotFoundException()
        return UserResponse.of(user)
    }

    @Idempotent
    @Transactional
    fun createUser(userCreateRequest: UserCreateRequest): UserResponse {
        userCreateRequest.validate()

        val user = User.create(userCreateRequest.name, userCreateRequest.email)
        val saved = userRepository.save(user)

        return UserResponse.of(saved)
    }
}