package com.dalmeng.template.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(
    name = "user_wallet",
    indexes = [
        Index(name = "idx_user_wallet_user_id", columnList = "user_id")
    ]
)
class UserWallet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Column(nullable = false)
    var balance: BigDecimal = BigDecimal.ZERO
)