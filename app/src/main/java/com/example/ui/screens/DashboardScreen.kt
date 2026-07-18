package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.grid.items
import com.example.data.Transaction
import com.example.data.Wallet
import com.example.ui.FinanceViewModel
import com.example.ui.FormatHelper
import com.example.ui.IconMapper
import com.example.ui.components.StripedProgressIndicator
import java.util.Calendar

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToWallets: (Wallet?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToSavings: () -> Unit = {},
    onNavigateToBankNotifications: () -> Unit = {},
    onNavigateToDebtBook: () -> Unit = {},
    onNavigateToAIAdvisor: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val wallets by viewModel.dailyWallets.collectAsState()
    val transactions by viewModel.dailyTransactions.collectAsState()
    val activeMonth by viewModel.activeMonth.collectAsState()
    val savingsWallets by viewModel.savingsWallets.collectAsState()
    val budgets by viewModel.allBudgets.collectAsState()
    val events by viewModel.allEvents.collectAsState()
    
    var eventToView by remember { mutableStateOf<com.example.data.Event?>(null) }

    val currentMonthBudgets = remember(budgets, activeMonth) {
        budgets.filter { it.month == activeMonth }
    }
    val savingsGoals by viewModel.allSavingsGoals.collectAsState()

    val totalBalance = wallets.sumOf { it.balance }

    // Filter current month transactions for stats - optimized to reuse a single Calendar instance
    val currentMonthTransactions = remember(transactions, activeMonth) {
        val cal = Calendar.getInstance()
        transactions.filter {
            cal.timeInMillis = it.timestamp
            val txMonth = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            txMonth == activeMonth
        }
    }

    val totalIncome = currentMonthTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = currentMonthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    val displayMonth = remember(activeMonth) {
        if (activeMonth.length >= 7) {
            "Tháng ${activeMonth.substring(5)}/${activeMonth.substring(0, 4)}"
        } else {
            activeMonth
        }
    }

    val displayMonthShort = remember(activeMonth) {
        if (activeMonth.length >= 7) {
            try {
                "T${activeMonth.substring(5).toInt()}"
            } catch (e: Exception) {
                "T${activeMonth.substring(5)}"
            }
        } else {
            activeMonth
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- Header Row with Title and Back Button ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại"
                    )
                }
            }
            Text(
                text = "Tổng quan",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        // --- Total Balance Section ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("total_balance_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Tổng số dư khả dụng",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = FormatHelper.formatVND(totalBalance),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = "Trending Up",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Income",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                        Column {
                            Text(
                                text = "Thu nhập ($displayMonthShort)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                            Text(
                                text = FormatHelper.formatVND(totalIncome),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Expense",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(14.dp)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Chi tiêu ($displayMonthShort)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                            Text(
                                text = FormatHelper.formatVND(totalExpense),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF44336)
                            )
                        }
                    }
                }
            }
        }

        // --- Pending Notifications Banner Alert ---
        val notificationLogs by viewModel.notificationLogs.collectAsState()
        val pendingCount = remember(notificationLogs) { notificationLogs.count { it.status == "PENDING" } }
        if (pendingCount > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToBankNotifications() }
                    .testTag("dashboard_pending_notifications_banner"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFFE0B2), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Pending Notifications",
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Có giao dịch mới!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Phát hiện $pendingCount giao dịch mới.",
                            fontSize = 12.sp,
                            color = Color(0xFF5D4037)
                        )
                    }
                    Button(
                        onClick = onNavigateToBankNotifications,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Duyệt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Active Debts Alert Banner ---
        val allDebts by viewModel.allDebts.collectAsState()
        val activeDebts = remember(allDebts) { allDebts.filter { it.status == "ACTIVE" && it.remainingAmount > 0 } }
        if (activeDebts.isNotEmpty()) {
            val totalDebt = activeDebts.filter { it.type == "DEBT" }.sumOf { it.remainingAmount }
            val totalLoan = activeDebts.filter { it.type == "LOAN" }.sumOf { it.remainingAmount }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToDebtBook() }
                    .testTag("dashboard_active_debts_banner"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFBE9E7)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFD84315).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFFCCBC), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Active Debts",
                            tint = Color(0xFFD84315),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bạn có khoản nợ chưa hoàn thành!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD84315)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Đi vay: ${FormatHelper.formatVND(totalDebt)} - Cho vay: ${FormatHelper.formatVND(totalLoan)}",
                            fontSize = 12.sp,
                            color = Color(0xFF5D4037)
                        )
                    }
                }
            }
        }

        // --- AI Financial Advisor Banner ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToAIAdvisor() }
                .testTag("dashboard_ai_advisor_banner"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Cố vấn AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Trợ lý AI",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // --- Events Widget ---
        val ongoingEvents = events.filter {
            val now = System.currentTimeMillis()
            now >= it.startDate && (it.endDate == null || now <= it.endDate + 86400000L - 1)
        }
        if (ongoingEvents.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sự kiện đang diễn ra",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(ongoingEvents) { event ->
                    val eventTransactions = viewModel.allTransactions.collectAsState().value.filter { it.eventId == event.id }
                    val spentAmount = eventTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    val progress = if ((event.limitAmount ?: 0.0) > 0.0) {
                        (spentAmount / event.limitAmount!!).toFloat().coerceIn(0f, 1f)
                    } else 0f
                    
                    Card(
                        modifier = Modifier
                            .width(280.dp)
                            .clickable { eventToView = event }
                            .testTag("dashboard_event_card_${event.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val eventColor = try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(event.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                                Icon(Icons.Default.Event, contentDescription = null, tint = eventColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(event.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = eventColor)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            if ((event.limitAmount ?: 0.0) > 0.0) {
                                Text("Đã chi: ${FormatHelper.formatVND(spentAmount)} / ${FormatHelper.formatVND(event.limitAmount!!)}", fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                StripedProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                                    color = if (progress >= 0.9f) MaterialTheme.colorScheme.error else try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(event.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary },
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            } else {
                                Text("Đã chi: ${FormatHelper.formatVND(spentAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // --- Wallets Section ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tài khoản của tôi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (wallets.size > 4) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Vuốt ngang để xem thêm ví",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "Xem tất cả",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onNavigateToWallets(null) }
                    .testTag("view_all_wallets_button")
            )
        }

        if (wallets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Không có ví nào. Thêm ví mới!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            val chunkWidth = if (wallets.size > 4) (screenWidth - 48.dp) / 2 else (screenWidth - 32.dp) / 2

            val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
            
            val pagesCount = remember(wallets) {
                if (wallets.size <= 4) 1
                else {
                    val totalCols = (wallets.size + 1) / 2
                    totalCols - 1
                }
            }

            val currentColumn by remember {
                derivedStateOf {
                    val layoutInfo = gridState.layoutInfo
                    val visibleItemsInfo = layoutInfo.visibleItemsInfo
                    if (visibleItemsInfo.isEmpty()) 0
                    else {
                        val firstItem = visibleItemsInfo.first()
                        val lastItem = visibleItemsInfo.last()
                        val totalItems = layoutInfo.totalItemsCount
                        val totalCols = (totalItems + 1) / 2
                        
                        val isAtStart = gridState.firstVisibleItemIndex == 0
                        val isAtEnd = lastItem.index >= totalItems - 1
                        
                        if (isAtStart) 0
                        else if (isAtEnd) {
                            if (totalCols - 2 >= 0) totalCols - 2 else 0
                        } else {
                            val firstVisibleCol = gridState.firstVisibleItemIndex / 2
                            val firstItemOffset = firstItem.offset.x
                            if (Math.abs(firstItemOffset) > firstItem.size.width / 2) {
                                val target = firstVisibleCol + 1
                                if (target <= totalCols - 2) target else firstVisibleCol
                            } else {
                                firstVisibleCol
                            }
                        }
                    }
                }
            }

            androidx.compose.foundation.lazy.grid.LazyHorizontalGrid(
                rows = androidx.compose.foundation.lazy.grid.GridCells.Fixed(if (wallets.size > 2) 2 else 1),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (wallets.size > 2) 228.dp else 110.dp) // Exact height: 110dp per card + 8dp spacing
                    .testTag("dashboard_wallets_row")
            ) {
                items(wallets) { wallet ->
                    WalletMiniCard(
                        wallet = wallet,
                        onClick = onNavigateToWallets,
                        modifier = Modifier.width(chunkWidth)
                    )
                }
            }

            if (pagesCount > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pagesCount) { index ->
                        val isSelected = currentColumn == index
                        val size = if (isSelected) 8.dp else 5.dp
                        val color = if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(size)
                                .clip(CircleShape)
                                .background(color)
                                .animateContentSize()
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Visual Gauge Visual Component ---
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToStats() }
                    .testTag("stats_donut_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val expenseRatio = remember(totalIncome, totalExpense) {
                        if (totalIncome > 0) (totalExpense / totalIncome).coerceIn(0.0, 1.0).toFloat()
                        else if (totalExpense > 0) 1f
                        else 0f
                    }

                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant
                        val strokeColor = MaterialTheme.colorScheme.error

                        Canvas(modifier = Modifier.size(36.dp)) {
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 8f)
                            )
                            drawArc(
                                color = strokeColor,
                                startAngle = -90f,
                                sweepAngle = expenseRatio * 360f,
                                useCenter = false,
                                style = Stroke(width = 8f)
                            )
                        }
                        Text(
                            text = "${(expenseRatio * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Tỷ lệ\nchi tiêu",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
            }

            // --- Kho Tiết Kiệm Component ---
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToSavings() }
                    .testTag("dashboard_savings_vault_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "Kho tiết kiệm",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Kho\ntiết kiệm",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // --- Budget Goals Shortcut ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToBudget() }
                .testTag("dashboard_budget_shortcut_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = "Budget",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Hạn mức chi tiêu",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Detail",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (currentMonthBudgets.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        currentMonthBudgets.forEach { budget ->
                            val progress = if (budget.limitAmount > 0) (budget.spentAmount / budget.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
                            val isExceeded = budget.spentAmount >= budget.limitAmount
                            val progressColor = if (isExceeded) MaterialTheme.colorScheme.error else FormatHelper.parseColor(budget.categoryColor)

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = budget.categoryName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${FormatHelper.formatVND(budget.spentAmount)} / ${FormatHelper.formatVND(budget.limitAmount)}",
                                        fontSize = 11.sp,
                                        color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                StripedProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = progressColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Smart Spending Analysis Component ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_smart_insights_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header of Insights
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = "Trợ lý thông minh",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Phân tích chi tiêu thông minh",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AI Trợ Lý",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Mini Trend Chart / Progress comparing This Week vs Last Week
                val expenses = remember(transactions) { transactions.filter { it.type == "EXPENSE" } }
                val anchorTime = remember(transactions) { System.currentTimeMillis() }
                val MS_IN_DAY = 24 * 60 * 60 * 1000L

                val thisWeekTotal = remember(expenses, anchorTime) {
                    expenses.filter { anchorTime - it.timestamp <= 7 * MS_IN_DAY }.sumOf { it.amount }
                }
                val lastWeekTotal = remember(expenses, anchorTime) {
                    expenses.filter {
                        val diff = anchorTime - it.timestamp
                        diff > 7 * MS_IN_DAY && diff <= 14 * MS_IN_DAY
                    }.sumOf { it.amount }
                }

                if (expenses.isNotEmpty() && (thisWeekTotal > 0.0 || lastWeekTotal > 0.0)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "So sánh xu hướng chi tiêu tuần này",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Dual Bar visualization
                        val maxTotal = Math.max(thisWeekTotal, lastWeekTotal)
                        val thisWeekRatio = if (maxTotal > 0) (thisWeekTotal / maxTotal).toFloat() else 0f
                        val lastWeekRatio = if (maxTotal > 0) (lastWeekTotal / maxTotal).toFloat() else 0f

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Last week
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Tuần trước",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = FormatHelper.formatVND(lastWeekTotal),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(lastWeekRatio.coerceIn(0.01f, 1f))
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), CircleShape)
                                    )
                                }
                            }

                            // This week
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Tuần này",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = FormatHelper.formatVND(thisWeekTotal),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (thisWeekTotal > lastWeekTotal) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(thisWeekRatio.coerceIn(0.01f, 1f))
                                            .fillMaxHeight()
                                            .background(
                                                if (thisWeekTotal > lastWeekTotal) MaterialTheme.colorScheme.error
                                                else Color(0xFF4CAF50),
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                // Insights section list
                val aiAdvisorData = remember(transactions, currentMonthTransactions, wallets, savingsWallets, currentMonthBudgets, savingsGoals, totalIncome, totalExpense) {
                    val riskAlerts = mutableListOf<SmartSpendingInsight>()
                    val recommendations = mutableListOf<SmartSpendingInsight>()
                    val evaluations = mutableListOf<SmartSpendingInsight>()

                    val now = System.currentTimeMillis()
                    val oneDay = 24L * 60L * 60L * 1000L
                    
                    val cal = Calendar.getInstance()
                    val daysElapsed = cal.get(Calendar.DAY_OF_MONTH)
                    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val remainingDays = maxDays - daysElapsed + 1
                    
                    val daysElapsedSafe = if (daysElapsed > 0) daysElapsed.toDouble() else 1.0

                    // --- 1. RISK ALERTS (Cảnh báo rủi ro) ---
                    val nonCreditNonSavingsBalance = wallets.filter { it.type != "CREDIT" && it.type != "SAVINGS" }.sumOf { it.balance }
                    val monthlyExpense = currentMonthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    val dailyExpenseRate = monthlyExpense / daysElapsedSafe
                    
                    if (dailyExpenseRate > 0 && nonCreditNonSavingsBalance > 0) {
                        val daysToDry = nonCreditNonSavingsBalance / dailyExpenseRate
                        if (daysToDry <= 7) {
                            riskAlerts.add(
                                SmartSpendingInsight(
                                    title = "Tài khoản sắp cạn tiền!",
                                    description = "Với tốc độ chi tiêu trung bình ${FormatHelper.formatVND(dailyExpenseRate)}/ngày, tài khoản khả dụng của bạn có thể bị cạn kiệt trong ${daysToDry.toInt()} ngày tới.",
                                    icon = Icons.Default.Warning,
                                    tint = Color(0xFFF44336)
                                )
                            )
                        }
                    }

                    currentMonthBudgets.forEach { bug ->
                        val spent = bug.spentAmount
                        val limit = bug.limitAmount
                        if (limit > 0) {
                            val remaining = limit - spent
                            val dailySpent = spent / daysElapsedSafe
                            
                            if (spent >= limit) {
                                riskAlerts.add(
                                    SmartSpendingInsight(
                                        title = "Đã vượt hạn mức '${bug.categoryName}'",
                                        description = "Bạn đã chi tiêu ${FormatHelper.formatVND(spent)}, vượt ${FormatHelper.formatVND(spent - limit)} so với hạn mức ngân sách tháng.",
                                        icon = Icons.Default.Report,
                                        tint = Color(0xFFD32F2F)
                                    )
                                )
                            } else if (dailySpent > 0) {
                                val daysToExhaust = remaining / dailySpent
                                if (daysToExhaust > 0 && daysToExhaust <= 3) {
                                    riskAlerts.add(
                                        SmartSpendingInsight(
                                            title = "Dự đoán vượt hạn mức '${bug.categoryName}'",
                                            description = "Với thói quen hiện tại, chi phí cho '${bug.categoryName}' dự kiến sẽ vượt hạn mức ngân sách trong ${daysToExhaust.toInt()} ngày tới.",
                                            icon = Icons.Default.TrendingUp,
                                            tint = Color(0xFFE57373)
                                        )
                                    )
                                }
                            }
                        }
                    }
                    
                    if (riskAlerts.isEmpty()) {
                        riskAlerts.add(
                            SmartSpendingInsight(
                                title = "Chưa phát hiện rủi ro",
                                description = "Tình hình chi tiêu trong tháng của bạn hiện tại vẫn nằm trong ngưỡng an toàn.",
                                icon = Icons.Default.CheckCircle,
                                tint = Color(0xFF4CAF50)
                            )
                        )
                    }

                    // --- 2. RECOMMENDATIONS (Khuyến nghị) ---
                    currentMonthBudgets.forEach { bug ->
                        val limit = bug.limitAmount
                        val spent = bug.spentAmount
                        if (limit > 0) {
                            val remaining = limit - spent
                            if (remaining > 0) {
                                val dailyLimit = remaining / remainingDays
                                recommendations.add(
                                    SmartSpendingInsight(
                                        title = "Định mức '${bug.categoryName}' khuyên dùng",
                                        description = "Bạn nên duy trì chi tiêu '${bug.categoryName}' trung bình khoảng ${FormatHelper.formatVND(dailyLimit)}/ngày từ nay đến cuối tháng.",
                                        icon = Icons.Default.Lightbulb,
                                        tint = Color(0xFFFFB300)
                                    )
                                )
                            } else {
                                recommendations.add(
                                    SmartSpendingInsight(
                                        title = "Lời khuyên về '${bug.categoryName}'",
                                        description = "Ngân sách tháng cho '${bug.categoryName}' đã hết sạch. Hãy tạm hoãn mọi khoản chi tiêu cho danh mục này.",
                                        icon = Icons.Default.Block,
                                        tint = Color(0xFFE53935)
                                    )
                                )
                            }
                        }
                    }
                    
                    if (recommendations.isEmpty()) {
                        recommendations.add(
                            SmartSpendingInsight(
                                title = "Chưa có khuyến nghị",
                                description = "Hãy tạo Hạn mức chi tiêu trong mục ngân sách để trợ lý AI tính toán lời khuyên định mức.",
                                icon = Icons.Default.Lightbulb,
                                tint = Color(0xFF78909C)
                            )
                        )
                    }

                    // --- 3. EVALUATIONS (Đánh giá xu hướng) ---
                    val exps = transactions.filter { it.type == "EXPENSE" }
                    if (exps.isNotEmpty()) {
                        val cWeek = exps.filter { now - it.timestamp <= 7 * oneDay }.sumOf { it.amount }
                        val pWeek = exps.filter { 
                            val diff = now - it.timestamp
                            diff > 7 * oneDay && diff <= 14 * oneDay 
                        }.sumOf { it.amount }

                        val allCategoryNames = transactions.map { it.categoryName }.distinct()
                        var foundCatEval = false
                        for (catName in allCategoryNames) {
                            val cCatSpent = exps.filter { it.categoryName == catName && now - it.timestamp <= 7 * oneDay }.sumOf { it.amount }
                            val pCatSpent = exps.filter { 
                                it.categoryName == catName && (now - it.timestamp) > 7 * oneDay && (now - it.timestamp) <= 14 * oneDay 
                            }.sumOf { it.amount }
                            
                            if (pCatSpent > 0.0) {
                                val change = ((pCatSpent - cCatSpent) / pCatSpent * 100).toInt()
                                if (change >= 20) {
                                    evaluations.add(
                                        SmartSpendingInsight(
                                            title = "Đánh giá: Giảm chi '${catName}'",
                                            description = "Chúc mừng! Bạn đã cắt giảm thành công chi phí nhóm '${catName}' đi ${change}% so với tuần trước.",
                                            icon = Icons.Default.TrendingDown,
                                            tint = Color(0xFF4CAF50)
                                        )
                                    )
                                    foundCatEval = true
                                } else if (change <= -30) {
                                    evaluations.add(
                                        SmartSpendingInsight(
                                            title = "Đánh giá: Tăng chi '${catName}'",
                                            description = "Cảnh báo: Chi phí dành cho nhóm '${catName}' đang tăng mạnh thêm ${-change}% so với tuần trước.",
                                            icon = Icons.Default.TrendingUp,
                                            tint = Color(0xFFF44336)
                                        )
                                    )
                                    foundCatEval = true
                                }
                            }
                        }
                        
                        if (!foundCatEval && pWeek > 0.0) {
                            val diffPercent = ((pWeek - cWeek) / pWeek * 100).toInt()
                            if (diffPercent >= 10) {
                                evaluations.add(
                                    SmartSpendingInsight(
                                        title = "Đánh giá chung: Tiết kiệm tốt",
                                        description = "Tổng chi tiêu tuần này giảm ${diffPercent}% so với tuần trước. Bạn quản lý ngân sách rất tốt!",
                                        icon = Icons.Default.CheckCircle,
                                        tint = Color(0xFF4CAF50)
                                    )
                                )
                            } else if (diffPercent <= -20) {
                                evaluations.add(
                                    SmartSpendingInsight(
                                        title = "Đánh giá chung: Tăng chi tiêu",
                                        description = "Tổng chi tiêu tuần này đang tăng ${-diffPercent}% so với tuần trước. Cần rà soát các khoản chi lớn.",
                                        icon = Icons.Default.TrendingUp,
                                        tint = Color(0xFFE91E63)
                                    )
                                )
                            }
                        }
                    }
                    
                    if (evaluations.isEmpty()) {
                        evaluations.add(
                            SmartSpendingInsight(
                                title = "Chưa có đánh giá",
                                description = "Tiếp tục chi tiêu và ghi chép để AI đánh giá và so sánh định kỳ giữa các tuần.",
                                icon = Icons.Default.History,
                                tint = Color(0xFF90A4AE)
                            )
                        )
                    }

                    Triple(riskAlerts, recommendations, evaluations)
                }

                // Financial Health Score UI
                val healthScore = remember(totalIncome, totalExpense, savingsWallets, budgets, savingsGoals) {
                    var score = 100
                    
                    if (totalIncome > 0) {
                        val svRatio = (totalIncome - totalExpense) / totalIncome
                        if (svRatio < 0) score -= 30
                        else if (svRatio < 0.1) score -= 15
                    } else if (totalExpense > 0) {
                        score -= 20
                    }

                    budgets.forEach { b ->
                        val sp = b.spentAmount
                        if (b.limitAmount > 0 && sp > b.limitAmount) score -= 10
                    }
                    
                    wallets.forEach { 
                        if (it.balance < 0) score -= 20 
                    }
                    
                    if (savingsWallets.isNotEmpty()) score += 10
                    
                    score.coerceIn(0, 100)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Điểm Sức Khỏe Tài Chính", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = when(healthScore) {
                                in 85..100 -> "Xuất sắc! Mood: Tự hào 🌟"
                                in 65..84 -> "Tốt! Mood: Thoải mái 😊"
                                in 40..64 -> "Trung bình. Mood: Cần chú ý 🤔"
                                else -> "Báo động! Mood: Căng thẳng 😰"
                            }, 
                            fontSize = 11.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$healthScore",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = when {
                            healthScore >= 80 -> Color(0xFF4CAF50)
                            healthScore >= 50 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }

                // 1. RISK ALERTS SECTION
                Text(
                    text = "⚠️ Cảnh báo rủi ro",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                aiAdvisorData.first.take(2).forEach { ins ->
                    InsightRow(ins)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)

                // 2. RECOMMENDATIONS SECTION
                Text(
                    text = "💡 Khuyến nghị",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                aiAdvisorData.second.take(2).forEach { ins ->
                    InsightRow(ins)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)

                // 3. EVALUATIONS SECTION
                Text(
                    text = "📊 Đánh giá xu hướng",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                aiAdvisorData.third.take(2).forEach { ins ->
                    InsightRow(ins)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (eventToView != null) {
        val event = eventToView!!
        val eventTransactions = viewModel.allTransactions.collectAsState().value.filter { it.eventId == event.id }
        val limit = event.limitAmount ?: 0.0
        val spent = eventTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        
        AlertDialog(
            onDismissRequest = { eventToView = null },
            title = { Text(text = event.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (event.description.isNotBlank()) {
                        Text(event.description, fontSize = 14.sp)
                    }
                    val startStr = FormatHelper.formatDate(event.startDate)
                    val endStr = event.endDate?.let { FormatHelper.formatDate(it) } ?: "Không tính"
                    Text("Thời gian: $startStr - $endStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

                    if (limit > 0) {
                        Text("Tiến độ chi tiêu:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        val progress = (spent / limit).toFloat().coerceIn(0f, 1f)
                        StripedProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color = if (progress >= 0.9f) MaterialTheme.colorScheme.error else try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(event.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "${FormatHelper.formatVND(spent)} / ${FormatHelper.formatVND(limit)}",
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    } else {
                        Text("Đã chi: ${FormatHelper.formatVND(spent)}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    }

                    if (eventTransactions.isNotEmpty()) {
                        Text("Lịch sử giao dịch liên quan:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(eventTransactions.sortedByDescending { it.timestamp }) { tx ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tx.categoryName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Text(FormatHelper.formatDate(tx.timestamp), fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    val isAdjustmentDecrease = tx.type == "ADJUSTMENT" && !tx.note.contains("tăng")
                                    Text(
                                        text = "${if (tx.type == "EXPENSE" || isAdjustmentDecrease) "-" else "+"}${FormatHelper.formatVND(tx.amount)}",
                                        color = if (tx.type == "EXPENSE" || isAdjustmentDecrease) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                androidx.compose.material3.HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    } else {
                        Text("Chưa có giao dịch.", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { eventToView = null }) {
                    Text("Đóng")
                }
            }
        )
    }
}

@Composable
fun WalletMiniCard(
    wallet: Wallet,
    onClick: (Wallet) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick(wallet) }
            .testTag("wallet_mini_${wallet.id}"),
        colors = CardDefaults.cardColors(
            containerColor = FormatHelper.parseColor(wallet.colorHex).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, FormatHelper.parseColor(wallet.colorHex).copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FormatHelper.parseColor(wallet.colorHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IconMapper.getIconByName(wallet.iconName),
                        contentDescription = wallet.name,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = wallet.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column {
                Text(
                    text = "Số dư",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FormatHelper.formatVND(wallet.balance),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

data class SmartSpendingInsight(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val score: Int = 0
)

@Composable
private fun InsightRow(ins: SmartSpendingInsight) {
    Row(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(ins.tint.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ins.icon,
                contentDescription = ins.title,
                tint = ins.tint,
                modifier = Modifier.size(14.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ins.title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = ins.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}


