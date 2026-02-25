package com.nobadhabbits.cornytask.features.mood_tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nobadhabbits.cornytask.features.mood_tracking.data.MoodRecord
import com.nobadhabbits.cornytask.features.mood_tracking.data.TimeOfDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

class MoodViewModel(
    private val moodTrackingRepository: MoodTrackingRepository
) : ViewModel() {

    private val _moodRecords = MutableStateFlow<List<MoodRecord>>(emptyList())
    val moodRecords: StateFlow<List<MoodRecord>> = _moodRecords

    fun addMoodRecord(date: Date, timeOfDay: TimeOfDay, moodScore: Int) {
        viewModelScope.launch {
            moodTrackingRepository.addMoodRecord(date, timeOfDay, moodScore)
        }
    }

    fun fetchMoodRecords() {
        viewModelScope.launch {
            moodTrackingRepository.getMoodRecords().collect {
                _moodRecords.value = it
            }
        }
    }
}
