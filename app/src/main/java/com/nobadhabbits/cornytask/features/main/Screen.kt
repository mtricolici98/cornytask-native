package com.nobadhabbits.cornytask.features.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val MORE_GRAPH = "More"
    const val MORE_MAIN = "More/more_main"
    const val TIMEGOALS_GRAPH = "TimeGoals"
    const val TIMEGOALS_MAIN = "TimeGoalsMain"
}

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Todo : Screen("TODOs", "TODOs", Icons.Default.List)
    object Rewards : Screen("Rewards", "Rewards", Icons.Default.Favorite)
    object TimeGoals : Screen("TimeGoalsMain", "Time Goals", Icons.Default.Timelapse)
    object More : Screen("More/more_main", "More", Icons.Default.Menu)
}
sealed class MoreScreen(val route: String, val title: String, val icon: ImageVector) {
    object History : MoreScreen("More/history", "History", Icons.Default.Check)
    object Notes : MoreScreen("More/notes", "Notes", Icons.Filled.Note)
    object Settings : MoreScreen("More/settings", "Settings", Icons.Default.Settings)
}
