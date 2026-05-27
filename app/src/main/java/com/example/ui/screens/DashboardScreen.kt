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
import com.example.data.Transaction
import com.example.data.Wallet
import com.example.ui.FinanceViewModel
import com.example.ui.FormatHelper
import com.example.ui.IconMapper
import java.util.Calendar

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToWallets: () -> Unit,
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
            Text(
                text = "Tài khoản của tôi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Xem tất cả",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onNavigateToWallets() }
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
            val walletChunks = remember(wallets) { wallets.chunked(4) }
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            val chunkWidth = remember(walletChunks.size, screenWidth) {
                if (walletChunks.size > 1) screenWidth - 48.dp else screenWidth - 32.dp
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 0.dp),
                modifier = Modifier.fillMaxWidth().testTag("dashboard_wallets_row")
            ) {
                items(walletChunks) { chunk ->
                    WalletChunkGrid(
                        chunk = chunk,
                        onWalletClick = onNavigateToWallets,
                        modifier = Modifier.width(chunkWidth)
                    )
                }
            }
        }

        // --- Visual Gauge Visual Component ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("stats_donut_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .clickable { onNavigateToStats() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular Spend Gauge using Canvas
                val expenseRatio = remember(totalIncome, totalExpense) {
                    if (totalIncome > 0) (totalExpense / totalIncome).coerceIn(0.0, 1.0).toFloat()
                    else if (totalExpense > 0) 1f
                    else 0f
                }

                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    val strokeColor = MaterialTheme.colorScheme.error

                    Canvas(modifier = Modifier.size(60.dp)) {
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 16f)
                        )
                        drawArc(
                            color = strokeColor,
                            startAngle = -90f,
                            sweepAngle = expenseRatio * 360f,
                            useCenter = false,
                            style = Stroke(width = 16f)
                        )
                    }
                    Text(
                        text = "${(expenseRatio * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tỷ lệ chi tiêu tháng này",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bạn đã chi tiêu ${((expenseRatio) * 100).toInt()}% trong tổng số thu nhập tháng này.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- Budget Goals Shortcut ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToBudget() }
                .testTag("dashboard_budget_shortcut_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = "Budget",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hạn mức chi tiêu",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Đặt giới hạn chi tiêu và theo dõi mức chi dùng hàng tháng.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Detail",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- Kho Tiết Kiệm (Savings goals) Component ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSavings() }
                .testTag("dashboard_savings_vault_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "Kho tiết kiệm",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kho tiết kiệm",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Theo dõi và tích lũy để đạt được các mục tiêu tài chính.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Detail",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                val computedInsights = remember(transactions, currentMonthTransactions, totalIncome, totalExpense) {
                    val insightsList = mutableListOf<SmartSpendingInsight>()
                    val exps = transactions.filter { it.type == "EXPENSE" }
                    
                    if (exps.isEmpty()) {
                        insightsList.add(
                            SmartSpendingInsight(
                                title = "Khởi tạo dữ liệu sinh trắc",
                                description = "Gợi ý thông minh sẽ tự động học hỏi khi bạn có giao dịch chi tiêu.",
                                icon = Icons.Default.Lightbulb,
                                tint = Color(0xFFA0A0A0)
                            )
                        )
                        return@remember insightsList
                    }

                    val now = System.currentTimeMillis()
                    val MS_DAY = 24 * 60 * 60 * 1000L

                    // 1. Difference trend
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
                                    title = "Xu hướng tuần này giảm tốt",
                                    description = "Chi tiêu tuần này giảm ${-diffPercent}% so với tuần trước. Bạn tiết kiệm giỏi hơn!",
                                    icon = Icons.Default.TrendingDown,
                                    tint = Color(0xFF4CAF50),
                                    score = 90
                                )
                            )
                        } else if (diffPercent > 0) {
                            insightsList.add(
                                SmartSpendingInsight(
                                    title = "Chi tiêu tăng so với tuần trước",
                                    description = "Chi tiêu tuần này đã tăng ${diffPercent}% so với tuần trước. Hãy cân đối bớt thói quen tiêu vặt.",
                                    icon = Icons.Default.TrendingUp,
                                    tint = Color(0xFFF44336),
                                    score = 85
                                )
                            )
                        }
                    }

                    // 2. Daily hour habit detecting
                    val hbMap = mutableMapOf<Pair<String, String>, MutableList<Transaction>>()
                    val cl = Calendar.getInstance()
                    exps.forEach { tx ->
                        cl.timeInMillis = tx.timestamp
                        val hr = cl.get(Calendar.HOUR_OF_DAY)
                        val period = when {
                            hr in 6..11 -> "buổi sáng (6h - 11h)"
                            hr in 12..17 -> "buổi trưa chiều (12h - 17h)"
                            hr in 18..22 -> "buổi tối (18h - 22h)"
                            else -> "đêm muộn (23h - 5h)"
                        }
                        val pairKey = Pair(tx.categoryName, period)
                        hbMap.getOrPut(pairKey) { mutableListOf() }.add(tx)
                    }

                    val maxHabit = hbMap.entries.filter { it.value.size >= 2 }.maxByOrNull { it.value.size }
                    if (maxHabit != null) {
                        val meanVal = maxHabit.value.map { it.amount }.average()
                        insightsList.add(
                            SmartSpendingInsight(
                                title = "Khung giờ chi tiêu nổi bật",
                                description = "Bạn thường chi tiêu mức ${FormatHelper.formatVND(meanVal)} cho '${maxHabit.key.first}' vào ${maxHabit.key.second}.",
                                icon = Icons.Default.Schedule,
                                tint = Color(0xFF2196F3),
                                score = 80
                            )
                        )
                    }

                    // 3. Subscription/Recurring Keyword mapping (e.g., Cafe, Highlands, Grab, Netflix)
                    val specNotes = exps.filter { it.note.isNotBlank() }
                        .groupBy { it.note.trim().lowercase() }
                        .mapValues { it.value.size }

                    val topNoteMatch = specNotes.entries.filter { it.value >= 2 }.maxByOrNull { it.value }
                    if (topNoteMatch != null) {
                        val item = exps.first { it.note.trim().lowercase() == topNoteMatch.key }
                        insightsList.add(
                            SmartSpendingInsight(
                                title = "Hành vi lặp lại thường xuyên",
                                description = "Ghi chú '${item.note}' được bạn thanh toán đều đặn lặp lại ${topNoteMatch.value} lần vừa qua.",
                                icon = Icons.Default.Sync,
                                tint = Color(0xFF9C27B0),
                                score = 75
                            )
                        )
                    }

                    // 4. Monthly High Outlier
                    val catSums = currentMonthTransactions.filter { it.type == "EXPENSE" }
                        .groupBy { it.categoryName }
                        .mapValues { it.value.sumOf { tx -> tx.amount } }

                    val maxCat = catSums.maxByOrNull { it.value }
                    if (maxCat != null && maxCat.value > 0) {
                        insightsList.add(
                            SmartSpendingInsight(
                                title = "Hạng mục tiêu dùng lớn nhất",
                                description = "Hạng mục '${maxCat.key}' đang tiêu tốn ngân sách lớn nhất tháng này (${FormatHelper.formatVND(maxCat.value)}).",
                                icon = Icons.Default.PieChart,
                                tint = Color(0xFFE91E63),
                                score = 70
                            )
                        )
                    }

                    // 5. Total saving rates metrics
                    if (totalIncome > 0.0) {
                        val svRatio = (totalIncome - totalExpense) / totalIncome
                        if (svRatio >= 0.2) {
                            insightsList.add(
                                SmartSpendingInsight(
                                    title = "Tích lũy tài sản tốt",
                                    description = "Dòng tiền dương! Bạn đã tiết kiệm thành công ${(svRatio * 100).toInt()}% thu nhập tháng này.",
                                    icon = Icons.Default.ThumbUp,
                                    tint = Color(0xFF4CAF50),
                                    score = 95
                                )
                            )
                        } else if (svRatio < 0.0) {
                            insightsList.add(
                                SmartSpendingInsight(
                                    title = "Báo động dòng tiền âm",
                                    description = "Tổng chi tiêu vượt quá mức thu nhập trong tháng. Vui lòng dừng mua sắm bất chợt để tránh thâm hụt tài chính.",
                                    icon = Icons.Default.Warning,
                                    tint = Color(0xFFF44336),
                                    score = 100
                                )
                            )
                        }
                    }

                    insightsList.sortedByDescending { it.score }.take(3)
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

        // --- Recent Transactions ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Giao dịch gần đây",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Toàn bộ",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onNavigateToHistory() }
                    .testTag("view_all_transactions_button")
            )
        }

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Chưa có giao dịch nào được nhập",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("dashboard_recent_transactions")
            ) {
                transactions.take(5).forEach { tx ->
                    RecentTransactionItem(tx = tx)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun WalletMiniCard(
    wallet: Wallet,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() }
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

@Composable
fun RecentTransactionItem(
    tx: Transaction,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(FormatHelper.parseColor(tx.categoryColor)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = IconMapper.getIconByName(tx.categoryIcon),
                contentDescription = tx.categoryName,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.note,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = tx.walletName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "•",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${FormatHelper.formatDate(tx.timestamp)} ${java.text.SimpleDateFormat("HH:mm", java.util.Locale("vi", "VN")).format(tx.timestamp)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = if (tx.type == "EXPENSE") "-${FormatHelper.formatVND(tx.amount)}"
                   else "+${FormatHelper.formatVND(tx.amount)}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (tx.type == "EXPENSE") Color(0xFFF44336) else Color(0xFF4CAF50)
        )
    }
}

@Composable
fun WalletChunkGrid(
    chunk: List<Wallet>,
    onWalletClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1 (Item 0 and Item 1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (chunk.isNotEmpty()) {
                WalletMiniCard(
                    wallet = chunk[0],
                    onClick = onWalletClick,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (chunk.size > 1) {
                WalletMiniCard(
                    wallet = chunk[1],
                    onClick = onWalletClick,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Row 2 (Item 2 and Item 3)
        if (chunk.size > 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WalletMiniCard(
                    wallet = chunk[2],
                    onClick = onWalletClick,
                    modifier = Modifier.weight(1f)
                )

                if (chunk.size > 3) {
                    WalletMiniCard(
                        wallet = chunk[3],
                        onClick = onWalletClick,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
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


