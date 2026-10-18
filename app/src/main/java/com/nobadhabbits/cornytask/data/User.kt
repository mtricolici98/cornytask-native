package com.nobadhabbits.cornytask.data

data class User(
    val uid: String = "",
    val coins: Int = 0,
    val firstLogin: Boolean = false,
    val fcmToken: String? = null,
    val activeTimeGoalId: String? = null,
    val activeTimeGoalStartTimeMillis: Long? = null,
    // Length of the running session. Null for timers started before sessions could be shorter.
    val activeTimeGoalDurationMillis: Long? = null
)
