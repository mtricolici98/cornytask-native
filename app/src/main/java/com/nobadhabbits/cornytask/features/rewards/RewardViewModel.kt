package com.nobadhabbits.cornytask.features.rewards

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nobadhabbits.cornytask.data.Reward
import com.nobadhabbits.cornytask.data.User
import com.nobadhabbits.cornytask.features.user.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RewardViewModel : ViewModel() {

    private val rewardRepository = RewardRepository()
    private val userRepository = UserRepository()

    private val _events = MutableSharedFlow<RewardScreenEvent>()
    val events = _events.asSharedFlow()

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

    fun onToggleFavorite(reward: Reward) {
        viewModelScope.launch {
            val currentFavorites = rewards.first().count { it.isFavorite }
            if (!reward.isFavorite && currentFavorites >= 3) {
                _events.emit(RewardScreenEvent.ShowToast("Sorry, but you can track up to 3 favourite rewards"))
                return@launch
            }
            val updatedReward = reward.copy(isFavorite = !reward.isFavorite)
            rewardRepository.updateReward(updatedReward)
        }
    }
}

sealed class RewardScreenEvent {
    data class ShowToast(val message: String) : RewardScreenEvent()
}
