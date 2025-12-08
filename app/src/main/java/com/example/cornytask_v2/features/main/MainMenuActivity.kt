package com.example.cornytask_v2.features.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cornytask_v2.R
import com.example.cornytask_v2.features.history.HistoryScreen
import com.example.cornytask_v2.features.login.LoginActivity
import com.example.cornytask_v2.features.rewards.RewardsScreen
import com.example.cornytask_v2.features.todo.AddTodoActivity
import com.example.cornytask_v2.features.todo.TodoScreen
import com.example.cornytask_v2.features.user.UserViewModel
import com.example.cornytask_v2.ui.theme.Cornytaskv2Theme
import com.example.cornytask_v2.ui.theme.DeepPink
import com.example.cornytask_v2.ui.theme.LightPink
import com.example.cornytask_v2.ui.theme.Pink40
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class MainMenuActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            Cornytaskv2Theme {
                MainScreen(onSignOut = ::signOut)
            }
        }
    }

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener(this) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onSignOut: () -> Unit, userViewModel: UserViewModel = viewModel()) {
    val navController = rememberNavController()
    var showMenu by remember { mutableStateOf(false) }
    val user by userViewModel.user.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CornyTask") },
                actions = {
                    user?.let { CoinPill(coins = it.coins) }
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sign out") },
                                onClick = onSignOut
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentDestination?.route == Screen.Todo.route) {
                FloatingActionButton(onClick = { context.startActivity(Intent(context, AddTodoActivity::class.java)) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add a new TODO")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    Screen.Todo,
                    Screen.Rewards,
                    Screen.History
                )
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.route) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepPink,
                            unselectedIconColor = Pink40,
                            selectedTextColor = DeepPink,
                            unselectedTextColor = Pink40,
                            indicatorColor = LightPink
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Todo.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Todo.route) { TodoScreen() }
            composable(Screen.Rewards.route) { RewardsScreen() }
            composable(Screen.History.route) { HistoryScreen() }
        }
    }
}

@Composable
fun CoinPill(coins: Int) {
    Card(
        shape = CircleShape,
        modifier = Modifier.padding(end = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = coins.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Image(
                painter = painterResource(id = R.drawable.unicorn_small),
                contentDescription = "Coins",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
