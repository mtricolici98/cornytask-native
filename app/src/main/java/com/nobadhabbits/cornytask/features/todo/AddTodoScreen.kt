package com.nobadhabbits.cornytask.features.todo

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    var showDueDateOptions by remember { mutableStateOf(false) }
    val dueDateOptions = listOf("In 1 hour", "In 4 hours", "Tomorrow", "In 2 days", "Custom")

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
        viewModel.dueDate?.let { cal.time = it }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = cal.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            date = Date(it)
                        }
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) { Text("OK") }
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
        viewModel.dueDate?.let { cal.time = it }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE)
        )
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        val resultCal = Calendar.getInstance()
                        date?.let { resultCal.time = it }
                        resultCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        resultCal.set(Calendar.MINUTE, timePickerState.minute)
                        viewModel.onDueDateChanged(resultCal.time)
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                Button(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timePickerState, modifier = Modifier.fillMaxWidth().padding(12.dp))
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

            ExposedDropdownMenuBox(
                expanded = showDueDateOptions,
                onExpandedChange = { showDueDateOptions = !showDueDateOptions }
            ) {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    value = viewModel.dueDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Due Date") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDueDateOptions) },
                )
                ExposedDropdownMenu(
                    expanded = showDueDateOptions,
                    onDismissRequest = { showDueDateOptions = false },
                ) {
                    dueDateOptions.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                val cal = Calendar.getInstance()
                                when (selectionOption) {
                                    "In 1 hour" -> cal.add(Calendar.HOUR_OF_DAY, 1)
                                    "In 4 hours" -> cal.add(Calendar.HOUR_OF_DAY, 4)
                                    "Tomorrow" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                                    "In 2 days" -> cal.add(Calendar.DAY_OF_YEAR, 2)
                                    "Custom" -> {
                                        showDatePicker = true
                                        showDueDateOptions = false
                                        return@DropdownMenuItem
                                    }
                                }
                                viewModel.onDueDateChanged(cal.time)
                                showDueDateOptions = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
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
