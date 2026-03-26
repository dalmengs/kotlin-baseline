package com.dalmeng.template.repository

import com.dalmeng.template.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Long> {
    @Query("""
        SELECT u
        FROM User u
        JOIN FETCH u.wallet
        WHERE u.name = :name
    """)
    fun findByNameWithWallet(@Param("name") name: String): User?
}