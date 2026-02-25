package com.nobadhabbits.cornytask.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class MoodTrackRecord(
        val id: String = "",
        val score: Int = 4,
        @ServerTimestamp val timestamp: Date? = null
)
