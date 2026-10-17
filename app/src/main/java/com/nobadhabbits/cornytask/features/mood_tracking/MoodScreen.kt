package com.nobadhabbits.cornytask.features.mood_tracking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
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
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

private enum class MoodView { PERIOD, LIST }
private enum class MoodPeriod(val label: String) { DAY("Day"), WEEK("Week"), MONTH("Month"), YEAR("Year") }
private enum class DaySlot(val shortLabel: String) { AM("AM"), PM("PM"), EVE("Eve") }

@Composable
fun MoodScreen(
    moodRecords: List<MoodRecord>,
    modifier: Modifier = Modifier,
    zoneId: ZoneId = ZoneId.systemDefault(),
    weekStartsOn: DayOfWeek = DayOfWeek.MONDAY
) {
    var view by rememberSaveable { mutableStateOf(MoodView.PERIOD) }
    var period by rememberSaveable { mutableStateOf(MoodPeriod.WEEK) }

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
            view = view,
            period = period,
            anchorDate = anchorDate,
            weekStartsOn = weekStartsOn,
            onPeriodSelected = {
                period = it
                view = MoodView.PERIOD
            },
            onListSelected = { view = MoodView.LIST },
            onPrev = {
                anchorDate = when (period) {
                    MoodPeriod.DAY -> anchorDate.minusDays(1)
                    MoodPeriod.WEEK -> anchorDate.minusWeeks(1)
                    MoodPeriod.MONTH -> anchorDate.minusMonths(1)
                    MoodPeriod.YEAR -> anchorDate.minusYears(1)
                }
            },
            onNext = {
                anchorDate = when (period) {
                    MoodPeriod.DAY -> anchorDate.plusDays(1)
                    MoodPeriod.WEEK -> anchorDate.plusWeeks(1)
                    MoodPeriod.MONTH -> anchorDate.plusMonths(1)
                    MoodPeriod.YEAR -> anchorDate.plusYears(1)
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(2.dp)
        ) {
            if (view == MoodView.LIST) {
                MoodList(
                    moodRecords = sortedRecords,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val model = remember(moodRecords, period, anchorDate, zoneId, weekStartsOn) {
                    when (period) {
                        MoodPeriod.DAY -> buildDayChart(moodRecords, anchorDate, zoneId)
                        MoodPeriod.WEEK -> buildWeekProgressionBySlots(moodRecords, anchorDate, zoneId, weekStartsOn)
                        MoodPeriod.MONTH -> buildMonthDailyChart(moodRecords, anchorDate, zoneId)
                        MoodPeriod.YEAR -> buildYearMonthlyChart(moodRecords, anchorDate, zoneId)
                    }
                }
                MoodChart(
                    model = model,
                    emptyText = "No mood data for this ${period.label.lowercase()}"
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        MoodLegendHint(view, period)
    }
}

@Composable
private fun MoodChart(model: MoodChartModel, emptyText: String) {
    if (model.points.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText)
        }
        return
    }

    MoodLineChart(points = model.points)
}

@Composable
private fun MoodRangeHeader(
    view: MoodView,
    period: MoodPeriod,
    anchorDate: LocalDate,
    weekStartsOn: DayOfWeek,
    onPeriodSelected: (MoodPeriod) -> Unit,
    onListSelected: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    var periodMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = view == MoodView.PERIOD,
                onClick = { periodMenuOpen = true },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Box {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(period.label)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose period")
                    }
                    DropdownMenu(
                        expanded = periodMenuOpen,
                        onDismissRequest = { periodMenuOpen = false }
                    ) {
                        MoodPeriod.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    periodMenuOpen = false
                                    onPeriodSelected(option)
                                },
                                trailingIcon = if (option == period) {
                                    { Icon(Icons.Filled.Check, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }
            }

            SegmentedButton(
                selected = view == MoodView.LIST,
                onClick = onListSelected,
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("List") }
        }

        if (view == MoodView.PERIOD) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous")
                }

                Text(
                    text = when (period) {
                        MoodPeriod.DAY -> formatDayTitle(anchorDate)
                        MoodPeriod.WEEK -> formatWeekTitle(anchorDate, weekStartsOn)
                        MoodPeriod.MONTH -> formatMonthTitle(anchorDate)
                        MoodPeriod.YEAR -> anchorDate.year.toString()
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

private fun formatDayTitle(date: LocalDate): String {
    val day = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return "$day, $month ${date.dayOfMonth}"
}

private data class MoodChartModel(val points: List<ChartPoint>)

private data class ZonedMood(val time: ZonedDateTime, val score: Int, val comment: String)

private fun List<MoodRecord>.atZone(zoneId: ZoneId): List<ZonedMood> = map {
    ZonedMood(it.timestamp.toInstant().atZone(zoneId), it.moodScore, it.comment.trim())
}

/** Averages the scores of [this] group and collects its comments (oldest first). */
private fun List<ZonedMood>.toPoint(
    label: String,
    title: String = label,
    noteFormat: (ZonedMood) -> String = { it.comment }
): ChartPoint = ChartPoint(
    label = label,
    value = map { it.score }.average().toFloat(),
    notes = sortedBy { it.time }.filter { it.comment.isNotEmpty() }.map(noteFormat),
    title = title
)

/** Day = every entry of the day as its own point. */
private fun buildDayChart(
    moodRecords: List<MoodRecord>,
    anchorDate: LocalDate,
    zoneId: ZoneId
): MoodChartModel {
    val points = moodRecords.atZone(zoneId)
        .filter { it.time.toLocalDate() == anchorDate }
        .sortedBy { it.time }
        .map { listOf(it).toPoint(label = it.time.toLocalTime().toString().substring(0, 5)) } // HH:mm

    return MoodChartModel(points)
}

/**
 * WEEK progression:
 * 7 days × 3 slots inferred from timestamp:
 * - AM: 05:00–11:59
 * - PM: 12:00–17:59
 * - Eve: 18:00–04:59 (wrap)
 *
 * Each slot = average moodScore of all records in that slot.
 */
private fun buildWeekProgressionBySlots(
    moodRecords: List<MoodRecord>,
    anchorDate: LocalDate,
    zoneId: ZoneId,
    weekStartsOn: DayOfWeek
): MoodChartModel {
    val weekStart = startOfWeek(anchorDate, weekStartsOn)
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    val weekEnd = weekStart.plusDays(6)

    val grouped = moodRecords.atZone(zoneId)
        .filter { val d = it.time.toLocalDate(); !d.isBefore(weekStart) && !d.isAfter(weekEnd) }
        .groupBy { it.time.toLocalDate() to slotFromHour(it.time.hour) }

    val points = days.flatMap { day ->
        val dayLabel = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        DaySlot.entries.mapNotNull { slot ->
            grouped[day to slot]?.toPoint(label = "$dayLabel ${slot.shortLabel}")
        }
    }

    return MoodChartModel(points)
}

/** Month = daily averages. */
private fun buildMonthDailyChart(
    moodRecords: List<MoodRecord>,
    anchorDate: LocalDate,
    zoneId: ZoneId
): MoodChartModel {
    val points = moodRecords.atZone(zoneId)
        .filter { it.time.year == anchorDate.year && it.time.month == anchorDate.month }
        .groupBy { it.time.toLocalDate() }
        .toSortedMap()
        .map { (date, entries) ->
            entries.toPoint(label = date.dayOfMonth.toString(), title = formatDayTitle(date))
        }

    return MoodChartModel(points)
}

/** Year = monthly averages (only months that have entries). */
private fun buildYearMonthlyChart(
    moodRecords: List<MoodRecord>,
    anchorDate: LocalDate,
    zoneId: ZoneId
): MoodChartModel {
    val points = moodRecords.atZone(zoneId)
        .filter { it.time.year == anchorDate.year }
        .groupBy { it.time.month }
        .toSortedMap()
        .map { (month, entries) ->
            entries.toPoint(
                label = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                title = month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                // A month can have many comments, so say which day each one is from
                noteFormat = { "${it.time.dayOfMonth}: ${it.comment}" }
            )
        }

    return MoodChartModel(points)
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
                if (record.comment.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = record.comment,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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

internal fun moodEmoji(score: Int): String = when {
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
private fun MoodLegendHint(view: MoodView, period: MoodPeriod) {
    val text = if (view == MoodView.LIST) {
        "List view shows all mood entries."
    } else when (period) {
        MoodPeriod.DAY -> "Shows progression throughout a day."
        MoodPeriod.WEEK -> "Week view shows AM/PM/Eve mood progression (inferred from timestamps)."
        MoodPeriod.MONTH -> "Month view shows daily averages."
        MoodPeriod.YEAR -> "Year view shows monthly averages."
    } + " Tap a point to see comments (ringed points have some)."
    Text(text, style = MaterialTheme.typography.bodySmall)
}
