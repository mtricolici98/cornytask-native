package com.nobadhabbits.cornytask.features.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nobadhabbits.cornytask.features.main.MoreScreen as MoreScreenItems

@Composable
fun MoreScreen(navController: NavController) {
    val items = listOf(
        MoreScreenItems.History,
        MoreScreenItems.Notes,
        MoreScreenItems.Settings
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        items.forEach { screen ->
            ListItem(
                headlineContent = { Text(screen.title) },
                leadingContent = { Icon(screen.icon, contentDescription = null) },
                modifier = Modifier.clickable { navController.navigate(screen.route) }
            )
        }
    }
}