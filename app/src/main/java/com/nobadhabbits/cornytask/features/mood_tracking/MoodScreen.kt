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
import io.github.dautovicharis.charts.LineChart
import io.github.dautovicharis.charts.model.toChartDataSet
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

private enum class MoodRangeMode { WEEK, MONTH, LIST }
private enum class DaySlot(val shortLabel: String) { AM("AM"), PM("PM"), EVE("Eve") }

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
                    MoodRangeMode.LIST -> anchorDate
                }
            },
            onNext = {
                anchorDate = when (mode) {
                    MoodRangeMode.WEEK -> anchorDate.plusWeeks(1)
                    MoodRangeMode.MONTH -> anchorDate.plusMonths(1)
                    MoodRangeMode.LIST -> anchorDate
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        when (mode) {
            MoodRangeMode.WEEK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                ) {
                    WeekMoodProgressionChart(
                        moodRecords = moodRecords,
                        anchorDate = anchorDate,
                        zoneId = zoneId,
                        weekStartsOn = weekStartsOn
                    )
                }
            }

            MoodRangeMode.MONTH -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                ) {
                    MonthMoodChart(
                        moodRecords = moodRecords,
                        anchorDate = anchorDate,
                        zoneId = zoneId
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

/**
 * WEEK progression:
 * 7 days × 3 slots inferred from timestamp:
 * - AM: 05:00–11:59
 * - PM: 12:00–17:59
 * - Eve: 18:00–04:59 (wrap)
 *
 * Each slot = average moodScore of all records in that slot.
 *
 * IMPORTANT: 0 is a valid mood score, so we use hasData instead of checking all values == 0f.
 */
@Composable
private fun WeekMoodProgressionChart(
    moodRecords: List<MoodRecord>,
    anchorDate: LocalDate,
    zoneId: ZoneId,
    weekStartsOn: DayOfWeek
) {
    val model = remember(moodRecords, anchorDate, zoneId, weekStartsOn) {
        buildWeekProgressionBySlots(moodRecords, anchorDate, zoneId, weekStartsOn)
    }

    if (!model.hasData) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No mood data for this week")
        }
        return
    }

    val dataSet = model.values.toChartDataSet(
        title = model.title,
        labels = model.labels
    )

    LineChart(dataSet = dataSet)
}

/**
 * Month = daily averages.
 * IMPORTANT: 0 is valid, so we use hasData.
 */
@Composable
private fun MonthMoodChart(
    moodRecords: List<MoodRecord>,
    anchorDate: LocalDate,
    zoneId: ZoneId
) {
    val model = remember(moodRecords, anchorDate, zoneId) {
        buildMonthDailyChart(moodRecords, anchorDate, zoneId)
    }

    if (!model.hasData) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No mood data for this month")
        }
        return
    }

    val dataSet = model.values.toChartDataSet(
        title = model.title,
        labels = model.labels
    )

    LineChart(dataSet = dataSet)
}

private data class FloatChartModel(
    val title: String,
    val labels: List<String>,
    val values: List<Float>,
    val hasData: Boolean
)

private fun buildWeekProgressionBySlots(
    moodRecords: List<MoodRecord>,
    anchorDate: LocalDate,
    zoneId: ZoneId,
    weekStartsOn: DayOfWeek
): FloatChartModel {
    val weekStart = startOfWeek(anchorDate, weekStartsOn)
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    val weekEnd = weekStart.plusDays(6)

    val inWeek: List<Pair<ZonedDateTime, Int>> = moodRecords.mapNotNull { record ->
        val zdt = record.timestamp.toInstant().atZone(zoneId)
        val d = zdt.toLocalDate()
        if (d.isBefore(weekStart) || d.isAfter(weekEnd)) null else zdt to record.moodScore
    }

    val grouped = inWeek
        .groupBy(
            keySelector = { (zdt, _) -> zdt.toLocalDate() to slotFromHour(zdt.hour) },
            valueTransform = { it.second }
        )
        .mapValues { (_, scores) -> scores.average().toFloat() }

    val labels = mutableListOf<String>()
    val values = mutableListOf<Float>()

    days.forEach { day ->
        val dayLabel = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        DaySlot.values().forEach { slot ->
            labels += "$dayLabel ${slot.shortLabel}"
            values += grouped[day to slot] ?: 0f
        }
    }

    return FloatChartModel(
        title = "Mood progression",
        labels = labels,
        values = values,
        hasData = inWeek.isNotEmpty()
    )
}

private fun buildMonthDailyChart(
    moodRecords: List<MoodRecord>,
    anchorDate: LocalDate,
    zoneId: ZoneId
): FloatChartModel {
    val monthStart = anchorDate.withDayOfMonth(1)
    val monthEnd = anchorDate.withDayOfMonth(anchorDate.lengthOfMonth())

    val inMonth = moodRecords.asSequence().mapNotNull { record ->
        val date = record.timestamp.toInstant().atZone(zoneId).toLocalDate()
        if (date.isBefore(monthStart) || date.isAfter(monthEnd)) null else date to record.moodScore
    }.toList()

    val byDay: Map<Int, Float> =
        inMonth.groupBy({ (date, _) -> date.dayOfMonth }, { it.second })
            .mapValues { (_, scores) -> scores.average().toFloat() }

    val daysInMonth = anchorDate.lengthOfMonth()
    val labels = (1..daysInMonth).map { it.toString() }
    val values = (1..daysInMonth).map { d -> byDay[d] ?: 0f }

    return FloatChartModel(
        title = "Mood (daily average)",
        labels = labels,
        values = values,
        hasData = inMonth.isNotEmpty()
    )
}

private fun slotFromHour(hour: Int): DaySlot {
    return when (hour) {
        in 5..11 -> DaySlot.AM
        in 12..17 -> DaySlot.PM
        else -> DaySlot.EVE
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
                    text = dateFormat.format(record.timestamp),
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
        MoodRangeMode.WEEK -> "Week view shows AM/PM/Eve mood progression (inferred from timestamps)."
        MoodRangeMode.MONTH -> "Month view shows daily averages."
        MoodRangeMode.LIST -> "List view shows all mood entries."
    }
    Text(text, style = MaterialTheme.typography.bodySmall)
}