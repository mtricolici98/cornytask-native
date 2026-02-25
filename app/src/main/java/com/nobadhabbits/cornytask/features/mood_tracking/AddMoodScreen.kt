package com.nobadhabbits.cornytask.features.mood_tracking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nobadhabbits.cornytask.features.mood_tracking.data.TimeOfDay
import java.util.Date

@Composable
fun AddMoodScreen(
    onAddMood: (Date, TimeOfDay, Int) -> Unit
) {
    var selectedTimeOfDay by remember { mutableStateOf(TimeOfDay.Morning) }
    var moodScore by remember { mutableStateOf(0) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddMood(Date(), selectedTimeOfDay, moodScore) }
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Add Mood")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Time of day", style = MaterialTheme.typography.titleMedium)

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val items = TimeOfDay.values()
                        items.forEachIndexed { index, tod ->
                            SegmentedButton(
                                selected = selectedTimeOfDay == tod,
                                onClick = { selectedTimeOfDay = tod },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size)
                            ) { Text(tod.name) }
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mood", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        AssistChip(
                            onClick = { /* no-op */ },
                            label = { Text("Selected: $moodScore") }
                        )
                    }

                    // More compact and less “tall”
                    MoodBarSelectorCompact(
                        moodScore = moodScore,
                        onMoodScoreChange = { moodScore = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Tap a level to select it.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "If you ever feel unsafe or overwhelmed, please reach out to a trusted adult or local emergency help.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp) // keep space so FAB doesn't cover text
            )
        }
    }
}

@Composable
fun MoodBarSelectorCompact(
    moodScore: Int,
    onMoodScoreChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val levels = remember {
        listOf(
            MoodLevel(-3, "😣", "Very distressed"),
            MoodLevel(-2, "😟", "Low"),
            MoodLevel(-1, "🙁", "Off"),
            MoodLevel(0, "😐", "Normal"),
            MoodLevel(1, "🙂", "Good"),
            MoodLevel(2, "😄", "Great"),
            MoodLevel(3, "🤯", "Manic")
        )
    }

    val selectedContainer = MaterialTheme.colorScheme.primaryContainer
    val unselectedContainer = MaterialTheme.colorScheme.surfaceVariant
    val selectedContent = MaterialTheme.colorScheme.onPrimaryContainer
    val unselectedContent = MaterialTheme.colorScheme.onSurfaceVariant

    // Fixed compact row height per item -> never truncates
    Column(
        modifier = modifier.clip(RoundedCornerShape(18.dp)),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // top = 3 down to -3
        levels.asReversed().forEach { level ->
            val selected = moodScore == level.score

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (selected) selectedContainer else unselectedContainer,
                tonalElevation = if (selected) 2.dp else 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp) // compact height
                    .clickable { onMoodScoreChange(level.score) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = level.emoji,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.Center
                    )

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = level.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selected) selectedContent else unselectedContent
                        )
                    }

                    Text(
                        text = level.score.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selected) selectedContent else unselectedContent,
                        modifier = Modifier.padding(start = 8.dp)
                    )

                    if (selected) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.titleLarge,
                            color = selectedContent
                        )
                    }
                }
            }
        }
    }
}

private data class MoodLevel(
    val score: Int,
    val emoji: String,
    val label: String
)