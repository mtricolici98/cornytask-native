package com.nobadhabbits.cornytask.features.calendar

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.ContentHeightMode
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.nobadhabbits.cornytask.data.Todo
import com.nobadhabbits.cornytask.features.todo.AddTodoActivity
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(calendarViewModel: CalendarViewModel = viewModel()) {
    val todosByDate by calendarViewModel.todosByDate.collectAsState()
    val selectedDate by calendarViewModel.selectedDate.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val currentMonth = YearMonth.now()
    val startMonth = currentMonth.minusMonths(100)
    val endMonth = currentMonth.plusMonths(100)
    val firstDayOfWeek = DayOfWeek.MONDAY
    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    val calendar = @Composable {
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                Day(
                    day,
                    isSelected = selectedDate == day.date,
                    events = todosByDate[day.date] ?: emptyList()
                ) { clickedDay ->
                    calendarViewModel.onDateSelected(clickedDay.date)
                }
            },
            monthHeader = { month ->
                MonthHeader(month = month, state = state)
            },
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        )
    }

    val events = @Composable {
        val events = todosByDate[selectedDate] ?: emptyList()
        val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

        Column {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Events for ${selectedDate.format(dateFormatter)}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    val intent = Intent(context, AddTodoActivity::class.java)
                    intent.putExtra("selectedDate", selectedDate.toString())
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add a new TODO")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No events for this day.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(events) { event ->
                        EventCard(event)
                    }
                }
            }
        }
    }

    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth(0.5f)) {
                calendar()
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                events()
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            calendar()
            events()
        }
    }
}

@Composable
private fun MonthHeader(month: CalendarMonth, state: CalendarState) {
    val coroutineScope = rememberCoroutineScope()
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek = DayOfWeek.MONDAY) }
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                coroutineScope.launch {
                    state.animateScrollToMonth(month.yearMonth.minusMonths(1))
                }
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = "${month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.yearMonth.year}",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = {
                coroutineScope.launch {
                    state.animateScrollToMonth(month.yearMonth.plusMonths(1))
                }
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            for (dayOfWeek in daysOfWeek) {
                Text(
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun Day(day: CalendarDay, isSelected: Boolean, events: List<Todo>, onClick: (CalendarDay) -> Unit) {
    val isToday = day.date == LocalDate.now()
    val dayTextColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        day.position != DayPosition.MonthDate -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f) // Square cells
            .padding(2.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .clickable {
                if (day.position == DayPosition.MonthDate) {
                    onClick(day)
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = dayTextColor,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (events.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    events.take(4).forEach { _ ->
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .padding(horizontal = 1.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: Todo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Event,
                contentDescription = "Event Icon",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (event.rewardCoins > 0) {
                    Text(
                        text = "${event.rewardCoins} coins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
