package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import com.example.ui.FinanceViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

object Routes {
    const val DASHBOARD = "dashboard"
    const val WALLETS = "wallets"
    const val HISTORY = "history"
    const val STATS = "stats"
    const val BUDGET_GOAL = "budget_goal"
    const val SETTINGS = "settings"
    const val ADD_TRANSACTION = "add_transaction"
    const val BANK_NOTIFICATION_HISTORY = "bank_notification_history"
}

class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Gracefully lock app if PIN protection is active to enforce background security protection
        viewModel.lockApp()
    }
}

@Composable
fun MainContent(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
    val isLoadingSettings by viewModel.isLoadingSettings.collectAsState()

    // Observe active routes
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.DASHBOARD

    // Keep track of the last primary route to keep bottom bar item selected even when in "add_transaction"
    var lastPrimaryRoute by remember { mutableStateOf(Routes.DASHBOARD) }
    LaunchedEffect(currentRoute) {
        if (currentRoute in listOf(Routes.DASHBOARD, Routes.HISTORY, Routes.BUDGET_GOAL, Routes.STATS, Routes.SETTINGS)) {
            lastPrimaryRoute = currentRoute
        }
    }

    // Determine adaptive layout size classes
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    val navigationItems = listOf(
        MainNavigationItem(Routes.DASHBOARD, "Tổng quan", Icons.Default.GridView),
        MainNavigationItem(Routes.HISTORY, "Lịch sử", Icons.Default.History),
        MainNavigationItem(Routes.ADD_TRANSACTION, "Giao dịch", Icons.Default.Add),
        MainNavigationItem(Routes.STATS, "Thống kê", Icons.Default.PieChart),
        MainNavigationItem(Routes.SETTINGS, "Cài đặt", Icons.Default.Settings)
    )

    if (isLoadingSettings) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (!isAppUnlocked) {
        // App is locked by PIN protection -> display overlay lock screen gate
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            PinLockScreen(viewModel = viewModel)
        }
    } else {
        // App is unlocked -> display primary dashboard or nested modules
        val focusManager = LocalFocusManager.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
        ) {
            if (isWideScreen) {
            // Adaptive wide layout: use Left Navigation Rail + Right Box
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    navigationItems.forEach { item ->
                        val isSelected = when (item.route) {
                            Routes.DASHBOARD -> currentRoute == Routes.DASHBOARD || currentRoute == Routes.WALLETS
                            Routes.HISTORY -> currentRoute == Routes.HISTORY
                            Routes.ADD_TRANSACTION -> currentRoute == Routes.ADD_TRANSACTION
                            Routes.STATS -> currentRoute == Routes.STATS
                            Routes.SETTINGS -> currentRoute == Routes.SETTINGS
                            else -> false
                        }
                        if (item.route == Routes.ADD_TRANSACTION) {
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                icon = {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = item.label, tint = Color.White)
                                    }
                                },
                                label = { Text(item.label, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("nav_rail_item_${item.route}")
                            )
                        } else {
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag("nav_rail_item_${item.route}")
                            )
                        }
                    }
                }

                // Screen container
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHostContainer(
                        navController = navController,
                        viewModel = viewModel,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    )
                }
            }
        } else {
            // Adaptive portrait layout: Standard Scaffold + Bottom Navigation Bar
            Scaffold(
                modifier = modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        navigationItems.forEach { item ->
                            val isSelected = when (item.route) {
                                Routes.DASHBOARD -> currentRoute == Routes.DASHBOARD || currentRoute == Routes.WALLETS
                                Routes.HISTORY -> currentRoute == Routes.HISTORY
                                Routes.ADD_TRANSACTION -> currentRoute == Routes.ADD_TRANSACTION
                                Routes.STATS -> currentRoute == Routes.STATS
                                Routes.SETTINGS -> currentRoute == Routes.SETTINGS
                                else -> false
                            }
                            if (item.route == Routes.ADD_TRANSACTION) {
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), 
                                                    shape = CircleShape
                                                )
                                                .padding(10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = item.label,
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.testTag("nav_bar_item_${item.route}")
                                )
                            } else {
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, fontSize = 11.sp, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_bar_item_${item.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHostContainer(
                        navController = navController,
                        viewModel = viewModel,
                        modifier = Modifier
                    )
                }
            }
        }
        }
    }
}

@Composable
fun NavHostContainer(
    navController: androidx.navigation.NavHostController,
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.ADD_TRANSACTION,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(150))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(150))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(150))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(150))
        }
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToWallets = { navController.navigate(Routes.WALLETS) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToStats = { navController.navigate(Routes.STATS) },
                onNavigateToBudget = { navController.navigate(Routes.BUDGET_GOAL) }
            )
        }

        composable(Routes.WALLETS) {
            WalletsScreen(viewModel = viewModel)
        }

        composable(Routes.HISTORY) {
            HistoryScreen(viewModel = viewModel)
        }

        composable(Routes.STATS) {
            ReportsScreen(viewModel = viewModel)
        }

        composable(Routes.BUDGET_GOAL) {
            BudgetGoalScreen(viewModel = viewModel)
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToBankNotificationHistory = { navController.navigate(Routes.BANK_NOTIFICATION_HISTORY) }
            )
        }

        composable(Routes.BANK_NOTIFICATION_HISTORY) {
            BankNotificationHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.ADD_TRANSACTION,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(250)
                ) + fadeIn(animationSpec = tween(250))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(250)
                ) + fadeOut(animationSpec = tween(250))
            },
            popEnterTransition = {
                null
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(250)
                ) + fadeOut(animationSpec = tween(250))
            }
        ) {
            AddTransactionScreen(
                viewModel = viewModel,
                onSuccess = {
                    navController.navigate(Routes.HISTORY) {
                        popUpTo(Routes.ADD_TRANSACTION) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

data class MainNavigationItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
