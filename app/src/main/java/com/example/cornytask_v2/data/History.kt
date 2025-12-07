package com.example.cornytask_v2.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class History(
    val id: String = "",
    val title: String = "",
    val rewardCoins: Int = 0,
    @ServerTimestamp
    val createdAt: Date? = null
)