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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
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

    // Observe active routes
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.DASHBOARD

    // Determine adaptive layout size classes
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    val navigationItems = listOf(
        NavigationItem(Routes.DASHBOARD, "Tổng quan", Icons.Default.GridView),
        NavigationItem(Routes.HISTORY, "Giao dịch", Icons.Default.History),
        NavigationItem(Routes.BUDGET_GOAL, "Hạn mức", Icons.Default.Savings),
        NavigationItem(Routes.STATS, "Thống kê", Icons.Default.PieChart),
        NavigationItem(Routes.SETTINGS, "Cài đặt", Icons.Default.Settings)
    )

    if (!isAppUnlocked) {
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
        if (isWideScreen) {
            // Adaptive wide layout: use Left Navigation Rail + Right Box
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                    header = {
                        FloatingActionButton(
                            onClick = { navController.navigate(Routes.ADD_TRANSACTION) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            modifier = Modifier.padding(vertical = 12.dp).testTag("fab_add_tx_rail")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm phát sinh")
                        }
                    }
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    navigationItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Routes.DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag("nav_rail_item_${item.route}")
                        )
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
                            val isSelected = currentRoute == item.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(Routes.DASHBOARD) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                                label = { Text(item.label, fontSize = 11.sp, maxLines = 1) },
                                modifier = Modifier.testTag("nav_bar_item_${item.route}")
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (currentRoute != Routes.ADD_TRANSACTION) {
                        FloatingActionButton(
                            onClick = { navController.navigate(Routes.ADD_TRANSACTION) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("fab_add_tx_main")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm mới giao dịch")
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

@Composable
fun NavHostContainer(
    navController: androidx.navigation.NavHostController,
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(220)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start, tween(220)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(220)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start, tween(220)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(220)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End, tween(220)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(220)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End, tween(220)
            )
        }
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToWallets = { navController.navigate(Routes.WALLETS) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToStats = { navController.navigate(Routes.STATS) }
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
            SettingsScreen(viewModel = viewModel)
        }

        composable(Routes.ADD_TRANSACTION) {
            AddTransactionScreen(
                viewModel = viewModel,
                onSuccess = {
                    navController.popBackStack()
                }
            )
        }
    }
}

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
