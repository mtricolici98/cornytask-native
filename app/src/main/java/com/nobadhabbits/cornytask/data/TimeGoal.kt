package com.nobadhabbits.cornytask.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class TimeGoal(
    val id: String = "",
    val title: String = "",
    val totalTimeMinutes: Long = 0,
    val remainingTimeMinutes: Long = 0,
    val rewardCoins: Int = 0,
    @ServerTimestamp
    val createdAt: Date? = null
)
