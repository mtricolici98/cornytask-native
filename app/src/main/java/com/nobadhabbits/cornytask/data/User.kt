package com.nobadhabbits.cornytask.data

data class User(
    val uid: String = "",
    val coins: Int = 0,
    val firstLogin: Boolean = false,
    val fcmToken: String? = null
)
