package com.nobadhabbits.cornytask.features.mood_tracking.data

import com.google.firebase.firestore.DocumentId
import java.util.Date

enum class TimeOfDay {
    Morning,
    Afternoon,
    Evening
}

data class MoodRecord(
    @DocumentId val id: String = "",
    val timestamp: Date = Date(),
    val timeOfDay: TimeOfDay = TimeOfDay.Morning,
    val moodScore: Int = 0,
    val userId: String = ""
)
