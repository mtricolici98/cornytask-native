package com.example.cornytask_v2.features.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val icon: ImageVector) {
    object Todo : Screen("TODOs", Icons.Default.List)
    object Rewards : Screen("Rewards", Icons.Default.Favorite)
    object History : Screen("History", Icons.Default.Check)
}
