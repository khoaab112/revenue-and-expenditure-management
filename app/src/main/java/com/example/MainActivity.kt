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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
import com.example.ui.FormatHelper
import com.example.ui.AppNotification
import com.example.ui.NotificationType
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

object Routes {
    const val DASHBOARD = "dashboard"
    const val WALLETS = "wallets"
    const val HISTORY = "history"
    const val STATS = "stats"
    const val BUDGET_GOAL = "budget_goal"
    const val SAVINGS_VAULT = "savings_vault"
    const val SETTINGS = "settings"
    const val ADD_TRANSACTION = "add_transaction"
    const val BANK_NOTIFICATION_HISTORY = "bank_notification_history"
    const val TIMELINE = "timeline"
    const val EVENTS = "events"
    const val DEBT_BOOK = "debt_book"
}

class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openBankNotifications = intent?.getBooleanExtra("OPEN_BANK_NOTIFICATIONS", false) ?: false
        setContent {
            MyApplicationTheme {
                MainContent(viewModel = viewModel, forceStartWithBankNotifications = openBankNotifications)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Note: Full recomposition will happen if we pass state, but this simple approach is fine
        // if the app is heavily recreated.
        // Actually to handle onNewIntent reliably during foreground, we can just send an event to viewModel.
        if (intent.getBooleanExtra("OPEN_BANK_NOTIFICATIONS", false)) {
            viewModel.triggerOpenBankNotifications()
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
    modifier: Modifier = Modifier,
    forceStartWithBankNotifications: Boolean = false
) {
    val navController = rememberNavController()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
    val isLoadingSettings by viewModel.isLoadingSettings.collectAsState()
    val startScreen by viewModel.startScreen.collectAsState()
    
    val openBankNotificationsEvent by viewModel.openBankNotificationsEvent.collectAsState()

    // Observe active routes
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startScreen

    // Keep track of the last primary route to keep bottom bar item selected even when in "add_transaction"
    var lastPrimaryRoute by remember(startScreen) { mutableStateOf(startScreen) }

    LaunchedEffect(isAppUnlocked) {
        if (isAppUnlocked && forceStartWithBankNotifications) {
            navController.navigate(Routes.BANK_NOTIFICATION_HISTORY) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(openBankNotificationsEvent, isAppUnlocked) {
        if (openBankNotificationsEvent && isAppUnlocked) {
            navController.navigate(Routes.BANK_NOTIFICATION_HISTORY) {
                launchSingleTop = true
            }
            viewModel.consumeOpenBankNotificationsEvent()
        }
    }

    LaunchedEffect(currentRoute) {
        val baseRoute = currentRoute.substringBefore("?")
        if (baseRoute in listOf(Routes.DASHBOARD, Routes.HISTORY, Routes.BUDGET_GOAL, Routes.SAVINGS_VAULT, Routes.STATS, Routes.SETTINGS, Routes.BANK_NOTIFICATION_HISTORY)) {
            lastPrimaryRoute = baseRoute
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationLogs by viewModel.notificationLogs.collectAsState()
    val notificationReaderEnabled by viewModel.notificationReaderEnabled.collectAsState()

    val pendingCount = remember(notificationLogs) {
        notificationLogs.count { it.status == "PENDING" }
    }

    var showScanResultPopup by remember { mutableStateOf(false) }
    var scannedLogsList by remember { mutableStateOf<List<com.example.ui.NotificationLog>>(emptyList()) }
    var showPermissionErrorPopup by remember { mutableStateOf(false) }

    LaunchedEffect(isAppUnlocked, notificationReaderEnabled) {
        if (isAppUnlocked && notificationReaderEnabled) {
            val isPermitted = com.example.ui.screens.isNotificationServiceEnabled(context)
            if (!isPermitted) {
                showPermissionErrorPopup = true
            } else {
                // If it's permitted but the service is dead (e.g., killed by Xiaomi HyperOS),
                // request a rebind.
                if (com.example.service.BankNotificationListenerService.instance == null) {
                    com.example.service.BankNotificationListenerService.requestRebindService(context)
                    // Wait a bit for it to bind before trying to scan
                    kotlinx.coroutines.delay(1000)
                }

                viewModel.scanNotificationsManual(
                    context = context,
                    onSuccess = { count ->
                        if (count > 0) {
                            val activePending = viewModel.notificationLogs.value.filter { it.status == "PENDING" }.take(count)
                            scannedLogsList = activePending
                            showScanResultPopup = true
                        }
                    },
                    onError = { err ->
                        // Silent or ignored
                    }
                )
            }
        }
    }

    // Determine adaptive layout size classes
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    val navigationItems = listOf(
        MainNavigationItem(Routes.DASHBOARD, "Tổng quan", Icons.Default.GridView),
        MainNavigationItem(Routes.HISTORY, "Lịch sử", Icons.Default.History),
        MainNavigationItem(Routes.ADD_TRANSACTION, "Giao dịch", Icons.Default.Add),
        MainNavigationItem(Routes.BANK_NOTIFICATION_HISTORY, "Thông báo", Icons.Default.Notifications),
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
                            Routes.BANK_NOTIFICATION_HISTORY -> currentRoute == Routes.BANK_NOTIFICATION_HISTORY
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
                                icon = {
                                    if (item.route == Routes.BANK_NOTIFICATION_HISTORY && pendingCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(text = pendingCount.toString())
                                                }
                                            }
                                        ) {
                                            Icon(imageVector = item.icon, contentDescription = item.label)
                                        }
                                    } else {
                                        Icon(imageVector = item.icon, contentDescription = item.label)
                                    }
                                },
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
                                Routes.BANK_NOTIFICATION_HISTORY -> currentRoute == Routes.BANK_NOTIFICATION_HISTORY
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
                                    icon = {
                                        if (item.route == Routes.BANK_NOTIFICATION_HISTORY && pendingCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge {
                                                        Text(text = pendingCount.toString(), modifier = Modifier.testTag("notification_badge_count"))
                                                    }
                                                }
                                            ) {
                                                Icon(imageVector = item.icon, contentDescription = item.label)
                                            }
                                        } else {
                                            Icon(imageVector = item.icon, contentDescription = item.label)
                                        }
                                    },
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

                    // -------------------- POPUPS AND DIALOGS --------------------
                    if (showPermissionErrorPopup) {
                        AlertDialog(
                            onDismissRequest = { showPermissionErrorPopup = false },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            title = {
                                Text(
                                    text = "Lỗi cấp quyền đọc thông báo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            },
                            text = {
                                Text(
                                    text = "Tính năng đọc tin nhắn ngân hàng tự động đang bật, nhưng ứng dụng chưa được cấp quyền truy cập thông báo hệ thống. Vui lòng cấp quyền trong cài đặt.",
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showPermissionErrorPopup = false
                                        try {
                                            val intent = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            viewModel.showErrorNotification("Không thể mở cài đặt")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Cấp quyền")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showPermissionErrorPopup = false }
                                ) {
                                    Text("Đóng")
                                }
                            }
                        )
                    }

                    if (showScanResultPopup) {
                        AlertDialog(
                            onDismissRequest = { showScanResultPopup = false },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            title = {
                                Text(
                                    text = "Phát hiện giao dịch mới!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Hệ thống đã tự động quét được ${scannedLogsList.size} thông báo ngân hàng mới cần phê duyệt:",
                                        fontSize = 14.sp
                                    )
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        scannedLogsList.forEach { log ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = log.bankName.ifEmpty { "Giao dịch" },
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            text = "${if (log.type == "INCOME") "+" else "-"}${FormatHelper.formatVND(log.amount)}",
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 12.sp,
                                                            color = if (log.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = log.text,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showScanResultPopup = false
                                        navController.navigate(Routes.BANK_NOTIFICATION_HISTORY) {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Xem chi tiết")
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Navigate to bank notifications",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showScanResultPopup = false }
                                ) {
                                    Text("Đóng")
                                }
                            }
                        )
                    }
                }
            }

            // Custom Global Notifications Overlay - Displayed in top-right corner
            val appNotifications by viewModel.appNotifications.collectAsState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
                contentAlignment = Alignment.TopEnd
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .padding(top = 16.dp, end = 16.dp)
                        .widthIn(max = 340.dp)
                ) {
                    appNotifications.forEach { notification ->
                        key(notification.id) {
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                visible = true
                            }
                            AnimatedVisibility(
                                visible = visible,
                                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("notification_card_${notification.type.name.lowercase()}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (notification.type) {
                                            NotificationType.SUCCESS -> Color(0xFFE8F5E9)
                                            NotificationType.WARNING -> Color(0xFFFFF8E1)
                                            NotificationType.ERROR -> Color(0xFFFFEBEE)
                                        }
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        when (notification.type) {
                                            NotificationType.SUCCESS -> Color(0xFF81C784)
                                            NotificationType.WARNING -> Color(0xFFFFD54F)
                                            NotificationType.ERROR -> Color(0xFFE57373)
                                        }
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (notification.type) {
                                                NotificationType.SUCCESS -> Icons.Default.CheckCircle
                                                NotificationType.WARNING -> Icons.Default.Warning
                                                NotificationType.ERROR -> Icons.Default.Error
                                            },
                                            contentDescription = null,
                                            tint = when (notification.type) {
                                                NotificationType.SUCCESS -> Color(0xFF2E7D32)
                                                NotificationType.WARNING -> Color(0xFFF57F17)
                                                NotificationType.ERROR -> Color(0xFFC62828)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = when (notification.type) {
                                                    NotificationType.SUCCESS -> "Thành công"
                                                    NotificationType.WARNING -> "Cảnh báo"
                                                    NotificationType.ERROR -> "Lỗi"
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = when (notification.type) {
                                                    NotificationType.SUCCESS -> Color(0xFF1B5E20)
                                                    NotificationType.WARNING -> Color(0xFFE65100)
                                                    NotificationType.ERROR -> Color(0xFFB71C1C)
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = notification.message,
                                                fontSize = 12.sp,
                                                color = when (notification.type) {
                                                    NotificationType.SUCCESS -> Color(0xFF2E7D32)
                                                    NotificationType.WARNING -> Color(0xFF5D4037)
                                                    NotificationType.ERROR -> Color(0xFF7F0000)
                                                },
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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
    val startScreen by viewModel.startScreen.collectAsState()
    NavHost(
        navController = navController,
        startDestination = startScreen,
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
                onNavigateToWallets = { wallet ->
                    if (wallet != null) {
                        viewModel.setFocusedWalletId(wallet.id)
                    }
                    navController.navigate(Routes.WALLETS)
                },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToStats = { navController.navigate(Routes.STATS) },
                onNavigateToBudget = { navController.navigate(Routes.BUDGET_GOAL) },
                onNavigateToSavings = { navController.navigate(Routes.SAVINGS_VAULT) },
                onNavigateToBankNotifications = { navController.navigate(Routes.BANK_NOTIFICATION_HISTORY) },
                onNavigateToDebtBook = { navController.navigate(Routes.DEBT_BOOK) }
            )
        }

        composable(Routes.WALLETS) {
            WalletsScreen(viewModel = viewModel)
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateToTimeline = { dateStr -> 
                    navController.navigate("${Routes.TIMELINE}/${android.net.Uri.encode(dateStr)}")
                }
            )
        }

        composable(Routes.STATS) {
            ReportsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.BUDGET_GOAL) {
            BudgetGoalScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SAVINGS_VAULT) {
            com.example.ui.screens.SavingsVaultScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToBankNotificationHistory = { navController.navigate(Routes.BANK_NOTIFICATION_HISTORY) },
                onNavigateToEvents = { navController.navigate(Routes.EVENTS) },
                onNavigateToStats = { navController.navigate(Routes.STATS) },
                onNavigateToSavings = { navController.navigate(Routes.SAVINGS_VAULT) },
                onNavigateToDebtBook = { navController.navigate(Routes.DEBT_BOOK) }
            )
        }

        composable(Routes.DEBT_BOOK) {
            com.example.ui.screens.DebtBookScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.EVENTS) {
            com.example.ui.components.EventManagementScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.BANK_NOTIFICATION_HISTORY) {
            BankNotificationHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Routes.TIMELINE}/{dateStr}",
            arguments = listOf(androidx.navigation.navArgument("dateStr") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val dateStr = backStackEntry.arguments?.getString("dateStr") ?: ""
            TimelineScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                initialDateStr = dateStr
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
