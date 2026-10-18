package com.nobadhabbits.cornytask.features.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nobadhabbits.cornytask.R
import com.nobadhabbits.cornytask.features.history.HistoryScreen
import com.nobadhabbits.cornytask.features.login.AuthSession
import com.nobadhabbits.cornytask.features.medat.MedatScreen
import com.nobadhabbits.cornytask.features.mood_tracking.AddMoodScreen
import com.nobadhabbits.cornytask.features.mood_tracking.MoodScreen
import com.nobadhabbits.cornytask.features.mood_tracking.MoodTrackingRepository
import com.nobadhabbits.cornytask.features.mood_tracking.MoodViewModel
import com.nobadhabbits.cornytask.features.notes.EditNoteScreen
import com.nobadhabbits.cornytask.features.notes.NotesScreen
import com.nobadhabbits.cornytask.features.rewards.RewardsScreen
import com.nobadhabbits.cornytask.features.settings.SettingsScreen
import com.nobadhabbits.cornytask.features.time_goals.TimeGoalsScreen
import com.nobadhabbits.cornytask.features.time_goals.TimerScreen
import com.nobadhabbits.cornytask.features.todo.AddTodoActivity
import com.nobadhabbits.cornytask.features.todo.TodoScreen
import com.nobadhabbits.cornytask.features.user.UserViewModel
import com.nobadhabbits.cornytask.features.widget.MoodNotificationReceiver.Companion.EXTRA_OPEN_ADD_MOOD
import com.nobadhabbits.cornytask.ui.theme.Cornytaskv2Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainMenuActivity : FragmentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private var redirectedToLogin = false

    // Routes to open because of a notification tap. Fed from both onCreate and onNewIntent,
    // so it works whether the app was closed or already open.
    private val deepLink = MutableStateFlow<List<String>?>(null)

    // If the Firebase session goes away (sign-out, revoked, expired), go to login
    // instead of letting screens hit a missing user.
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (firebaseAuth.currentUser == null) redirectToLogin()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Nothing to do either way; reminders just won't show if denied.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Can be opened directly from the timer notification, bypassing MainActivity.
        if (auth.currentUser == null) {
            redirectToLogin()
            return
        }

        // On recreation (rotation, process restore) the intent is the old one; don't replay it.
        if (savedInstanceState == null) deepLink.value = deepLinkRoutes(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            Cornytaskv2Theme {
                MainScreen(
                    deepLink = deepLink,
                    onDeepLinkHandled = { deepLink.value = null },
                    onSignOut = ::signOut
                )
            }
        }

        askNotificationPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkRoutes(intent)?.let { deepLink.value = it }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED &&
            !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        auth.removeAuthStateListener(authStateListener)
        super.onStop()
    }

    private fun signOut() {
        AuthSession.signOut(this)
    }

    private fun redirectToLogin() {
        if (redirectedToLogin) return
        redirectedToLogin = true
        startActivity(AuthSession.loginIntent(this))
        finish()
    }

    companion object {
        /** Set by the timer notification; opens the running timer. */
        const val EXTRA_TIME_GOAL_ID = "timeGoalId"

        /** The back stack to build for an intent, top-level screen first. */
        fun deepLinkRoutes(intent: Intent?): List<String>? = when {
            intent == null -> null
            intent.getBooleanExtra(EXTRA_OPEN_ADD_MOOD, false) -> listOf(Routes.MOOD, Routes.ADD_MOOD)
            intent.getStringExtra(EXTRA_TIME_GOAL_ID) != null -> listOf(Routes.TIME_GOALS, Routes.TIMER)
            else -> null
        }
    }
}

/**
 * Switch to a top-level screen, always landing on its main view.
 * No saveState/restoreState on purpose: the saved stack gets keyed to the start destination
 * too, so after a deep link (TODOs -> Mood -> Add mood) tapping TODOs restored Add mood again.
 */
fun NavController.openTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    deepLink: StateFlow<List<String>?>,
    onDeepLinkHandled: () -> Unit,
    onSignOut: () -> Unit,
    userViewModel: UserViewModel = viewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val navOrderPreferences = remember { NavOrderPreferences(context) }
    var order by remember { mutableStateOf(navOrderPreferences.getOrder()) }
    // Read once: the screen you put first is where the app opens.
    val startRoute = remember { order.first().route }

    var showMenu by remember { mutableStateOf(false) }
    var moreExpanded by remember { mutableStateOf(false) }
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    val user by userViewModel.user.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showFab by remember { mutableStateOf(true) }
    val moodViewModel: MoodViewModel = viewModel(
        factory = MoodViewModelFactory(
            MoodTrackingRepository(
                FirebaseFirestore.getInstance(),
                FirebaseAuth.getInstance()
            )
        )
    )

    val pendingDeepLink by deepLink.collectAsState()
    LaunchedEffect(pendingDeepLink) {
        val routes = pendingDeepLink ?: return@LaunchedEffect
        moreExpanded = false
        navController.navigate(routes.first()) {
            popUpTo(navController.graph.findStartDestination().id)
            launchSingleTop = true
        }
        routes.drop(1).forEach { route ->
            navController.navigate(route) { launchSingleTop = true }
        }
        onDeepLinkHandled()
    }

    // Close the menu whenever we land somewhere new.
    LaunchedEffect(currentRoute) { moreExpanded = false }
    BackHandler(enabled = moreExpanded) { moreExpanded = false }

    fun openDestination(destination: Destination) {
        moreExpanded = false
        navController.openTopLevel(destination.route)
    }

    val showBottomBar = currentRoute != Routes.NEW_NOTE && currentRoute != Routes.EDIT_NOTE

    Box(modifier = Modifier.fillMaxSize()) {
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
                                openDestination(Destination.REWARDS)
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
                                        showMenu = false
                                        onSignOut()
                                    }
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (currentRoute == Routes.TODOS && showFab) {
                    FloatingActionButton(onClick = {
                        context.startActivity(Intent(context, AddTodoActivity::class.java))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add a new TODO")
                    }
                } else if (currentRoute == Routes.MOOD) {
                    FloatingActionButton(onClick = {
                        navController.navigate(Routes.ADD_MOOD) { launchSingleTop = true }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add a new mood record")
                    }
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    MainBottomBar(
                        order = order,
                        currentRoute = currentRoute,
                        moreExpanded = moreExpanded,
                        onNavigate = ::openDestination,
                        onToggleMore = { moreExpanded = !moreExpanded },
                        modifier = Modifier.onSizeChanged { bottomBarHeightPx = it.height }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController,
                startDestination = startRoute,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.TODOS) { TodoScreen(onTabSelected = { showFab = it }) }
                composable(Routes.REWARDS) { RewardsScreen() }
                composable(Routes.TIME_GOALS) { TimeGoalsScreen(navController = navController) }
                composable(Routes.TIMER) { TimerScreen(navController = navController) }
                composable(Routes.HISTORY) { HistoryScreen() }
                composable(Routes.MOOD) {
                    val moodRecords by moodViewModel.moodRecords.collectAsState()
                    LaunchedEffect(Unit) {
                        moodViewModel.fetchMoodRecords()
                    }
                    MoodScreen(moodRecords = moodRecords)
                }
                composable(Routes.ADD_MOOD) {
                    AddMoodScreen(onAddMood = { date, moodScore, comment ->
                        moodViewModel.addMoodRecord(date, moodScore, comment)
                        navController.popBackStack()
                    })
                }
                composable(Routes.NOTES) { NotesScreen(navController = navController) }
                composable(
                    Routes.EDIT_NOTE,
                    arguments = listOf(navArgument("noteId") { type = NavType.StringType })
                ) { backStackEntry ->
                    EditNoteScreen(
                        navController = navController,
                        noteId = backStackEntry.arguments?.getString("noteId")
                    )
                }
                composable(Routes.NEW_NOTE) {
                    EditNoteScreen(navController = navController, noteId = null)
                }
                composable(Routes.MEDAT) { MedatScreen() }
                composable(Routes.SETTINGS) { SettingsScreen() }
            }
        }

        // The More menu slides up from the bar, over the content. The bar itself stays
        // tappable so the More button can close it again.
        val barHeight = with(LocalDensity.current) { bottomBarHeightPx.toDp() }
        AnimatedVisibility(
            visible = moreExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = barHeight)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { moreExpanded = false }
            )
        }
        AnimatedVisibility(
            visible = moreExpanded,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = barHeight),
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
        ) {
            MoreMenuPanel(
                order = order,
                currentRoute = currentRoute,
                onNavigate = ::openDestination,
                onOrderChange = {
                    order = it
                    navOrderPreferences.setOrder(it)
                }
            )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = animatedCoins.toInt().toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
