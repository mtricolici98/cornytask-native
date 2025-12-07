package com.example.cornytask_v2.features.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cornytask_v2.data.User
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class UserViewModel : ViewModel() {

    private val userRepository = UserRepository()

    val user: StateFlow<User?> = userRepository.getUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

}