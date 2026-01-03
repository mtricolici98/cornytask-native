package com.nobadhabbits.cornytask.features.time_goals

import android.content.Context
import android.content.SharedPreferences

class TimeGoalPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("time_goal_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_ACTIVE_TIME_GOAL_ID = "active_time_goal_id"
        private const val KEY_IS_TIMER_RUNNING = "is_timer_running"
        private const val KEY_START_TIME_MILLIS = "start_time_millis"
        private const val KEY_DURATION_MINUTES = "duration_minutes"
    }

    fun setTimerRunning(isRunning: Boolean) {
        prefs.edit().putBoolean(KEY_IS_TIMER_RUNNING, isRunning).commit()
    }

    fun isTimerRunning(): Boolean {
        return prefs.getBoolean(KEY_IS_TIMER_RUNNING, false)
    }

    fun setActiveTimeGoal(goalId: String, durationMinutes: Long) {
        prefs.edit()
            .putString(KEY_ACTIVE_TIME_GOAL_ID, goalId)
            .putLong(KEY_START_TIME_MILLIS, System.currentTimeMillis())
            .putLong(KEY_DURATION_MINUTES, durationMinutes)
            .commit()
    }

    fun getActiveTimeGoal(): Triple<String?, Long, Long> {
        val goalId = prefs.getString(KEY_ACTIVE_TIME_GOAL_ID, null)
        val startTime = prefs.getLong(KEY_START_TIME_MILLIS, 0)
        val duration = prefs.getLong(KEY_DURATION_MINUTES, 0)
        return Triple(goalId, startTime, duration)
    }

    fun clearActiveTimeGoal() {
        prefs.edit()
            .remove(KEY_ACTIVE_TIME_GOAL_ID)
            .remove(KEY_START_TIME_MILLIS)
            .remove(KEY_DURATION_MINUTES)
            .putBoolean(KEY_IS_TIMER_RUNNING, false)
            .commit()
    }

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
