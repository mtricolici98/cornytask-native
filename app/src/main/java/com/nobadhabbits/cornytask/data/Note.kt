package com.nobadhabbits.cornytask.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Note(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    @ServerTimestamp val timestamp: Date? = null
)
