package com.dalmeng.template.repository

import org.springframework.data.jpa.repository.JpaRepository
import com.dalmeng.template.entity.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TransactionRepository : JpaRepository<Transaction, Long> {
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.user.id = :userId
        ORDER BY t.createdAt DESC
    """)
    fun findTransactions(
        @Param("userId") userId: Long,
        pageable: Pageable
    ): Page<Transaction>
}