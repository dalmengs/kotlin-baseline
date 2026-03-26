package com.dalmeng.template.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_user_name", columnList = "name")
    ]
)
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val name: String,
) {
    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var wallet: UserWallet? = null

    companion object {
        fun create(name: String): User {
            val user = User(name = name)
            val wallet = UserWallet(
                user = user,
                balance = 0.toBigDecimal(),
            )
            user.wallet = wallet

            return user
        }
    }
}