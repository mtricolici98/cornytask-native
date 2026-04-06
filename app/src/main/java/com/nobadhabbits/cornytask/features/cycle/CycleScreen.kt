package com.nobadhabbits.cornytask.features.cycle

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kizitonwose.calendar.compose.*
import com.kizitonwose.calendar.core.*
import com.nobadhabbits.cornytask.App
import com.nobadhabbits.cornytask.data.cycle.CycleType
import com.nobadhabbits.cornytask.ui.theme.Purple40
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.core.models.Shape
import java.time.*
import java.time.format.TextStyle
import java.util.*
import androidx.compose.runtime.collectAsState

@Composable
fun CycleScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as App

    val viewModel: CycleViewModel = viewModel(
        factory = CycleViewModelFactory(app.container.cycleRepository)
    )
    val entries by viewModel.entries.collectAsState()
    val predictions by viewModel.predictions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    val currentMonth = YearMonth.now()

    val state = rememberCalendarState(
        startMonth = currentMonth.minusMonths(100),
        endMonth = currentMonth.plusMonths(100),
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = DayOfWeek.MONDAY
    )

    val configuration = LocalConfiguration.current

    val calendar = @Composable {
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                val isPeriod = entries.any {
                    it.date == day.date && it.type == CycleType.PERIOD
                }
                val isSelected = viewModel.selectedDate.collectAsState().value == day.date
                val prediction = predictions[day.date]
                val bg = when {
                    isPeriod -> Color(0xFFE57373) // REAL data highest priority
                    prediction == PredictionType.PREDICTED_PERIOD -> Color(0xFFFFCDD2)
                    prediction == PredictionType.FERTILE -> Color(0xFF81C784)
                    else -> Color.Transparent
                }
                var modifier = Modifier
                    .aspectRatio(1f)
                    .padding(2.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(bg)
                    .clickable {
                        viewModel.select(day.date)
                    };
                if (isSelected) {
                    modifier = modifier.border(2.dp, color = Purple40, shape = CircleShape)
                }
                Box(
                    modifier = modifier,
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        fontWeight = if (day.date == LocalDate.now()) FontWeight.Bold else FontWeight.Normal
                    )
                }
            },
            monthHeader = { month ->
                MonthHeader(month, state)
            }
        )
    }

    val details = @Composable {
        val prediction = predictions[selectedDate]
        val isPeriod = entries.any { it.date == selectedDate }


        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Selected: $selectedDate")

                    Spacer(Modifier.height(8.dp))

                    when {
                        isPeriod -> Text("🌸 Menstruation")
                        prediction == PredictionType.FERTILE -> Text("🌿 Fertile Window")
                        prediction == PredictionType.PREDICTED_PERIOD -> Text("🩸 Predicted Period")
                        else -> Text("No data")
                    }
                }
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = { viewModel.toggle(selectedDate) }
                ) {
                    Text(if (isPeriod) "Remove Period" else "Mark as Period")
                }
            }

        }
    }


    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Row {
            Box(Modifier.weight(1f)) { calendar() }
            Box(Modifier.weight(1f)) { details() }
        }
    } else {
        Column {
            calendar()
            details()
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
