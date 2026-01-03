package com.example.cornytask_v2.features.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val icon: ImageVector) {
    object Todo : Screen("TODOs", Icons.Default.List)
    object Rewards : Screen("Rewards", Icons.Default.Favorite)
    object TimeGoals : Screen("TimeGoals", Icons.Default.Timelapse)
    object More : Screen("More", Icons.Default.Menu)
}

sealed class MoreScreen(val route: String, val title: String, val icon: ImageVector) {
    object History : MoreScreen("history", "History", Icons.Default.Check)
    object Notes : MoreScreen("notes", "Notes", Icons.Filled.Note)
}
