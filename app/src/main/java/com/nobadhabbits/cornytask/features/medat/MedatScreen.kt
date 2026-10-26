package com.nobadhabbits.cornytask.features.medat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale


/** What the convert dialog is turning into a time goal or TODO. */
private data class ConvertRequest(
    val items: List<MedatItemState>,
    val title: String,
    val asTimeGoal: Boolean
)

@Composable
fun MedatScreen(viewModel: MedatViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var convertRequest by remember { mutableStateOf<ConvertRequest?>(null) }

    // The countdown scrolls away with the content so small screens still fit everything.
    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Plan") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Calendar") })
        }
        when (selectedTab) {
            0 -> PlanDay(
                state = state,
                onShowDay = viewModel::showDay,
                onShowCurrentDay = viewModel::showCurrentDay,
                onSetDone = viewModel::setDone,
                onPostpone = viewModel::postpone,
                onConvert = { convertRequest = it }
            )
            else -> MedatCalendar(
                state = state,
                onSetDayType = viewModel::setDayType,
                header = { CountdownCard(state) }
            )
        }
    }

    convertRequest?.let { request ->
        ConvertDialog(
            request = request,
            onDismiss = { convertRequest = null },
            onConfirm = { title, minutes, coins ->
                if (request.asTimeGoal) {
                    viewModel.createTimeGoal(request.items, title, minutes, coins)
                } else {
                    viewModel.createTodo(request.items, title, coins)
                }
                convertRequest = null
            }
        )
    }
}

@Composable
private fun CountdownCard(state: MedatUiState) {
    val today = LocalDate.now()
    val daysToTest = ChronoUnit.DAYS.between(today, MEDAT_DATE)
    val dateText = MEDAT_DATE.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = when {
                    daysToTest > 1 -> "$daysToTest days to go"
                    daysToTest == 1L -> "MedAT is tomorrow!"
                    daysToTest == 0L -> "MedAT is today. Viel Erfolg!"
                    else -> "MedAT is done"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "MedAT on $dateText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (state.totalCount == 0) 0f else state.doneCount.toFloat() / state.totalCount },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            val planText = if (state.currentDay == null) {
                "Whole plan done! 🎉"
            } else {
                "Plan day ${state.currentDay} · ${state.daysLeft} plan days left"
            }
            Text(
                text = planText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (state.currentDay != null) {
                val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                val target = MEDAT_TARGET_DATE.format(formatter)
                val finish = state.projectedFinish
                // Always encouraging: either "on track" or what pace gets you there.
                val paceText = when {
                    finish != null && !finish.isAfter(MEDAT_TARGET_DATE) -> {
                        val spare = state.studyDaysUntilTarget - state.daysLeft
                        "On track ✨ Last plan day on ${finish.format(formatter)}, " +
                            "$spare spare study days before your target ($target)"
                    }
                    state.studyDaysUntilTarget > 0 -> {
                        val perDay = state.daysLeft.toDouble() / state.studyDaysUntilTarget
                        "Target $target: ${state.studyDaysUntilTarget} study days for ${state.daysLeft} plan days, " +
                            "≈ ${String.format(Locale.getDefault(), "%.1f", perDay)} plan days per study day 💪"
                    }
                    else -> "Final stretch! Keep going, you've got this 💪"
                }
                Text(
                    text = paceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun PlanDay(
    state: MedatUiState,
    onShowDay: (Int) -> Unit,
    onShowCurrentDay: () -> Unit,
    onSetDone: (MedatItemState, Boolean) -> Unit,
    onPostpone: (MedatItemState) -> Unit,
    onConvert: (ConvertRequest) -> Unit
) {
    val day = state.shownDay
    val items = state.shownItems
    val isSuperDay = day in state.superDays
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item(key = "countdown") { CountdownCard(state) }
        item(key = "day-header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onShowDay(day - 1) }, enabled = day > 1) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isSuperDay) "Tag $day · Superlerntag" else "Tag $day",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    val status = when {
                        day > MEDAT_PLAN_DAYS -> "Catch-up day"
                        items.isNotEmpty() && items.all { it.isDone } -> "Done 🎉"
                        day == state.currentDay -> "Current day · ${items.filter { !it.isDone }.sumOf { it.item.minutes }} min left"
                        else -> "Coming up"
                    }
                    Text(status, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { onShowDay(day + 1) }, enabled = day < state.lastDay) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
                }
            }
        }
        if (state.currentDay != null && day != state.currentDay) {
            item(key = "back-to-current") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = onShowCurrentDay) {
                        Text("Back to Tag ${state.currentDay}")
                    }
                }
            }
        }
        if (items.isEmpty()) {
            item(key = "empty") {
                Text(
                    "Nothing left on this day. Everything was moved on.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        MedatBlock.entries.forEach { block ->
            val blockItems = items.filter { it.item.block == block }
            if (blockItems.isEmpty()) return@forEach
            item(key = "header-$block") {
                BlockHeader(
                    block = block,
                    items = blockItems,
                    day = day,
                    onConvert = onConvert
                )
            }
            items(blockItems, key = { it.item.id }) { item ->
                PlanItemRow(
                    item = item,
                    onSetDone = { onSetDone(item, it) },
                    onPostpone = { onPostpone(item) },
                    onConvert = onConvert
                )
            }
        }
        if (isSuperDay) {
            item(key = "med-story") {
                TextButton(
                    onClick = { uriHandler.openUri("https://studymed.at/q/ms$day") },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.AutoStories, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Your reward: today's Med-Story")
                }
            }
        }
    }
}

@Composable
private fun BlockHeader(
    block: MedatBlock,
    items: List<MedatItemState>,
    day: Int,
    onConvert: (ConvertRequest) -> Unit
) {
    // Converting a whole block only covers what's still open and not linked yet.
    val open = items.filter { !it.isDone && it.timeGoalId == null && it.todoId == null }
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${block.label} · ${items.sumOf { it.item.minutes }} min",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        if (open.isNotEmpty()) {
            val title = "MedAT Tag $day · ${block.label}"
            ItemMenu(
                expanded = menuOpen,
                onToggle = { menuOpen = it },
                postponeLabel = null,
                onPostpone = {},
                onTimeGoal = { onConvert(ConvertRequest(open, title, asTimeGoal = true)) },
                onTodo = { onConvert(ConvertRequest(open, title, asTimeGoal = false)) },
                contentDescription = "Options for ${block.label}"
            )
        }
    }
}

@Composable
private fun PlanItemRow(
    item: MedatItemState,
    onSetDone: (Boolean) -> Unit,
    onPostpone: () -> Unit,
    onConvert: (ConvertRequest) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val linked = item.timeGoalId != null || item.todoId != null

    // Each item gets its own card so neighbouring titles don't run together.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isDone) MaterialTheme.colorScheme.surfaceContainerLow
            else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.isDone, onCheckedChange = onSetDone)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.item.displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val details = buildList {
                        add("${item.item.minutes} min · ${item.item.coins} 🦄")
                        item.movedFromDay?.let { add("moved from Tag $it") }
                    }.joinToString(" · ")
                    Text(
                        details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.timeGoalId != null) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Timelapse,
                            contentDescription = "Has a time goal",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (item.todoId != null) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "Has a TODO",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (!item.isDone) {
                val title = "MedAT: ${item.item.displayTitle}"
                ItemMenu(
                    expanded = menuOpen,
                    onToggle = { menuOpen = it },
                    postponeLabel = "Didn't do it · move to Tag ${item.day + 1}",
                    onPostpone = onPostpone,
                    onTimeGoal = if (linked) null else { { onConvert(ConvertRequest(listOf(item), title, asTimeGoal = true)) } },
                    onTodo = if (linked) null else { { onConvert(ConvertRequest(listOf(item), title, asTimeGoal = false)) } },
                    contentDescription = "Options for ${item.item.displayTitle}"
                )
            }
        }
    }
}

@Composable
private fun ItemMenu(
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    postponeLabel: String?,
    onPostpone: () -> Unit,
    onTimeGoal: (() -> Unit)?,
    onTodo: (() -> Unit)?,
    contentDescription: String
) {
    Column {
        IconButton(onClick = { onToggle(true) }) {
            Icon(Icons.Default.MoreVert, contentDescription = contentDescription)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onToggle(false) }) {
            if (postponeLabel != null) {
                DropdownMenuItem(
                    text = { Text(postponeLabel) },
                    onClick = { onToggle(false); onPostpone() }
                )
            }
            if (onTimeGoal != null) {
                DropdownMenuItem(
                    text = { Text("Make a time goal") },
                    leadingIcon = { Icon(Icons.Default.Timelapse, contentDescription = null) },
                    onClick = { onToggle(false); onTimeGoal() }
                )
            }
            if (onTodo != null) {
                DropdownMenuItem(
                    text = { Text("Make a TODO") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    onClick = { onToggle(false); onTodo() }
                )
            }
        }
    }
}

@Composable
private fun ConvertDialog(
    request: ConvertRequest,
    onDismiss: () -> Unit,
    onConfirm: (title: String, minutes: Long, coins: Int) -> Unit
) {
    var title by remember { mutableStateOf(request.title) }
    var minutes by remember { mutableStateOf(request.items.sumOf { it.item.minutes }.toString()) }
    var coins by remember { mutableStateOf(request.items.sumOf { it.item.coins }.toString()) }
    val minutesValue = minutes.toLongOrNull()
    val coinsValue = coins.toIntOrNull()
    val valid = title.isNotBlank() && coinsValue != null && coinsValue >= 0 &&
        (!request.asTimeGoal || (minutesValue != null && minutesValue > 0))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (request.asTimeGoal) "New time goal" else "New TODO") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (request.items.size > 1) {
                    Text(
                        "Covers ${request.items.size} items. Finishing it ticks them all off.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text("Finishing it ticks this item off.", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (request.asTimeGoal) {
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it.filter(Char::isDigit) },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = coins,
                    onValueChange = { coins = it.filter(Char::isDigit) },
                    label = { Text("Reward coins") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title.trim(), minutesValue ?: 0, coinsValue ?: 0) },
                enabled = valid
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
