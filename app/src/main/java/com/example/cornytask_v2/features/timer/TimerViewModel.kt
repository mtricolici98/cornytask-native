package com.example.cornytask_v2.features.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {

    private val _timeMillis = MutableStateFlow(0L)
    val timeMillis: StateFlow<Long> = _timeMillis

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private var initialTime: Long = 0L
    private var timerJob: Job? = null

    fun setInitialTime(millis: Long) {
        initialTime = millis
        if (!_isRunning.value) {
            _timeMillis.value = initialTime
        }
    }

    fun toggle() {
        if (_isRunning.value) {
            pause()
        } else {
            start()
        }
    }

    private fun start() {
        if (_timeMillis.value == 0L && initialTime > 0L) {
            _timeMillis.value = initialTime
        }
        if (_timeMillis.value > 0) {
            _isRunning.value = true
            timerJob = viewModelScope.launch {
                while (_timeMillis.value > 0 && _isRunning.value) {
                    delay(1000)
                    _timeMillis.value -= 1000
                }
                if (_timeMillis.value <= 0) {
                    _isRunning.value = false
                }
            }
        }
    }

    private fun pause() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    fun reset() {
        _isRunning.value = false
        timerJob?.cancel()
        _timeMillis.value = initialTime
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
