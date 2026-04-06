package com.nobadhabbits.cornytask.features.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nobadhabbits.cornytask.R
import com.nobadhabbits.cornytask.features.history.HistoryScreen
import com.nobadhabbits.cornytask.features.login.LoginActivity
import com.nobadhabbits.cornytask.features.main.MoreScreen as MoreScreenItems
import com.nobadhabbits.cornytask.features.mood_tracking.AddMoodScreen
import com.nobadhabbits.cornytask.features.mood_tracking.MoodScreen
import com.nobadhabbits.cornytask.features.mood_tracking.MoodTrackingRepository
import com.nobadhabbits.cornytask.features.mood_tracking.MoodViewModel
import com.nobadhabbits.cornytask.features.more.MoreScreen
import com.nobadhabbits.cornytask.features.notes.EditNoteScreen
import com.nobadhabbits.cornytask.features.notes.NotesScreen
import com.nobadhabbits.cornytask.features.rewards.RewardsScreen
import com.nobadhabbits.cornytask.features.settings.SettingsScreen
import com.nobadhabbits.cornytask.features.time_goals.TimeGoalsScreen
import com.nobadhabbits.cornytask.features.time_goals.TimerScreen
import com.nobadhabbits.cornytask.features.todo.AddTodoActivity
import com.nobadhabbits.cornytask.features.todo.TodoScreen
import com.nobadhabbits.cornytask.features.user.UserViewModel
import com.nobadhabbits.cornytask.features.widget.ACTION_DATA_UPDATED
import com.nobadhabbits.cornytask.features.widget.MoodNotificationReceiver.Companion.EXTRA_OPEN_ADD_MOOD
import com.nobadhabbits.cornytask.features.widget.TodoWidget
import com.nobadhabbits.cornytask.ui.theme.Cornytaskv2Theme
import com.nobadhabbits.cornytask.ui.theme.DeepPink
import com.nobadhabbits.cornytask.ui.theme.LightPink
import com.nobadhabbits.cornytask.ui.theme.Pink40
import kotlinx.coroutines.launch

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

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            Cornytaskv2Theme {
                MainScreen(onSignOut = ::signOut)
            }
        }

        askNotificationPermission()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
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
    var showFab by remember { mutableStateOf(true) }
    var handledAddMoodIntent by remember { mutableStateOf(false) }
    val moodViewModel: MoodViewModel = viewModel(
        factory = MoodViewModelFactory(
            MoodTrackingRepository(
                FirebaseFirestore.getInstance(),
                FirebaseAuth.getInstance()
            )
        )
    )
    val activity = context as? MainMenuActivity

    LaunchedEffect(Unit) {
        if (!handledAddMoodIntent) {
            val shouldOpen = activity?.intent?.getBooleanExtra(EXTRA_OPEN_ADD_MOOD, false) == true

            if (shouldOpen) {
                handledAddMoodIntent = true
                Log.i("TAG", "MainScreen: logging this aagaina")
                navController.navigate("${Routes.MORE_GRAPH}/add_mood"){
                    popUpTo(navController.graph.findStartDestination().id) {
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                // Clear intent AFTER consuming
                activity?.intent?.removeExtra(EXTRA_OPEN_ADD_MOOD)
            }
        }
    }
    (context as? MainMenuActivity)?.intent?.getStringExtra("timeGoalId")?.let {
        LaunchedEffect(it) {
            navController.navigate(Screen.TimeGoals.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val showBottomBar = when (currentDestination?.route) {
        "More/edit_note", "More/edit_note/{noteId}" -> false
        else -> true
    }
    Scaffold(
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
            )
        ),
        topBar = {
            TopAppBar(
                title = { Text("CornyTask") },
                actions = {
                    user?.let { coinPill ->
                        CoinPill(coins = coinPill.coins, onClick = {
                            navController.navigate(Screen.Rewards.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        })
                    }
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
                                        GlanceAppWidgetManager(context).getGlanceIds(TodoWidget::class.java)
                                            .forEach { glanceId ->
                                                updateAppWidgetState(context, glanceId) {
                                                    it.clear()
                                                }
                                            }

                                        context.sendBroadcast(
                                            Intent(
                                                context,
                                                TodoWidget::class.java
                                            ).setAction(ACTION_DATA_UPDATED)
                                        )
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
            if (currentDestination?.route == Screen.Todo.route && showFab) {
                FloatingActionButton(onClick = {
                    context.startActivity(
                        Intent(
                            context,
                            AddTodoActivity::class.java
                        )
                    )
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add a new TODO")
                }
            } else if (currentDestination?.route == MoreScreenItems.Mood.route) {
                FloatingActionButton(
                    onClick = {

                        navController.navigate("${Routes.MORE_GRAPH}/add_mood")
                        {
                            launchSingleTop = true
                        }
                    }
                ){Icon(Icons.Default.Add, contentDescription = "Add a new mood record")
                }
            }
        },
        bottomBar = {

            val currentRoute = currentDestination?.route
            if (showBottomBar) {
                NavigationBar {
                    val items = listOf(
                        Screen.Todo,
                        Screen.Rewards,
                        Screen.TimeGoals,
                        Screen.More
                    )
                    items.forEach { screen ->
                        val selected = when (screen) {
                            Screen.More -> currentRoute?.startsWith("More/") == true
                            Screen.TimeGoals -> currentRoute == Routes.TIMEGOALS_MAIN || currentRoute == "timer_screen"
                            else -> currentRoute == screen.route
                        }
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.label) },
                            selected = selected,
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
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Todo.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Todo.route) { TodoScreen(onTabSelected = { showFab = it }) }
            composable(Screen.Rewards.route) { RewardsScreen() }

            navigation(startDestination = Routes.TIMEGOALS_MAIN, route = Routes.TIMEGOALS_GRAPH) {
                composable(Routes.TIMEGOALS_MAIN) { TimeGoalsScreen(navController = navController) }
                composable("timer_screen") { TimerScreen(navController = navController) }
            }


            navigation(startDestination = Routes.MORE_MAIN, route = Routes.MORE_GRAPH) {
                composable(Routes.MORE_MAIN) { MoreScreen(navController = navController) }
                composable(MoreScreenItems.History.route) { HistoryScreen() }
                composable("${MoreScreenItems.Mood.route}") {
                    val moodRecords by moodViewModel.moodRecords.collectAsState()
                    LaunchedEffect(Unit) {
                        moodViewModel.fetchMoodRecords()
                    }
                    MoodScreen(moodRecords = moodRecords)
                }
                composable("${Routes.MORE_GRAPH}/add_mood") {
                    AddMoodScreen(onAddMood = { date, moodScore ->
                        moodViewModel.addMoodRecord(date, moodScore)
                        navController.popBackStack()
                    })
                }
                composable(MoreScreenItems.Notes.route) { NotesScreen(navController = navController) }
                composable(
                    "More/edit_note/{noteId}",
                    arguments = listOf(navArgument("noteId") { type = NavType.StringType })
                ) { backStackEntry ->
                    EditNoteScreen(
                        navController = navController,
                        noteId = backStackEntry.arguments?.getString("noteId")
                    )
                }
                composable("edit_note") {
                    EditNoteScreen(navController = navController, noteId = null)
                }
                composable(MoreScreenItems.Settings.route) { SettingsScreen(onSignOut) }
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
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = animatedCoins.toInt().toString(),
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Image(
                painter = painterResource(id = R.drawable.unicorn_small),
                contentDescription = "Coins",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

class MoodViewModelFactory(private val repository: MoodTrackingRepository) :
    androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MoodViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MoodViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}