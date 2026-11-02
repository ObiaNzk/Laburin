package com.example.laburin.model

data class User (
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val cvUrl: String? = null,
    val type: UserType
)

enum class UserType {
    CANDIDATE,
    BUSINESS
}