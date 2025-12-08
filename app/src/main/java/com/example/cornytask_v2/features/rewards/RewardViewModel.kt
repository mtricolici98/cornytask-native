package com.example.cornytask_v2.features.rewards

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cornytask_v2.data.Reward
import com.example.cornytask_v2.data.User
import com.example.cornytask_v2.features.user.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RewardViewModel : ViewModel() {

    private val rewardRepository = RewardRepository()
    private val userRepository = UserRepository()

    val rewards: StateFlow<List<Reward>> = rewardRepository.getRewardsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val user: StateFlow<User?> = userRepository.getUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var isAddingReward by mutableStateOf(false)
    var newRewardTitle by mutableStateOf("")
    var newRewardCost by mutableStateOf("")

    fun onRedeemReward(reward: Reward) {
        viewModelScope.launch {
            user.value?.let {
                if (it.coins >= reward.cost) {
                    userRepository.spendCoins(reward.cost)
                    // Here you would typically show a success message
                }
            }
        }
    }

    fun onAddReward() {
        viewModelScope.launch {
            val cost = newRewardCost.toIntOrNull()
            if (newRewardTitle.isNotBlank() && cost != null && cost > 0) {
                rewardRepository.addReward(newRewardTitle, cost)
                // Reset fields and UI state
                newRewardTitle = ""
                newRewardCost = ""
                isAddingReward = false
            }
        }
    }

    fun onDeleteReward(reward: Reward) {
        viewModelScope.launch {
            rewardRepository.deleteReward(reward.id)
        }
    }
}