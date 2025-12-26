package com.example.cornytask_v2.features.rewards

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cornytask_v2.data.Reward
import com.example.cornytask_v2.ui.theme.DeepPink
import com.example.cornytask_v2.ui.theme.Purple40
import com.example.cornytask_v2.ui.theme.SoftPink
import java.util.concurrent.TimeUnit
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter

private val festiveTitles = listOf(
    "Woohoo!",
    "Congratulations!",
    "You earned it!",
    "Sweet Reward!",
    "Awesome!"
)

private val festiveMessages = listOf(
    "Enjoy your well-deserved {reward}!",
    "Time to celebrate with your new {reward}!",
    "All your hard work paid off. Enjoy the {reward}!",
    "This {reward} is all yours. You're a star!",
    "Look what you got! A shiny new {reward}."
)

@Composable
fun RewardsScreen(viewModel: RewardViewModel = viewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RewardScreenEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val rewards by viewModel.rewards.collectAsState()
    val user by viewModel.user.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<Reward?>(null) }
    var showRedeemedDialog by remember { mutableStateOf<Reward?>(null) }
    val favoriteRewards = rewards.filter { it.isFavorite }

    val currentCoins = user?.coins ?: 0

    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)) {
            if (favoriteRewards.isNotEmpty()) {
                FavoriteRewardsProgress(
                    favoriteRewards = favoriteRewards,
                    currentCoins = currentCoins
                )
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(rewards, key = { it.id }) { reward ->
                    RewardItem(
                        reward = reward,
                        currentCoins = currentCoins,
                        onRedeem = {
                            viewModel.onRedeemReward(reward)
                            showRedeemedDialog = reward
                        },
                        onLongPress = { showDeleteDialog = reward },
                        onToggleFavorite = { viewModel.onToggleFavorite(reward) }
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
        val randomTitle = remember { festiveTitles.random() }
        val randomMessage = remember { festiveMessages.random().replace("{reward}", reward.title) }
        Dialog(onDismissRequest = { showRedeemedDialog = null }) {
            Box(contentAlignment = Alignment.Center) {
                Card {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = randomTitle,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(text = randomMessage)
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = { showRedeemedDialog = null }) {
                            Text("Yay", color = DeepPink)
                        }
                    }
                }

                KonfettiView(
                    modifier = Modifier.fillMaxSize(),
                    parties = remember {
                        listOf(
                            Party(
                                speed = 0f,
                                maxSpeed = 30f,
                                damping = 0.9f,
                                spread = 360,
                                colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
                                emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(100),
                                position = Position.Relative(0.5, 0.0)
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun RewardItem(
    reward: Reward,
    currentCoins: Int,
    onRedeem: () -> Unit,
    onLongPress: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val needed = reward.cost - currentCoins
    val trailingText = if (needed > 0) "Need $needed" else "Redeem"

    ListItem(
        headlineContent = { Text(reward.title) },
        supportingContent = { Text("Cost: ${reward.cost} unicorns") },
        leadingContent = {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (reward.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Favorite",
                    tint = if (reward.isFavorite) Purple40 else Color.Gray
                )
            }
        },
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


@Composable
private fun FavoriteRewardsProgress(
    favoriteRewards: List<Reward>,
    currentCoins: Int,
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(8.dp),
        ) {
            Text("Favorite Rewards Progress", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = Purple40)
            Spacer(modifier = Modifier.height(4.dp))
            favoriteRewards.sortedByDescending { (currentCoins.toFloat() / it.cost).coerceAtMost(1f) }
                .forEach { reward ->
                    val progress by animateFloatAsState(
                        targetValue = (currentCoins.toFloat() / reward.cost).coerceAtMost(1f),
                        label = "progressAnimation"
                    )
                    val rawProgress = (currentCoins.toFloat() / reward.cost).coerceAtMost(1f)
                    if (rawProgress == 1f) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(reward.title, modifier = Modifier.weight(1f), color = Purple40)
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Favorite",
                                tint = DeepPink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(reward.title, modifier = Modifier.weight(1f), color = Purple40)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Purple40
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
        }
    }
}
