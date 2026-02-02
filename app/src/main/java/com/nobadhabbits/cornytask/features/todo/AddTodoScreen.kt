package com.nobadhabbits.cornytask.features.todo

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoScreen(onNavigateUp: () -> Unit, selectedDate: String?) {
    val context = LocalContext.current
    val viewModel: AddTodoViewModel = viewModel(
        factory = AddTodoViewModelFactory(context.applicationContext as Application)
    )
    val suggestions by viewModel.suggestions.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var date by remember { mutableStateOf<Date?>(null) }

    var tempDate by remember { mutableStateOf<Date?>(null) }
    var tempHour by remember { mutableStateOf(0) }
    var tempMinute by remember { mutableStateOf(0) }

    LaunchedEffect(selectedDate) {
        if (selectedDate != null) {
            val localDate = LocalDate.parse(selectedDate)
            val cal = Calendar.getInstance()
            cal.set(localDate.year, localDate.monthValue - 1, localDate.dayOfMonth)
            viewModel.onDueDateChanged(cal.time)
        }
    }


    if (showDatePicker) {
        val cal = Calendar.getInstance()
        tempDate?.let { cal.time = it } ?: run {
            viewModel.dueDate?.let { cal.time = it }
        }

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = cal.timeInMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // Keep the chosen date in tempDate; time comes next
                            tempDate = Date(millis)
                        }
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) { Text("Next") }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance()
        // If tempDate exists use it; else seed from dueDate/now
        (tempDate ?: viewModel.dueDate)?.let { cal.time = it }

        val timePickerState = rememberTimePickerState(
            initialHour = tempHour.takeIf { it in 0..23 } ?: cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = tempMinute.takeIf { it in 0..59 } ?: cal.get(Calendar.MINUTE)
        )

        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        // Save latest time into temp vars (optional but nice)
                        tempHour = timePickerState.hour
                        tempMinute = timePickerState.minute

                        val resultCal = Calendar.getInstance()
                        val baseDate = tempDate ?: viewModel.dueDate ?: Date()
                        resultCal.time = baseDate
                        resultCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        resultCal.set(Calendar.MINUTE, timePickerState.minute)
                        resultCal.set(Calendar.SECOND, 0)
                        resultCal.set(Calendar.MILLISECOND, 0)

                        viewModel.onDueDateChanged(resultCal.time)
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                // Back + Cancel in the dismiss slot
                Row {
                    Button(
                        onClick = {
                            // store current time so it doesn't "reset" when going back
                            tempHour = timePickerState.hour
                            tempMinute = timePickerState.minute

                            showTimePicker = false
                            showDatePicker = true
                        }
                    ) { Text("Back") }

                    Spacer(Modifier.width(8.dp))

                    Button(onClick = { showTimePicker = false }) { Text("Cancel") }
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(
                    state = timePickerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Add To-do") }) }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            ExposedDropdownMenuBox(expanded = suggestions.isNotEmpty(), onExpandedChange = {}) {
                TextField(
                    value = viewModel.title,
                    onValueChange = { viewModel.onTitleChanged(it) },
                    label = { Text("Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    isError = viewModel.titleError != null,
                    supportingText = { viewModel.titleError?.let { Text(it) } }
                )

                ExposedDropdownMenu(
                    expanded = suggestions.isNotEmpty(),
                    onDismissRequest = { viewModel.clearSuggestions() },
                ) {
                    suggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text("%s %d".format(suggestion.title, suggestion.rewardCoins)) },
                            onClick = {
                                viewModel.onSuggestionTapped(suggestion)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = viewModel.rewardCoins,
                onValueChange = { viewModel.rewardCoins = it },
                label = { Text("Reward in Unicorns") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = viewModel.rewardCoinsError != null,
                supportingText = { viewModel.rewardCoinsError?.let { Text(it) } }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val cal = Calendar.getInstance()
                        val existing = viewModel.dueDate
                        if (existing != null) cal.time = existing

                        tempDate = cal.time
                        tempHour = cal.get(Calendar.HOUR_OF_DAY)
                        tempMinute = cal.get(Calendar.MINUTE)

                        if (existing != null) showTimePicker = true else showDatePicker = true
                    }
            ) {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,          // important: prevents TextField from consuming the click
                    readOnly = true,
                    value = viewModel.dueDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Due Date") },
                    colors = TextFieldDefaults.colors(
                        // make it LOOK enabled even though enabled=false
                        disabledTextColor = TextFieldDefaults.colors().focusedTextColor,
                        disabledLabelColor = TextFieldDefaults.colors().focusedLabelColor,
                        disabledIndicatorColor = TextFieldDefaults.colors().focusedIndicatorColor,
                        disabledContainerColor = TextFieldDefaults.colors().focusedContainerColor
                    )
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.onAddTodo(onSuccess = onNavigateUp) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add To-do")
            }
        }
    }

}
