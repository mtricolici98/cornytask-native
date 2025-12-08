package com.example.cornytask_v2.features.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cornytask_v2.R
import com.example.cornytask_v2.data.History
import com.example.cornytask_v2.ui.theme.Purple40
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val weeklyHistory by viewModel.weeklyHistory.collectAsState()
    var selectedDate by remember { mutableStateOf(Date().stripTime()) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (weeklyHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No history yet!")
            }
        } else {
            WeeklyCalendarView(weeklyHistory, selectedDate) { date ->
                selectedDate = date
            }
            Spacer(modifier = Modifier.height(16.dp))
            HistoryDetailView(weeklyHistory.find { it.days.containsKey(selectedDate) }?.days?.get(selectedDate) ?: emptyList())
        }
    }
}

@Composable
fun WeeklyCalendarView(
    weeklyHistory: List<WeeklyHistory>,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit
) {
    LazyRow(modifier = Modifier.fillMaxWidth(), reverseLayout = true) {
        items(weeklyHistory) { week ->
            Card(
                modifier = Modifier.padding(8.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = "Week ${week.weekOfYear}, ${week.year}", style = MaterialTheme.typography.titleMedium, color = Purple40)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Create indicators for all 7 days of the week
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.YEAR, week.year)
                        cal.set(Calendar.WEEK_OF_YEAR, week.weekOfYear)
                        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)

                        repeat(7) {
                            val date = cal.time.stripTime()
                            DayIndicator(
                                date = date,
                                count = week.days[date]?.size ?: 0,
                                isSelected = selectedDate == date
                            ) {
                                onDateSelected(date)
                            }
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayIndicator(date: Date, count: Int, isSelected: Boolean, onClick: () -> Unit) {
    val dayOfWeekFormat = SimpleDateFormat("E", Locale.getDefault())
    val dayOfMonthFormat = SimpleDateFormat("d", Locale.getDefault())

    val color = if (count > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (count > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val modifier = if (isSelected) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Text(dayOfWeekFormat.format(date), color = textColor)
        Text(dayOfMonthFormat.format(date), fontWeight = FontWeight.Bold, color = textColor)
        Text(if (count > 0) "$count" else "-", color = textColor)
    }
}


@Composable
fun HistoryDetailView(historyItems: List<History>) {
    if (historyItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No tasks completed on this day.")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(historyItems) { item ->
                ListItem(
                    headlineContent = { Text(item.title) },
                    leadingContent = { Checkbox(checked = true, onCheckedChange = null) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = item.rewardCoins.toString())
                            Spacer(Modifier.width(4.dp))
                            Image(
                                painter = painterResource(id = R.drawable.unicorn_small),
                                contentDescription = "Coins",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}