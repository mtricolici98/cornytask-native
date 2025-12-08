package com.example.cornytask_v2.data

import com.google.firebase.firestore.PropertyName

data class Reward(
    val id: String = "",
    val title: String = "",
    val cost: Int = 0,
    @get:PropertyName("isFavorite") // Explicitly map the 'isCompleted' field
    @set:PropertyName("isFavorite")
    var isFavorite: Boolean = false
)
