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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Date
import java.text.SimpleDateFormat
import java.util.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMoodScreen(
    onAddMood: (date: Date, moodScore: Int, comment: String) -> Unit
) {
    var moodScore by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var selectedDateTime by remember { mutableStateOf(Date()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val calendar = remember { Calendar.getInstance() }

    val formattedDate = remember(selectedDateTime) {
        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
            .format(selectedDateTime)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddMood(selectedDateTime, moodScore, comment) }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
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
                            onClick = { showDatePicker = true },
                            label = { Text(formattedDate) }
                        )
                    }

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

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("What happened?", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Optional comment about your day") },
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                }
            }

            // Keep the comment field clear of the FAB when scrolled to the bottom
            Spacer(Modifier.height(72.dp))
        }
    }

    // -----------------------
    // DATE PICKER
    // -----------------------
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        calendar.timeInMillis = millis
                        showDatePicker = false
                        showTimePicker = true
                    }
                }) {
                    Text("Next")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // -----------------------
    // TIME PICKER
    // -----------------------
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(Calendar.MINUTE, timePickerState.minute)

                    selectedDateTime = calendar.time
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
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

    Column(
        modifier = modifier.clip(RoundedCornerShape(18.dp)),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        levels.asReversed().forEach { level ->
            val selected = moodScore == level.score

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (selected) selectedContainer else unselectedContainer,
                tonalElevation = if (selected) 2.dp else 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
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