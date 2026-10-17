package com.nobadhabbits.cornytask.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nobadhabbits.cornytask.features.user.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel : ViewModel() {

    private val userRepository = UserRepository()

    val coins: StateFlow<Int?> = userRepository.getUserFlow()
        .map { it?.coins }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun resetCoins() = userRepository.resetCoins()
}
