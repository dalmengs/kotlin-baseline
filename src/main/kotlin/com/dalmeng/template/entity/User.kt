package com.dalmeng.template.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_user_name", columnList = "name"),
        Index(name = "idx_user_email", columnList = "email"),
    ]
)
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val email: String,
) {
    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var wallet: UserWallet? = null

    companion object {
        fun create(name: String, email: String): User {
            val user = User(name = name, email = email)
            val wallet = UserWallet(
                user = user,
                balance = 0.toBigDecimal(),
            )
            user.wallet = wallet

            return user
        }
    }
}