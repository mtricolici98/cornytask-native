package com.nobadhabbits.cornytask.data

import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * What has happened to one item of the MedAT study plan. The plan itself is bundled with
 * the app (assets/medat_plan.json); only items that were touched get a document here.
 */
data class MedatItemProgress(
    val id: String = "",
    // The plan day the item sits on now. Null means its original day.
    val day: Int? = null,
    @get:PropertyName("isDone")
    @set:PropertyName("isDone")
    var isDone: Boolean = false,
    val completedAt: Date? = null,
    val postponedCount: Int = 0,
    val timeGoalId: String? = null,
    val todoId: String? = null,
    // Unicorns paid for ticking it off by hand (taken back if unticked), and that history entry.
    val rewardCoins: Int = 0,
    val historyId: String? = null
)

/** How a calendar date is used for MedAT. Dates without a mark are study days. */
enum class MedatDayType { STUDY, BREAK, SIMULATION }
