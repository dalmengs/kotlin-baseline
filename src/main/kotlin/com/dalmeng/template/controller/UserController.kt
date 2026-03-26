package com.dalmeng.template.controller

import com.dalmeng.template.dto.request.UserCreateRequest
import com.dalmeng.template.dto.response.UserResponse
import com.dalmeng.template.other.BaseResponse
import com.dalmeng.template.service.UserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/user")
@RestController
class UserController(
    private val userService: UserService
){
    @GetMapping("/{name}")
    fun findUserByName(@PathVariable name: String): BaseResponse<UserResponse>
        = BaseResponse.ok(data = userService.findUserByName(name))


    @PostMapping
    fun createUser(@RequestBody request: UserCreateRequest): BaseResponse<UserResponse>
            = BaseResponse.ok(data = userService.createUser(request))
}