package com.nobadhabbits.cornytask.features.mood_tracking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nobadhabbits.cornytask.features.mood_tracking.data.MoodRecord
import com.nobadhabbits.cornytask.features.mood_tracking.data.TimeOfDay
import io.github.dautovicharis.charts.LineChart
import io.github.dautovicharis.charts.model.toChartDataSet
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

private enum class MoodRangeMode { WEEK, MONTH, LIST }

@Composable
fun MoodScreen(
    moodRecords: List<MoodRecord>,
    modifier: Modifier = Modifier,
    zoneId: ZoneId = ZoneId.systemDefault(),
    weekStartsOn: DayOfWeek = DayOfWeek.MONDAY
) {
    var mode by rememberSaveable { mutableStateOf(MoodRangeMode.WEEK) }

    var anchorDate by rememberSaveable {
        mutableStateOf(LocalDate.now(zoneId))
    }

    val sortedRecords = remember(moodRecords) {
        moodRecords.sortedByDescending { it.timestamp.time }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Text(
            text = "Mood Tracking",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(12.dp))

        MoodRangeHeader(
            mode = mode,
            anchorDate = anchorDate,
            weekStartsOn = weekStartsOn,
            onModeChange = { mode = it },
            onPrev = {
                anchorDate = when (mode) {
                    MoodRangeMode.WEEK -> anchorDate.minusWeeks(1)
                    MoodRangeMode.MONTH -> anchorDate.minusMonths(1)
                    MoodRangeMode.LIST -> anchorDate // no-op
                }
            },
            onNext = {
                anchorDate = when (mode) {
                    MoodRangeMode.WEEK -> anchorDate.plusWeeks(1)
                    MoodRangeMode.MONTH -> anchorDate.plusMonths(1)
                    MoodRangeMode.LIST -> anchorDate // no-op
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        when (mode) {
            MoodRangeMode.WEEK, MoodRangeMode.MONTH -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                    ) {
                        MoodChart(
                            moodRecords = moodRecords,
                            mode = mode,
                            anchorDate = anchorDate,
                            zoneId = zoneId,
                            weekStartsOn = weekStartsOn
                        )
                    }
            }

            MoodRangeMode.LIST -> {
                    MoodList(
                        moodRecords = sortedRecords,
                        modifier = Modifier.fillMaxSize()
                    )
            }
        }

        Spacer(Modifier.height(8.dp))
        MoodLegendHint(mode)
    }
}

@Composable
private fun MoodRangeHeader(
    mode: MoodRangeMode,
    anchorDate: LocalDate,
    weekStartsOn: DayOfWeek,
    onModeChange: (MoodRangeMode) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: mode toggle
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = mode == MoodRangeMode.WEEK,
                onClick = { onModeChange(MoodRangeMode.WEEK) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
            ) { Text("Week") }

            SegmentedButton(
                selected = mode == MoodRangeMode.MONTH,
                onClick = { onModeChange(MoodRangeMode.MONTH) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
            ) { Text("Month") }

            SegmentedButton(
                selected = mode == MoodRangeMode.LIST,
                onClick = { onModeChange(MoodRangeMode.LIST) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
            ) { Text("List") }
        }

        // Row 2: navigation (hide arrows in LIST)
        if (mode != MoodRangeMode.LIST) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous")
                }

                Text(
                    text = when (mode) {
                        MoodRangeMode.WEEK -> formatWeekTitle(anchorDate, weekStartsOn)
                        MoodRangeMode.MONTH -> formatMonthTitle(anchorDate)
                        MoodRangeMode.LIST -> ""
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next")
                }
            }
        }
    }
}

@Composable
private fun MoodChart(
    moodRecords: List<MoodRecord>,
    mode: MoodRangeMode,
    anchorDate: LocalDate,
    zoneId: ZoneId,
    weekStartsOn: DayOfWeek
) {
    when (mode) {
        MoodRangeMode.WEEK -> {
            val week = remember(moodRecords, anchorDate, zoneId, weekStartsOn) {
                buildWeekChartWithTimeSlots(moodRecords, anchorDate, zoneId, weekStartsOn)
            }

            if (week.values.all { it == 0f }) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No mood data for this week")
                }
                return
            }

            val dataSet = week.values.toChartDataSet(
                title = week.title,
                labels = week.labels
            )

            LineChart(dataSet = dataSet)
        }

        MoodRangeMode.MONTH -> {
            val month = remember(moodRecords, anchorDate, zoneId) {
                buildMonthChart(moodRecords, anchorDate, zoneId)
            }

            if (month.values.all { it == null }) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No mood data for this month")
                }
                return
            }

            val values = month.values.map { (it ?: 0.0).toFloat() }
            val dataSet = values.toChartDataSet(
                title = month.title,
                labels = month.labels
            )

            LineChart(dataSet = dataSet)
        }

        MoodRangeMode.LIST -> Unit
    }
}

@Composable
private fun MoodList(
    moodRecords: List<MoodRecord>,
    modifier: Modifier = Modifier
) {
    if (moodRecords.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("No mood records yet")
        }
        return
    }

    val dateFormat = remember { SimpleDateFormat("EEE, d MMM yyyy • HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(moodRecords, key = { it.id.ifBlank { it.timestamp.time.toString() } }) { record ->
            MoodRecordRow(
                record = record,
                dateFormat = dateFormat,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun MoodRecordRow(
    record: MoodRecord,
    dateFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    val score = record.moodScore
    val emoji = moodEmoji(score)
    val label = moodLabel(score)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.Center
            )

            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${dateFormat.format(record.timestamp)} • ${record.timeOfDay.name}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            AssistChip(
                onClick = { /* no-op */ },
                label = { Text(score.toString()) }
            )
        }
    }
}

private data class WeekSlotChartModel(
    val title: String,
    val labels: List<String>, // e.g. "Mon AM", "Mon PM", ...
    val values: List<Float>   // same size as labels
)

/**
 * Weekly chart with time-of-day points:
 * Categories = 7 days * N timeOfDay slots.
 * Each slot shows the average for that (day, timeOfDay) if multiple records exist.
 * Missing slots are 0f.
 */
private fun buildWeekChartWithTimeSlots(
    moodRecords: List<MoodRecord>,
    anchorDate: LocalDate,
    zoneId: ZoneId,
    weekStartsOn: DayOfWeek
): WeekSlotChartModel {
    val weekStart = startOfWeek(anchorDate, weekStartsOn)
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    val slots = TimeOfDay.values().toList()

    val grouped = moodRecords
        .asSequence()
        .map { record ->
            val date = record.timestamp.toInstant().atZone(zoneId).toLocalDate()
            Triple(date, record.timeOfDay, record.moodScore)
        }
        .filter { (date, _, _) -> !date.isBefore(weekStart) && !date.isAfter(weekStart.plusDays(6)) }
        .groupBy({ it.first to it.second }, { it.third })

    val labels = mutableListOf<String>()
    val values = mutableListOf<Float>()

    days.forEach { day ->
        val dayLabel = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

        slots.forEach { tod ->
            val todLabel = shortTimeOfDayLabel(tod)
            labels += "$dayLabel $todLabel"

            val scores = grouped[day to tod].orEmpty()
            values += if (scores.isEmpty()) 0f else scores.average().toFloat()
        }
    }

    return WeekSlotChartModel(
        title = "Mood (by time of day)",
        labels = labels,
        values = values
    )
}

private data class ChartModel(
    val title: String,
    val labels: List<String>,
    val values: List<Double?> // null = no data that day
)

private fun buildMonthChart(
    moodRecords: List<MoodRecord>,
    anchorDate: LocalDate,
    zoneId: ZoneId
): ChartModel {
    val monthStart = anchorDate.withDayOfMonth(1)
    val monthEnd = anchorDate.withDayOfMonth(anchorDate.lengthOfMonth())
    val days = generateSequence(monthStart) { d ->
        val next = d.plusDays(1)
        if (next.isAfter(monthEnd)) null else next
    }.toList()

    val avgByDay = moodRecords
        .asSequence()
        .map { it.toLocalDate(zoneId) to it.moodScore }
        .filter { (date, _) -> !date.isBefore(monthStart) && !date.isAfter(monthEnd) }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, scores) -> scores.average() }

    val labels = days.map { it.dayOfMonth.toString() }
    val values = days.map { day -> avgByDay[day] }

    return ChartModel(
        title = "Daily Mood Average",
        labels = labels,
        values = values
    )
}

private fun MoodRecord.toLocalDate(zoneId: ZoneId): LocalDate {
    return timestamp.toInstant().atZone(zoneId).toLocalDate()
}

private fun startOfWeek(date: LocalDate, weekStartsOn: DayOfWeek): LocalDate {
    var d = date
    while (d.dayOfWeek != weekStartsOn) d = d.minusDays(1)
    return d
}

private fun formatWeekTitle(anchorDate: LocalDate, weekStartsOn: DayOfWeek): String {
    val start = startOfWeek(anchorDate, weekStartsOn)
    val end = start.plusDays(6)
    val startLabel = "${start.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${start.dayOfMonth}"
    val endLabel = "${end.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${end.dayOfMonth}"
    return "$startLabel – $endLabel"
}

private fun formatMonthTitle(anchorDate: LocalDate): String {
    val month = anchorDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return "$month ${anchorDate.year}"
}

private fun shortTimeOfDayLabel(tod: TimeOfDay): String {
    return when (tod.name.lowercase(Locale.getDefault())) {
        "morning" -> "AM"
        "afternoon" -> "PM"
        "evening" -> "Eve"
        "night" -> "N"
        else -> tod.name.take(3)
    }
}

private fun moodEmoji(score: Int): String = when {
    score <= -3 -> "😣"
    score == -2 -> "😟"
    score == -1 -> "🙁"
    score == 0 -> "😐"
    score == 1 -> "🙂"
    score == 2 -> "😄"
    else -> "🤯"
}

private fun moodLabel(score: Int): String = when {
    score <= -3 -> "Very distressed"
    score == -2 -> "Low"
    score == -1 -> "Off"
    score == 0 -> "Normal"
    score == 1 -> "Good"
    score == 2 -> "Great"
    else -> "Manic"
}

@Composable
private fun MoodLegendHint(mode: MoodRangeMode) {
    val text = when (mode) {
        MoodRangeMode.WEEK -> "Shows mood points per day and time of day."
        MoodRangeMode.MONTH -> "Shows the average mood score for each day of the month."
        MoodRangeMode.LIST -> "Shows all mood entries in reverse chronological order."
    }
    Text(text, style = MaterialTheme.typography.bodySmall)
}