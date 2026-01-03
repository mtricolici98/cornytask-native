package com.nobadhabbits.cornytask.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class History(
    val id: String = "",
    val title: String = "",
    val rewardCoins: Int = 0,
    val type: String = "TASK", // TASK or TIME_GOAL
    val durationMinutes: Long? = null,
    val timeGoalId: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null
)
