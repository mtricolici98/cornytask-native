package com.nobadhabbits.cornytask.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nobadhabbits.cornytask.features.user.UserRepository
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val userRepository = UserRepository()

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            userRepository.deleteUser()
            onSuccess()
        }
    }
}
