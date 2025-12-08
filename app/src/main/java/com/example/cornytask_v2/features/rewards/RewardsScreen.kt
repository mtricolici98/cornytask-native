package com.example.cornytask_v2.features.rewards

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cornytask_v2.data.Reward

@Composable
fun RewardsScreen(viewModel: RewardViewModel = viewModel()) {
    val rewards by viewModel.rewards.collectAsState()
    val user by viewModel.user.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<Reward?>(null) }
    var showRedeemedDialog by remember { mutableStateOf<Reward?>(null) }

    val currentCoins = user?.coins ?: 0

    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(rewards, key = { it.id }) { reward ->
                    RewardItem(
                        reward = reward,
                        currentCoins = currentCoins,
                        onRedeem = { viewModel.onRedeemReward(reward); showRedeemedDialog = reward },
                        onLongPress = { showDeleteDialog = reward }
                    )
                }
            }

            if (viewModel.isAddingReward) {
                AddRewardSection(
                    title = viewModel.newRewardTitle,
                    onTitleChange = { viewModel.newRewardTitle = it },
                    cost = viewModel.newRewardCost,
                    onCostChange = { viewModel.newRewardCost = it },
                    onAdd = { viewModel.onAddReward() },
                    onCancel = { viewModel.isAddingReward = false }
                )
            } else {
                Button(onClick = { viewModel.isAddingReward = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Add New Reward")
                }
            }
        }
    }

    showDeleteDialog?.let { reward ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Reward") },
            text = { Text("Are you sure you want to delete this reward?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onDeleteReward(reward); showDeleteDialog = null }) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") } }
        )
    }

    showRedeemedDialog?.let { reward ->
        AlertDialog(
            onDismissRequest = { showRedeemedDialog = null },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
            title = { Text("Reward Redeemed!") },
            text = { Text("Enjoy your ${reward.title}!") },
            confirmButton = { TextButton(onClick = { showRedeemedDialog = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun RewardItem(
    reward: Reward,
    currentCoins: Int,
    onRedeem: () -> Unit,
    onLongPress: () -> Unit
) {
    val needed = reward.cost - currentCoins
    val trailingText = if (needed > 0) "Need $needed" else "Redeem"

    ListItem(
        headlineContent = { Text(reward.title) },
        supportingContent = { Text("Cost: ${reward.cost} unicorns") },
        trailingContent = {
            Button(onClick = onRedeem, enabled = needed <= 0) {
                Text(trailingText)
            }
        },
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = { onLongPress() })
        }
    )
}

@Composable
private fun AddRewardSection(
    title: String,
    onTitleChange: (String) -> Unit,
    cost: String,
    onCostChange: (String) -> Unit,
    onAdd: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        TextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        TextField(
            value = cost,
            onValueChange = onCostChange,
            label = { Text("Cost (in unicorns)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onAdd) { Text("Add Reward") }
        }
    }
}