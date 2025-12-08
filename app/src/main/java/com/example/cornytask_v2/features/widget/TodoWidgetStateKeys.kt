package com.example.cornytask_v2.features.widget

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object TodoWidgetStateKeys {
    val userCoinsKey = stringPreferencesKey("user_coins")
    val todoCountKey = intPreferencesKey("todo_count")

    fun todoIdKey(index: Int): Preferences.Key<String> = stringPreferencesKey("todo_${index}_id")
    fun todoTitleKey(index: Int): Preferences.Key<String> = stringPreferencesKey("todo_${index}_title")
    fun todoCompletedKey(index: Int): Preferences.Key<Boolean> = booleanPreferencesKey("todo_${index}_completed")
    fun todoRewardKey(index: Int): Preferences.Key<Int> = intPreferencesKey("todo_${index}_reward")
}
