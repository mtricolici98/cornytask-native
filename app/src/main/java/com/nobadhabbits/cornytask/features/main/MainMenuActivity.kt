package com.nobadhabbits.cornytask.features.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.nobadhabbits.cornytask.MainActivity
import com.nobadhabbits.cornytask.R
import com.nobadhabbits.cornytask.features.history.HistoryScreen
import com.nobadhabbits.cornytask.features.login.LoginActivity
import com.nobadhabbits.cornytask.features.more.MoreScreen
import com.nobadhabbits.cornytask.features.notes.NotesScreen
import com.nobadhabbits.cornytask.features.rewards.RewardsScreen
import com.nobadhabbits.cornytask.features.time_goals.TimeGoalsScreen
import com.nobadhabbits.cornytask.features.time_goals.TimerScreen
import com.nobadhabbits.cornytask.features.todo.AddTodoActivity
import com.nobadhabbits.cornytask.features.todo.TodoScreen
import com.nobadhabbits.cornytask.features.user.UserViewModel
import com.nobadhabbits.cornytask.features.widget.ACTION_DATA_UPDATED
import com.nobadhabbits.cornytask.features.widget.TodoWidget
import com.nobadhabbits.cornytask.ui.theme.Cornytaskv2Theme
import com.nobadhabbits.cornytask.ui.theme.DeepPink
import com.nobadhabbits.cornytask.ui.theme.LightPink
import com.nobadhabbits.cornytask.ui.theme.Pink40
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch
import com.nobadhabbits.cornytask.features.main.MoreScreen as MoreScreenItems

class MainMenuActivity : FragmentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted. Continue the action or workflow in your
            // app.
        } else {
            // Explain to the user that the feature is unavailable because the
            // feature requires a permission that the user has denied. At the
            // same time, respect the user's decision. Don't link to system
            // settings in an effort to convince the user to change their
            // decision.
        }
    }

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

        askNotificationPermission()
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: Display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       with two options: "Allow" and "Don't allow".
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
    val scope = rememberCoroutineScope()
    val timeGoalSet = (context as MainMenuActivity).intent.getStringExtra("timeGoalId")
    if (timeGoalSet != null && timeGoalSet != "") {
        navController.navigate(Screen.TimeGoals.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CornyTask") },
                navigationIcon = {
                    if (currentDestination?.parent?.route == Screen.More.route && currentDestination.route != "more_menu") {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    user?.let { CoinPill(coins = it.coins, onClick = {navController.navigate(Screen.Rewards.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    } }) }
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
                                onClick = {
                                    scope.launch {
                                        GlanceAppWidgetManager(context).getGlanceIds(TodoWidget::class.java).forEach { glanceId ->
                                            updateAppWidgetState(context, glanceId) {
                                                it.clear()
                                            }
                                        }

                                        context.sendBroadcast(Intent(context, TodoWidget::class.java).setAction(ACTION_DATA_UPDATED))
                                    }
                                    onSignOut()
                                }
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
                    Screen.TimeGoals,
                    Screen.More
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
            composable(Screen.Todo.route) { TodoScreen( ) }
            composable(Screen.Rewards.route) { RewardsScreen() }
            composable(Screen.TimeGoals.route) { TimeGoalsScreen(navController = navController) }
            composable("timer_screen") { TimerScreen(navController = navController) }
            navigation(startDestination = "more_menu", route = Screen.More.route) {
                composable("more_menu") { MoreScreen(navController = navController) }
                composable(MoreScreenItems.History.route) { HistoryScreen() }
                composable(MoreScreenItems.Notes.route) { NotesScreen() }
            }
        }
    }
}

@Composable
fun CoinPill(coins: Int, onClick: () -> Unit) {
    val animatedCoins by animateFloatAsState(
        targetValue = coins.toFloat(),
        label = "animatedCoins"
    )
    Card(
        shape = CircleShape,
        modifier = Modifier.padding(end = 8.dp).clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = animatedCoins.toInt().toString(), color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Image(
                painter = painterResource(id = R.drawable.unicorn_small),
                contentDescription = "Coins",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
