package com.nobadhabbits.cornytask.features.medat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.nobadhabbits.cornytask.ui.isWideScreen
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.nobadhabbits.cornytask.data.MedatDayType
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * Cheer for a day based on how many items were ticked off. Only ever positive:
 * a day with nothing done just shows no emoji.
 */
private fun cheerEmoji(doneCount: Int): String? = when {
    doneCount <= 0 -> null
    doneCount <= 2 -> "🌱"
    doneCount <= 4 -> "😊"
    doneCount <= 6 -> "💪"
    doneCount <= 8 -> "🔥"
    else -> "🏆"
}

private fun cheerMessage(doneCount: Int): String = when {
    doneCount <= 2 -> "Every bit counts!"
    doneCount <= 4 -> "Nice work!"
    doneCount <= 6 -> "Strong day!"
    doneCount <= 8 -> "On fire!"
    else -> "Champion day!"
}

/** The MedAT calendar: what was studied when, break/simulation days, and where the plan lands next. */
@Composable
fun MedatCalendar(
    state: MedatUiState,
    onSetDayType: (LocalDate, MedatDayType) -> Unit,
    header: @Composable () -> Unit
) {
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    val currentMonth = remember { YearMonth.now() }
    val calendarState = rememberCalendarState(
        startMonth = currentMonth.minusMonths(12),
        endMonth = maxOf(currentMonth, YearMonth.from(MEDAT_DATE)).plusMonths(1),
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = DayOfWeek.MONDAY
    )
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val doneOnDate = state.completedByDate[selectedDate].orEmpty()
    val selectedType = state.dayType(selectedDate)

    fun LazyListScope.calendarSection() {
        item(key = "header") { header() }
        item(key = "calendar") {
            HorizontalCalendar(
                state = calendarState,
                dayContent = { day ->
                    CalendarDayCell(
                        day = day,
                        isSelected = day.date == selectedDate,
                        doneCount = state.completedByDate[day.date]?.size ?: 0,
                        dayType = state.dayType(day.date),
                        projectedDay = state.projectedDays[day.date],
                        onClick = { selectedDate = it }
                    )
                },
                monthHeader = { month -> MonthHeader(month, calendarState) }
            )
        }
        item(key = "legend") {
            Text(
                "🌴 break · 📝 simulation · 🎯 target · T12 = plan day for that date",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    fun LazyListScope.detailSection() {
        item(key = "selected-date") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = when (selectedDate) {
                        MEDAT_DATE -> "MedAT day 🎓"
                        MEDAT_TARGET_DATE -> "${selectedDate.format(dateFormatter)} · target 🎯"
                        else -> selectedDate.format(dateFormatter)
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayTypeChip("Study", MedatDayType.STUDY, selectedType) { onSetDayType(selectedDate, it) }
                    DayTypeChip("🌴 Break", MedatDayType.BREAK, selectedType) { onSetDayType(selectedDate, it) }
                    DayTypeChip("📝 Simulation", MedatDayType.SIMULATION, selectedType) { onSetDayType(selectedDate, it) }
                }
                val planLine = when (selectedType) {
                    MedatDayType.BREAK -> "Break day. Enjoy it, rest helps it all stick 🌴"
                    MedatDayType.SIMULATION -> "Simulation day 📝 A full test run instead of the plan."
                    MedatDayType.STUDY -> state.projectedDays[selectedDate]?.let { "Up next on this day: Tag $it" }
                }
                if (planLine != null) {
                    Text(planLine, style = MaterialTheme.typography.bodyMedium)
                }
                if (doneOnDate.isNotEmpty()) {
                    Text(
                        "${cheerEmoji(doneOnDate.size)} ${cheerMessage(doneOnDate.size)} " +
                            "${doneOnDate.size} items · ${doneOnDate.sumOf { it.item.minutes }} min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(doneOnDate.sortedBy { it.item.day }, key = { it.item.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(item.item.displayTitle, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Tag ${item.day} · ${item.item.minutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    val listPadding = PaddingValues(bottom = 16.dp)
    if (isWideScreen()) {
        // Tablets and landscape: calendar left, the selected day right, like the other calendars.
        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = listPadding) { calendarSection() }
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = listPadding) { detailSection() }
        }
    } else {
        // One scrolling list, so the day's details are reachable below the calendar.
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = listPadding) {
            calendarSection()
            detailSection()
        }
    }
}

@Composable
private fun DayTypeChip(
    label: String,
    type: MedatDayType,
    selectedType: MedatDayType,
    onSelect: (MedatDayType) -> Unit
) {
    FilterChip(
        selected = type == selectedType,
        onClick = { onSelect(type) },
        label = { Text(label) }
    )
}

@Composable
private fun MonthHeader(month: CalendarMonth, state: CalendarState) {
    val coroutineScope = rememberCoroutineScope()
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek = DayOfWeek.MONDAY) }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                coroutineScope.launch { state.animateScrollToMonth(month.yearMonth.minusMonths(1)) }
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = "${month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.yearMonth.year}",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = {
                coroutineScope.launch { state.animateScrollToMonth(month.yearMonth.plusMonths(1)) }
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
            }
        }
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
private fun CalendarDayCell(
    day: CalendarDay,
    isSelected: Boolean,
    doneCount: Int,
    dayType: MedatDayType,
    projectedDay: Int?,
    onClick: (LocalDate) -> Unit
) {
    val inMonth = day.position == DayPosition.MonthDate
    val isTestDay = day.date == MEDAT_DATE
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isTestDay -> MaterialTheme.colorScheme.tertiaryContainer
        doneCount > 0 -> MaterialTheme.colorScheme.primaryContainer
        dayType != MedatDayType.STUDY -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        !inMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        isTestDay -> MaterialTheme.colorScheme.onTertiaryContainer
        doneCount > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
        dayType != MedatDayType.STUDY -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    // One small marker under the date; what was achieved wins over what's planned.
    val marker = when {
        isTestDay -> null
        doneCount > 0 -> cheerEmoji(doneCount)
        dayType == MedatDayType.BREAK -> "🌴"
        dayType == MedatDayType.SIMULATION -> "📝"
        day.date == MEDAT_TARGET_DATE -> "🎯"
        projectedDay != null -> "T$projectedDay"
        else -> null
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (inMonth) background else Color.Transparent)
            .clickable(enabled = inMonth) { onClick(day.date) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isTestDay) "MedAT" else day.date.dayOfMonth.toString(),
                color = textColor,
                style = if (isTestDay) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
                fontWeight = if (day.date == LocalDate.now() || isTestDay) FontWeight.Bold else FontWeight.Normal
            )
            if (marker != null && inMonth) {
                Text(
                    text = marker,
                    color = textColor.copy(alpha = 0.8f),
                    style = if (projectedDay != null && marker == "T$projectedDay") MaterialTheme.typography.labelSmall
                    else MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
