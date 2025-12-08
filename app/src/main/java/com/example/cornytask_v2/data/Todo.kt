package com.example.cornytask_v2.data

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Todo(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val keywords: List<String> = emptyList(),
    val rewardCoins: Int = 0,
    @get:PropertyName("isCompleted") // Explicitly map the 'isCompleted' field
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    @ServerTimestamp
    val createdAt: Date? = null,
    val historyId: String? = null
)
