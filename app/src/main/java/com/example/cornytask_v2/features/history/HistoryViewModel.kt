package com.example.cornytask_v2.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cornytask_v2.data.History
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Using java.util.Date as the key, normalized to the start of the day
data class WeeklyHistory(val weekOfYear: Int, val year: Int, val days: Map<Date, List<History>>)

class HistoryViewModel : ViewModel() {

    private val repository = HistoryRepository()

    val weeklyHistory: StateFlow<List<WeeklyHistory>> = repository.getHistoryFlow()
        .map { history ->
            history
                .filter { it.createdAt != null } // Ensure createdAt is not null
                .groupBy {
                    val cal = Calendar.getInstance(Locale.getDefault())
                    cal.time = it.createdAt!!
                    // Create a unique key for the week and year to group by
                    Pair(cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR))
                }
                .map { (yearWeekPair, historyItemsInWeek) ->
                    val (year, week) = yearWeekPair

                    // Group items within the week by the normalized date (start of day)
                    val daysMap = historyItemsInWeek.groupBy {
                        it.createdAt!!.stripTime()
                    }

                    WeeklyHistory(weekOfYear = week, year = year, days = daysMap)
                }
                .sortedByDescending { it.year * 100 + it.weekOfYear }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

}

// Helper to normalize a Date to the start of the day
fun Date.stripTime(): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
}