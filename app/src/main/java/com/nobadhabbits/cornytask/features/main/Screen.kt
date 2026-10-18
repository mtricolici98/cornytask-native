package com.nobadhabbits.cornytask.features.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val TODOS = "todos"
    const val REWARDS = "rewards"
    const val TIME_GOALS = "time_goals"
    const val TIMER = "time_goals/timer"
    const val HISTORY = "history"
    const val NOTES = "notes"
    const val NEW_NOTE = "notes/new"
    const val EDIT_NOTE = "notes/edit/{noteId}"
    const val MOOD = "mood"
    const val ADD_MOOD = "mood/add"
    const val MEDAT = "medat"
    const val SETTINGS = "settings"

    fun editNote(noteId: String) = "notes/edit/$noteId"
}

/**
 * Top-level screens. The user picks their order (see [NavOrderPreferences]); the first
 * [BAR_SLOTS] go in the bottom bar, the rest live in the expandable More menu.
 */
enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    // Sub-routes that still count as "being on" this destination.
    private val childRoutes: Set<String> = emptySet()
) {
    TODOS(Routes.TODOS, "TODOs", Icons.AutoMirrored.Filled.List),
    REWARDS(Routes.REWARDS, "Rewards", Icons.Default.Favorite),
    TIME_GOALS(Routes.TIME_GOALS, "Time Goals", Icons.Default.Timelapse, setOf(Routes.TIMER)),
    HISTORY(Routes.HISTORY, "History", Icons.Default.Check),
    NOTES(Routes.NOTES, "Notes", Icons.AutoMirrored.Filled.Note, setOf(Routes.NEW_NOTE, Routes.EDIT_NOTE)),
    MOOD(Routes.MOOD, "Mood", Icons.Default.Face, setOf(Routes.ADD_MOOD)),
    MEDAT(Routes.MEDAT, "MedAT", Icons.Default.School),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Default.Settings);

    fun matches(route: String?): Boolean = route == this.route || route in childRoutes

    companion object {
        const val BAR_SLOTS = 4

        fun forRoute(route: String?): Destination? = entries.firstOrNull { it.matches(route) }
    }
}
