package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    modifier: Modifier = Modifier
) {
    val wallets by viewModel.dailyWallets.collectAsState()
    val transactions by viewModel.dailyTransactions.collectAsState()
    val activeMonth by viewModel.activeMonth.collectAsState()
    val savingsWallets by viewModel.savingsWallets.collectAsState()
    val budgets by viewModel.allBudgets.collectAsState()
    
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
        // --- Total Balance Section ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("total_balance_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Tổng số dư khả dụng",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = FormatHelper.formatVND(totalBalance),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Trending Up",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Income",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Thu nhập ($displayMonthShort)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = FormatHelper.formatVND(totalIncome),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Expense",
                                tint = Color(0xFFF44336),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Chi tiêu ($displayMonthShort)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = FormatHelper.formatVND(totalExpense),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
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
                            text = "Giao dịch chờ duyệt mới!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Phát hiện $pendingCount giao dịch mới từ tin ngân hàng cần bạn xác nhận.",
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

            androidx.compose.foundation.lazy.grid.LazyHorizontalGrid(
                rows = androidx.compose.foundation.lazy.grid.GridCells.Fixed(if (wallets.size > 2) 2 else 1),
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
                        currentMonthBudgets.take(3).forEach { budget ->
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
                val computedInsights = remember(transactions, currentMonthTransactions, totalIncome, totalExpense, wallets, savingsWallets, budgets, savingsGoals) {
                    val insightsList = mutableListOf<SmartSpendingInsight>()
                    val exps = transactions.filter { it.type == "EXPENSE" }
                    
                    val now = System.currentTimeMillis()
                    val MS_DAY = 24 * 60 * 60 * 1000L
                    
                    // --- 1. RISK ALERTS (Cảnh báo rủi ro) ---
                    // Ví đang âm / sắp âm
                    val lowestWallet = wallets.minByOrNull { it.balance }
                    if (lowestWallet != null) {
                        if (lowestWallet.balance <= 0.0) {
                            insightsList.add(
                                SmartSpendingInsight(
                                    title = "Cảnh báo: Ví '${lowestWallet.name}' đang cạn kiệt",
                                    description = "Số dư hiện tại là ${FormatHelper.formatVND(lowestWallet.balance)}. Cần nạp thêm tiền để duy trì chi tiêu thiết yếu.",
                                    icon = Icons.Default.Warning,
                                    tint = Color(0xFFF44336), 
                                    score = 110 // Highest priority
                                )
                            )
                        } else if (lowestWallet.balance < 500000.0) {
                            insightsList.add(
                                SmartSpendingInsight(
                                    title = "Lưu ý: Số dư '${lowestWallet.name}' ở mức thấp",
                                    description = "Chỉ còn lại ${FormatHelper.formatVND(lowestWallet.balance)}. Hãy cẩn trọng với các khoản mua sắm tùy hứng nhé.",
                                    icon = Icons.Default.Info,
                                    tint = Color(0xFFFF9800),
                                    score = 80
                                )
                            )
                        }
                    }

                    // Vượt ngân sách
                    budgets.forEach { bug ->
                        val spent = currentMonthTransactions.filter { it.categoryName == bug.categoryName && it.type == "EXPENSE" }.sumOf { it.amount }
                        if (bug.limitAmount > 0) {
                            val ratio = spent / bug.limitAmount
                            if (ratio >= 1.0) {
                                insightsList.add(
                                    SmartSpendingInsight(
                                        title = "Đã vượt ngân sách '${bug.categoryName}'",
                                        description = "Tháng này bạn đã dùng ${FormatHelper.formatVND(spent)}, vượt ${FormatHelper.formatVND(spent - bug.limitAmount)} so với hạn mức.",
                                        icon = Icons.Default.Report,
                                        tint = Color(0xFFD32F2F),
                                        score = 105
                                    )
                                )
                            } else if (ratio >= 0.8) {
                                insightsList.add(
                                    SmartSpendingInsight(
                                        title = "Chạm ngưỡng ngân sách '${bug.categoryName}'",
                                        description = "Đã tiêu ${(ratio * 100).toInt()}% hạn mức tháng. Giảm nhịp độ chi tiêu ngay thôi!",
                                        icon = Icons.Default.AvTimer,
                                        tint = Color(0xFFFF9800),
                                        score = 90
                                    )
                                )
                            }
                        }
                    }

                    // Cashflow alert
                    if (totalIncome > 0.0) {
                        val svRatio = (totalIncome - totalExpense) / totalIncome
                        if (svRatio < 0.0) {
                            insightsList.add(
                                SmartSpendingInsight(
                                    title = "Dòng tiền tháng này đang âm!",
                                    description = "Tốc độ chi tiêu đang vượt xa thu nhập. Hãy rà soát lại các khoản chi không cần thiết nhé.",
                                    icon = Icons.Default.TrendingDown,
                                    tint = Color(0xFFF44336),
                                    score = 100
                                )
                            )
                        } else if (svRatio >= 0.2) {
                            insightsList.add(
                                SmartSpendingInsight(
                                    title = "Dòng tiền khỏe mạnh",
                                    description = "Bạn đã giữ lại được ${(svRatio * 100).toInt()}% thu nhập tháng này.",
                                    icon = Icons.Default.ThumbUp,
                                    tint = Color(0xFF4CAF50),
                                    score = 95
                                )
                            )
                        }
                    } else if (totalExpense > 0.0) {
                        insightsList.add(
                            SmartSpendingInsight(
                                title = "Chưa có nguồn thu trong tháng",
                                description = "Bạn đã chi ${FormatHelper.formatVND(totalExpense)} ra nhưng chưa có khoản tiền vào.",
                                icon = Icons.Default.TrendingDown,
                                tint = Color(0xFFFF9800),
                                score = 75
                            )
                        )
                    }

                    // --- 2. GOALS & TÍCH LŨY (Mục tiêu) ---
                    val nearGoalvault = savingsGoals.find { 
                        it.targetAmount > 0.0 && 
                        (it.currentAmount / it.targetAmount) >= 0.8 && it.currentAmount < it.targetAmount 
                    }
                    if (nearGoalvault != null) {
                        val remaining = nearGoalvault.targetAmount - nearGoalvault.currentAmount
                        insightsList.add(
                            SmartSpendingInsight(
                                title = "Mục tiêu '${nearGoalvault.name}' sắp hoàn thành",
                                description = "Tuyệt vời! Chỉ còn ${FormatHelper.formatVND(remaining)} nữa là bạn chạm đích. Tiếp tục phát huy!",
                                icon = Icons.Default.EmojiEvents,
                                tint = Color(0xFF4CAF50),
                                score = 98
                            )
                        )
                    }

                    val completedVault = savingsGoals.find { it.targetAmount > 0.0 && it.currentAmount >= it.targetAmount }
                    if (completedVault != null) {
                        insightsList.add(
                            SmartSpendingInsight(
                                title = "Thành tựu: Đạt mục tiêu '${completedVault.name}'",
                                description = "Chúc mừng! Bạn đã hoàn thành 100% mục tiêu tích lũy này.",
                                icon = Icons.Default.Celebration,
                                tint = Color(0xFF9C27B0),
                                score = 85
                            )
                        )
                    }

                    // --- 3. PHÂN TÍCH HÀNH VI CHI TIÊU & XU HƯỚNG ---
                    if (exps.isNotEmpty()) {
                        // So sánh tuần này & trước
                        val cWeek = exps.filter { now - it.timestamp <= 7 * MS_DAY }.sumOf { it.amount }
                        val pWeek = exps.filter { 
                            val diff = now - it.timestamp
                            diff > 7 * MS_DAY && diff <= 14 * MS_DAY 
                        }.sumOf { it.amount }

                        if (pWeek > 0.0) {
                            val diffPercent = ((cWeek - pWeek) / pWeek * 100).toInt()
                            if (diffPercent < 0) {
                                insightsList.add(
                                    SmartSpendingInsight(
                                        title = "Tiết chế chi tiêu tốt",
                                        description = "Tuần này bạn tiêu ít hơn tuần trước ${-diffPercent}%. Rất đáng khen ngợi!",
                                        icon = Icons.Default.TrendingDown,
                                        tint = Color(0xFF4CAF50),
                                        score = 90
                                    )
                                )
                            } else if (diffPercent > 30) {
                                insightsList.add(
                                    SmartSpendingInsight(
                                        title = "Chi tiêu tăng vọt bất thường",
                                        description = "Tuần này chi phí của bạn tăng đột biến ${diffPercent}% so với tuần trước. Hãy kiểm tra lại lịch sử giao dịch.",
                                        icon = Icons.Default.SsidChart,
                                        tint = Color(0xFFE91E63),
                                        score = 88
                                    )
                                )
                            }
                        }

                        // Khoản chi định kỳ (Recurring bills)
                        val specNotes = exps.filter { it.note.isNotBlank() && now - it.timestamp <= 30 * MS_DAY }
                            .groupBy { it.note.trim().lowercase() }
                            .mapValues { it.value.size }

                        val topNoteMatch = specNotes.entries.filter { it.value >= 3 }.maxByOrNull { it.value }
                        if (topNoteMatch != null) {
                            val item = exps.first { it.note.trim().lowercase() == topNoteMatch.key }
                            insightsList.add(
                                SmartSpendingInsight(
                                    title = "Phát hiện khoản chi lặp lại",
                                    description = "Giao dịch '${item.note}' xuất hiện thường xuyên. Hãy chắc chắn bạn đã chuẩn bị đủ số dư cho các kỳ tiếp theo.",
                                    icon = Icons.Default.Sync,
                                    tint = Color(0xFF2196F3),
                                    score = 75
                                )
                            )
                        }
                    } else {
                        insightsList.add(
                            SmartSpendingInsight(
                                title = "Khởi tạo dữ liệu sinh trắc",
                                description = "Thêm các khoản chi tiêu để AI học hỏi và đưa ra những gợi ý phù hợp nhất.",
                                icon = Icons.Default.Lightbulb,
                                tint = Color(0xFFA0A0A0)
                            )
                        )
                    }

                    insightsList.sortedByDescending { it.score }.take(3)
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
                        val sp = currentMonthTransactions.filter { it.categoryName == b.categoryName && it.type == "EXPENSE" }.sumOf { it.amount }
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

                computedInsights.forEachIndexed { idx, ins ->
                    if (idx > 0) {
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(ins.tint.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ins.icon,
                                contentDescription = ins.title,
                                tint = ins.tint,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ins.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ins.description,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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


