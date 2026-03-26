package com.dalmeng.template.repository

import com.dalmeng.template.entity.UserWallet
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WalletRepository : JpaRepository<UserWallet, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT w
        FROM UserWallet w
        JOIN FETCH w.user u
        WHERE w.user.id = :userId
    """)
    fun findByUserIdWithLock(@Param("userId") userId: Long): UserWallet?
}