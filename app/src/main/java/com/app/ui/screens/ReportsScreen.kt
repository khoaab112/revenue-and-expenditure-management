package com.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.data.Transaction
import com.app.ui.FinanceViewModel
import com.app.ui.FormatHelper
import com.app.ui.IconMapper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedMainTab by remember { mutableStateOf("TREND") } // "TREND" or "DISTRIBUTION"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // MAIN TAB SELECTOR ("Xu hướng" vs "Phân bổ")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedMainTab == "TREND") MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { selectedMainTab = "TREND" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Xu hướng",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedMainTab == "TREND") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedMainTab == "DISTRIBUTION") MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { selectedMainTab = "DISTRIBUTION" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Phân bổ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedMainTab == "DISTRIBUTION") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedMainTab == "TREND") {
            TrendReportContent(viewModel = viewModel)
        } else {
            DistributionReportContent(viewModel = viewModel)
        }
    }
}

// ==========================================
// TAB 1: XU HƯỚNG (TREND REPORT CONTENT)
// ==========================================
@Composable
fun TrendReportContent(viewModel: FinanceViewModel) {
    var selectedPeriod by remember { mutableStateOf("WEEK") } // "WEEK", "MONTH", "YEAR", "5YEARS"
    var periodOffset by remember { mutableStateOf(0) }

    val allTransactions by viewModel.dailyTransactions.collectAsState()
    val savingsWallets by viewModel.savingsWallets.collectAsState()
    val savingsWalletIds = remember(savingsWallets) { savingsWallets.map { it.id }.toSet() }

    // Calculate Date Range based on Period and Offset
    val (startDate, endDate, dateLabel) = remember(selectedPeriod, periodOffset) {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY

        when (selectedPeriod) {
            "WEEK" -> {
                cal.add(Calendar.WEEK_OF_YEAR, periodOffset)
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val label = "${sdf.format(Date(start))} - ${sdf.format(Date(end))}"
                Triple(start, end, label)
            }
            "MONTH" -> {
                cal.add(Calendar.MONTH, periodOffset)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, maxDay)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis

                val sdf = SimpleDateFormat("MM/yyyy", Locale.getDefault())
                val label = "Tháng ${sdf.format(Date(start))}"
                Triple(start, end, label)
            }
            "YEAR" -> {
                cal.add(Calendar.YEAR, periodOffset)
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
                cal.set(Calendar.DAY_OF_YEAR, maxDay)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis

                val sdf = SimpleDateFormat("yyyy", Locale.getDefault())
                val label = "Năm ${sdf.format(Date(start))}"
                Triple(start, end, label)
            }
            else -> { // 5YEARS
                cal.add(Calendar.YEAR, periodOffset * 5)
                val currentYear = cal.get(Calendar.YEAR)
                val startYear = currentYear - 4

                cal.set(Calendar.YEAR, startYear)
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.YEAR, currentYear)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
                cal.set(Calendar.DAY_OF_YEAR, maxDay)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis

                val label = "$startYear - $currentYear"
                Triple(start, end, label)
            }
        }
    }

    // Filter transactions for current period
    val periodTransactions = remember(allTransactions, startDate, endDate) {
        allTransactions.filter { it.timestamp in startDate..endDate }
    }

    // Filter transactions for previous period (for % comparison)
    val prevPeriodTransactions = remember(allTransactions, startDate, endDate, selectedPeriod) {
        val duration = endDate - startDate + 1
        val prevStart = startDate - duration
        val prevEnd = endDate - duration
        allTransactions.filter { it.timestamp in prevStart..prevEnd }
    }

    val currentSummary = remember(periodTransactions, savingsWalletIds) {
        com.app.ui.calculateRealFinancialSummary(periodTransactions, savingsWalletIds)
    }

    val prevSummary = remember(prevPeriodTransactions, savingsWalletIds) {
        com.app.ui.calculateRealFinancialSummary(prevPeriodTransactions, savingsWalletIds)
    }

    val totalIncome = currentSummary.realIncome
    val totalExpense = currentSummary.realExpense

    val prevIncome = prevSummary.realIncome
    val prevExpense = prevSummary.realExpense

    // Calculate percentage change
    val incomeChangePercent = remember(totalIncome, prevIncome) {
        if (prevIncome > 0) ((totalIncome - prevIncome) / prevIncome * 100) else 0.0
    }
    val expenseChangePercent = remember(totalExpense, prevExpense) {
        if (prevExpense > 0) ((totalExpense - prevExpense) / prevExpense * 100) else 0.0
    }

    val categoriesList by viewModel.categoriesList.collectAsState()

    fun getRootCategory(catName: String): com.app.data.FinanceCategory {
        var currentCat = categoriesList.find { it.name.equals(catName, ignoreCase = true) }
        while (currentCat?.parentName != null) {
            val parent = categoriesList.find { it.name.equals(currentCat!!.parentName, ignoreCase = true) }
            if (parent != null) {
                currentCat = parent
            } else {
                break
            }
        }
        return currentCat ?: com.app.data.FinanceCategory(catName, "Category", "#607D8B", "BOTH")
    }

    val topExpenseCategories = remember(periodTransactions, savingsWalletIds, totalExpense, categoriesList) {
        periodTransactions
            .filter { tx ->
                val catName = tx.categoryName.trim()
                val isAdjustment = tx.type == "ADJUSTMENT" || catName.contains("Điều chỉnh số dư", ignoreCase = true)
                val isInternalTransfer = tx.type == "TRANSFER"
                val isSavingsDeposit = catName.contains("Gửi tiết kiệm", ignoreCase = true) || catName.contains("Cất quỹ", ignoreCase = true) || (tx.destinationWalletId != null && tx.destinationWalletId in savingsWalletIds)
                tx.type == "EXPENSE" && !isAdjustment && !isInternalTransfer && !isSavingsDeposit
            }
            .groupBy { tx -> getRootCategory(tx.categoryName.trim()).name }
            .map { (catName, txs) ->
                val sum = txs.sumOf { it.amount }
                val rootCat = getRootCategory(catName)
                CategorySpend(
                    name = rootCat.name,
                    amount = sum,
                    colorHex = rootCat.colorHex,
                    iconName = rootCat.iconName,
                    percentage = if (totalExpense > 0) (sum / totalExpense * 100).toFloat() else 0f
                )
            }
            .sortedByDescending { it.amount }
            .take(3)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TIME PERIOD SUB-SELECTOR ("Tuần", "Tháng", "Năm", "5 năm")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val periods = listOf(
                "WEEK" to "Tuần",
                "MONTH" to "Tháng",
                "YEAR" to "Năm",
                "5YEARS" to "5 năm"
            )
            periods.forEach { (key, label) ->
                val selected = selectedPeriod == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable {
                            selectedPeriod = key
                            periodOffset = 0
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // DATE RANGE NAVIGATOR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { periodOffset -= 1 }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Trước", tint = MaterialTheme.colorScheme.onSurface)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = dateLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }

            IconButton(onClick = { periodOffset += 1 }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Sau", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        // TOP ROW SUMMARY CARDS (Tổng thu & Tổng chi)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val periodText = when (selectedPeriod) {
                "WEEK" -> "tuần"
                "MONTH" -> "tháng"
                "YEAR" -> "năm"
                else -> "kỳ"
            }

            // Card 1: Tổng thu
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF4FBF7)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2F3E9))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                            Text("Tổng thu", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B3A2B))
                        }
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFE2F3E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = FormatHelper.formatVND(totalIncome),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (incomeChangePercent >= 0) "↑ +${String.format("%.1f", incomeChangePercent)}% so với $periodText trước"
                        else "↓ ${String.format("%.1f", incomeChangePercent)}% so với $periodText trước",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (incomeChangePercent >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            // Card 2: Tổng chi
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF2F2)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFFEBEE))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFE53935)))
                            Text("Tổng chi", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E1E1E))
                        }
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = FormatHelper.formatVND(totalExpense),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE53935)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (expenseChangePercent <= 0) "↓ ${String.format("%.1f", expenseChangePercent)}% so với $periodText trước"
                        else "↑ +${String.format("%.1f", expenseChangePercent)}% so với $periodText trước",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (expenseChangePercent <= 0) Color(0xFF2E7D32) else Color(0xFFE53935)
                    )
                }
            }
        }

        // COMBINED CHART CARD ("Biểu đồ thu chi")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header of Chart
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Biểu đồ thu chi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Đơn vị: triệu đ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Legend Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.width(16.dp).height(3.dp).background(Color(0xFF2E7D32)))
                        Text("Thu", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF7043)))
                        Text("Chi", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Canvas Combined Chart
                CombinedTrendCanvasChart(
                    selectedPeriod = selectedPeriod,
                    periodTransactions = periodTransactions,
                    startDate = startDate,
                    endDate = endDate
                )
            }
        }

        // OVERVIEW GRID SECTION ("Tổng quan" - 4 Cards)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tổng quan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val netBalance = totalIncome - totalExpense
                val savingsRate = if (totalIncome > 0) ((netBalance / totalIncome) * 100).coerceAtLeast(0.0) else 0.0

                // 1. Tổng thu
                OverviewItemCard(
                    modifier = Modifier.weight(1f),
                    bgColor = Color(0xFFE8F5E9),
                    iconColor = Color(0xFF2E7D32),
                    icon = Icons.Default.ArrowDownward,
                    valueStr = FormatHelper.formatVND(totalIncome),
                    labelStr = "Tổng thu"
                )

                // 2. Tổng chi
                OverviewItemCard(
                    modifier = Modifier.weight(1f),
                    bgColor = Color(0xFFFFEBEE),
                    iconColor = Color(0xFFC62828),
                    icon = Icons.Default.ArrowUpward,
                    valueStr = FormatHelper.formatVND(totalExpense),
                    labelStr = "Tổng chi"
                )

                // 3. Chênh lệch
                OverviewItemCard(
                    modifier = Modifier.weight(1f),
                    bgColor = Color(0xFFE3F2FD),
                    iconColor = Color(0xFF1976D2),
                    icon = Icons.Default.AccountBalanceWallet,
                    valueStr = FormatHelper.formatVND(netBalance),
                    labelStr = "Chênh lệch"
                )

                // 4. Tỷ lệ tiết kiệm
                OverviewItemCard(
                    modifier = Modifier.weight(1f),
                    bgColor = Color(0xFFF3E5F5),
                    iconColor = Color(0xFF7B1FA2),
                    icon = Icons.Default.Savings,
                    valueStr = "${savingsRate.toInt()}%",
                    labelStr = "Tỷ lệ tiết kiệm"
                )
            }
        }

        // TOP SPENDING CATEGORIES SECTION
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Danh mục chi tiêu nhiều nhất", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Xem tất cả", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), modifier = Modifier.clickable { })
            }

            if (topExpenseCategories.isEmpty()) {
                Text("Không có dữ liệu chi tiêu trong khoảng thời gian này.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        topExpenseCategories.forEach { cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFFFEBEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = IconMapper.getIconByName(cat.iconName),
                                        contentDescription = cat.name,
                                        tint = Color(0xFFC62828),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(cat.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    LinearProgressIndicator(
                                        progress = { cat.percentage / 100f },
                                        color = Color(0xFFFF7043),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(FormatHelper.formatVND(cat.amount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${cat.percentage.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                }
                            }
                        }
                    }
                }
            }
        }

        // SMART INSIGHT BANNER
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFFFF8E1)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                }

                val periodName = when (selectedPeriod) {
                    "WEEK" -> "tuần"
                    "MONTH" -> "tháng"
                    "YEAR" -> "năm"
                    else -> "kỳ"
                }

                val insightText = if (expenseChangePercent <= 0) {
                    "Bạn đã chi tiêu ít hơn ${String.format("%.1f", Math.abs(expenseChangePercent))}% so với $periodName trước. Tiếp tục duy trì thói quen tốt nhé!"
                } else {
                    "Chi tiêu của bạn đã tăng ${String.format("%.1f", expenseChangePercent)}% so với $periodName trước. Hãy cân nhắc tối ưu các khoản không cần thiết!"
                }

                Text(
                    text = insightText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    lineHeight = 15.sp
                )

                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = null,
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun OverviewItemCard(
    modifier: Modifier = Modifier,
    bgColor: Color,
    iconColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueStr: String,
    labelStr: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            }
            Text(
                text = valueStr,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = iconColor,
                maxLines = 1
            )
            Text(
                text = labelStr,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// CANVAS COMBINED LINE & BAR CHART FOR TRENDS
@Composable
fun CombinedTrendCanvasChart(
    selectedPeriod: String,
    periodTransactions: List<Transaction>,
    startDate: Long,
    endDate: Long
) {
    val textMeasurer = rememberTextMeasurer()

    val chartData = remember(selectedPeriod, periodTransactions, startDate, endDate) {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY

        when (selectedPeriod) {
            "WEEK" -> {
                val labels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                val dayFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

                labels.mapIndexed { index, dayName ->
                    cal.timeInMillis = startDate
                    cal.add(Calendar.DAY_OF_YEAR, index)
                    val dayStart = cal.timeInMillis
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    val dayEnd = cal.timeInMillis

                    val dayTxs = periodTransactions.filter { it.timestamp in dayStart..dayEnd }
                    val inc = dayTxs.filter { it.type == "INCOME" }.sumOf { it.amount } / 1_000_000.0
                    val exp = dayTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount } / 1_000_000.0
                    val fullLabel = "$dayName\n${dayFormat.format(Date(dayStart))}"

                    ChartBucket(fullLabel, inc, exp)
                }
            }
            "MONTH" -> {
                val labels = listOf("Tuần 1", "Tuần 2", "Tuần 3", "Tuần 4")
                val duration = (endDate - startDate) / 4
                labels.mapIndexed { index, weekName ->
                    val wStart = startDate + index * duration
                    val wEnd = if (index == 3) endDate else startDate + (index + 1) * duration - 1

                    val wTxs = periodTransactions.filter { it.timestamp in wStart..wEnd }
                    val inc = wTxs.filter { it.type == "INCOME" }.sumOf { it.amount } / 1_000_000.0
                    val exp = wTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount } / 1_000_000.0

                    ChartBucket(weekName, inc, exp)
                }
            }
            "YEAR" -> {
                (1..12).map { monthNum ->
                    cal.timeInMillis = startDate
                    cal.set(Calendar.MONTH, monthNum - 1)
                    val mStart = cal.timeInMillis
                    val maxD = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, maxD)
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    val mEnd = cal.timeInMillis

                    val mTxs = periodTransactions.filter { it.timestamp in mStart..mEnd }
                    val inc = mTxs.filter { it.type == "INCOME" }.sumOf { it.amount } / 1_000_000.0
                    val exp = mTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount } / 1_000_000.0

                    ChartBucket("T$monthNum", inc, exp)
                }
            }
            else -> { // 5YEARS
                (0..4).map { yearOffset ->
                    cal.timeInMillis = startDate
                    cal.add(Calendar.YEAR, yearOffset)
                    val yStart = cal.timeInMillis
                    cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
                    val yEnd = cal.timeInMillis

                    val yTxs = periodTransactions.filter { it.timestamp in yStart..yEnd }
                    val inc = yTxs.filter { it.type == "INCOME" }.sumOf { it.amount } / 1_000_000.0
                    val exp = yTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount } / 1_000_000.0

                    val sdf = SimpleDateFormat("yyyy", Locale.getDefault())
                    ChartBucket(sdf.format(Date(yStart)), inc, exp)
                }
            }
        }
    }

    val maxVal = remember(chartData) {
        val highest = chartData.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0
        if (highest <= 0.0) 10.0 else highest * 1.25
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val width = size.width
        val height = size.height
        val bottomMargin = 45.dp.toPx()
        val topMargin = 30.dp.toPx()
        val chartHeight = height - bottomMargin - topMargin
        val numPoints = chartData.size
        val pointSpacing = width / numPoints

        // Draw horizontal grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = topMargin + (chartHeight / gridLines) * i
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(width, y),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }

        // Draw Bars for Expense (Chi)
        val barWidth = (pointSpacing * 0.4f).coerceIn(12.dp.toPx(), 28.dp.toPx())
        chartData.forEachIndexed { index, bucket ->
            val cx = index * pointSpacing + pointSpacing / 2f
            val barH = ((bucket.expense / maxVal) * chartHeight).toFloat()
            val barTop = topMargin + chartHeight - barH

            // Bar Gradient
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF7043), Color(0xFFFFCCBC))
                ),
                topLeft = Offset(cx - barWidth / 2f, barTop),
                size = Size(barWidth, barH.coerceAtLeast(4f)),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // Value text above bar if expense > 0
            if (bucket.expense > 0) {
                val expStr = String.format(Locale.US, "%.1f", bucket.expense)
                val textLayout = textMeasurer.measure(
                    text = expStr,
                    style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(cx - textLayout.size.width / 2f, barTop - textLayout.size.height - 2.dp.toPx())
                )
            }

            // X-Axis Labels
            val labelLines = bucket.label.split("\n")
            var labelY = topMargin + chartHeight + 6.dp.toPx()
            labelLines.forEach { line ->
                val labelLayout = textMeasurer.measure(
                    text = line,
                    style = TextStyle(fontSize = 9.sp, color = Color.Gray, textAlign = TextAlign.Center)
                )
                drawText(
                    textLayoutResult = labelLayout,
                    topLeft = Offset(cx - labelLayout.size.width / 2f, labelY)
                )
                labelY += labelLayout.size.height
            }
        }

        // Draw Line & Nodes for Income (Thu)
        val linePath = Path()
        val points = chartData.mapIndexed { index, bucket ->
            val cx = index * pointSpacing + pointSpacing / 2f
            val nodeY = topMargin + chartHeight - ((bucket.income / maxVal) * chartHeight).toFloat()
            Offset(cx, nodeY)
        }

        if (points.isNotEmpty()) {
            linePath.moveTo(points[0].x, points[0].y)
            for (i in 0 until points.size - 1) {
                val p1 = points[i]
                val p2 = points[i + 1]
                val controlX = (p1.x + p2.x) / 2f
                linePath.cubicTo(controlX, p1.y, controlX, p2.y, p2.x, p2.y)
            }

            // Draw line
            drawPath(
                path = linePath,
                color = Color(0xFF2E7D32),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw nodes and callout boxes
            points.forEachIndexed { index, pt ->
                val incVal = chartData[index].income
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pt)
                drawCircle(color = Color(0xFF2E7D32), radius = 3.5.dp.toPx(), center = pt)

                if (incVal > 0) {
                    val incStr = String.format(Locale.US, "%.1f", incVal)
                    val textLayout = textMeasurer.measure(
                        text = incStr,
                        style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    val badgeW = textLayout.size.width + 8.dp.toPx()
                    val badgeH = textLayout.size.height + 4.dp.toPx()
                    val badgeTop = pt.y - badgeH - 4.dp.toPx()

                    drawRoundRect(
                        color = Color(0xFF2E7D32),
                        topLeft = Offset(pt.x - badgeW / 2f, badgeTop),
                        size = Size(badgeW, badgeH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(pt.x - textLayout.size.width / 2f, badgeTop + 2.dp.toPx())
                    )
                }
            }
        }
    }
}

data class ChartBucket(
    val label: String,
    val income: Double,
    val expense: Double
)

// ==========================================
// TAB 2: PHÂN BỔ (DISTRIBUTION REPORT CONTENT)
// ==========================================
@Composable
fun DistributionReportContent(viewModel: FinanceViewModel) {
    val transactions by viewModel.dailyTransactions.collectAsState()
    val activeMonth by viewModel.activeMonth.collectAsState()

    val currentYear = remember(activeMonth) {
        try { activeMonth.substring(0, 4).toInt() } catch (e: Exception) { Calendar.getInstance().get(Calendar.YEAR) }
    }
    val currentMonth = remember(activeMonth) {
        try { activeMonth.substring(5, 7).toInt() } catch (e: Exception) { Calendar.getInstance().get(Calendar.MONTH) + 1 }
    }

    fun adjustMonth(diff: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth - 1 + diff)
        }
        val formatted = String.format("%04d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
        viewModel.setActiveMonth(formatted)
    }

    val monthTransactions = remember(transactions, activeMonth) {
        val cal = Calendar.getInstance()
        transactions.filter {
            cal.timeInMillis = it.timestamp
            val txMonth = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            txMonth == activeMonth
        }
    }

    val savingsWallets by viewModel.savingsWallets.collectAsState()
    val savingsWalletIds = remember(savingsWallets) { savingsWallets.map { it.id }.toSet() }

    val financialSummary = remember(monthTransactions, savingsWalletIds) {
        com.app.ui.calculateRealFinancialSummary(monthTransactions, savingsWalletIds)
    }

    val totalExpenses = financialSummary.realExpense
    val totalIncome = financialSummary.realIncome
    val categoriesList by viewModel.categoriesList.collectAsState()

    fun getRootCategory(catName: String): com.app.data.FinanceCategory {
        var currentCat = categoriesList.find { it.name.equals(catName, ignoreCase = true) }
        while (currentCat?.parentName != null) {
            val parent = categoriesList.find { it.name.equals(currentCat!!.parentName, ignoreCase = true) }
            if (parent != null) {
                currentCat = parent
            } else {
                break
            }
        }
        return currentCat ?: com.app.data.FinanceCategory(catName, "Category", "#607D8B", "BOTH")
    }

    val categoryExpenses = remember(monthTransactions, savingsWalletIds, totalExpenses, categoriesList) {
        monthTransactions
            .filter { tx ->
                val catName = tx.categoryName.trim()
                val isAdjustment = tx.type == "ADJUSTMENT" || catName.contains("Điều chỉnh số dư", ignoreCase = true)
                val isInternalTransfer = tx.type == "TRANSFER"
                val isSavingsDeposit = catName.contains("Gửi tiết kiệm", ignoreCase = true) || catName.contains("Cất quỹ", ignoreCase = true) || (tx.destinationWalletId != null && tx.destinationWalletId in savingsWalletIds)
                tx.type == "EXPENSE" && !isAdjustment && !isInternalTransfer && !isSavingsDeposit
            }
            .groupBy { tx -> getRootCategory(tx.categoryName.trim()).name }
            .map { (catName, txs) ->
                val sum = txs.sumOf { it.amount }
                val rootCat = getRootCategory(catName)
                CategorySpend(
                    name = rootCat.name,
                    amount = sum,
                    colorHex = rootCat.colorHex,
                    iconName = rootCat.iconName,
                    percentage = if (totalExpenses > 0) (sum / totalExpenses * 100).toFloat() else 0f
                )
            }.sortedByDescending { it.amount }
    }

    val categoryIncomes = remember(monthTransactions, savingsWalletIds, totalIncome, categoriesList) {
        monthTransactions
            .filter { tx ->
                val catName = tx.categoryName.trim()
                val isAdjustment = tx.type == "ADJUSTMENT" || catName.contains("Điều chỉnh số dư", ignoreCase = true)
                val isInternalTransfer = tx.type == "TRANSFER"
                val isSavingsWithdraw = catName.contains("Đóng hũ", ignoreCase = true) || catName.contains("Rút từ tiết kiệm", ignoreCase = true) || catName.contains("Hoàn quỹ", ignoreCase = true) || (tx.walletId in savingsWalletIds)
                tx.type == "INCOME" && !isAdjustment && !isInternalTransfer && !isSavingsWithdraw
            }
            .groupBy { tx -> getRootCategory(tx.categoryName.trim()).name }
            .map { (catName, txs) ->
                val sum = txs.sumOf { it.amount }
                val rootCat = getRootCategory(catName)
                CategorySpend(
                    name = rootCat.name,
                    amount = sum,
                    colorHex = rootCat.colorHex,
                    iconName = rootCat.iconName,
                    percentage = if (totalIncome > 0) (sum / totalIncome * 100).toFloat() else 0f
                )
            }.sortedByDescending { it.amount }
    }

    var selectedCategoryForHistory by remember { mutableStateOf<String?>(null) }
    var selectedReportType by remember { mutableStateOf("EXPENSE") }
    var selectedCategoryName by remember { mutableStateOf<String?>(null) }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(activeMonth, selectedReportType) {
        selectedCategoryName = null
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    if (selectedCategoryForHistory != null) {
        com.app.ui.components.CategoryTransactionsDialog(
            categoryName = selectedCategoryForHistory!!,
            monthKey = activeMonth,
            viewModel = viewModel,
            onDismiss = { selectedCategoryForHistory = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timeline month selector box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    RoundedCornerShape(14.dp)
                )
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { adjustMonth(-1) },
                modifier = Modifier.testTag("report_prev_month_btn")
            ) {
                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Prev month")
            }

            Text(
                text = "THÁNG $currentMonth - NĂM $currentYear",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            IconButton(
                onClick = { adjustMonth(1) },
                modifier = Modifier.testTag("report_next_month_btn")
            ) {
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next month")
            }
        }

        // Summary comparative indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Tổng thu nhập", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatHelper.formatVND(totalIncome),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Tổng chi tiêu", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatHelper.formatVND(totalExpenses),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
            }
        }

        // Selector tab row
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))) {
            Box(modifier = Modifier.weight(1f).clickable { selectedReportType = "EXPENSE" }.background(if (selectedReportType == "EXPENSE") MaterialTheme.colorScheme.primary else Color.Transparent).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text("Khoản chi", fontWeight = FontWeight.Bold, color = if (selectedReportType == "EXPENSE") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            }
            Box(modifier = Modifier.weight(1f).clickable { selectedReportType = "INCOME" }.background(if (selectedReportType == "INCOME") MaterialTheme.colorScheme.primary else Color.Transparent).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text("Khoản thu", fontWeight = FontWeight.Bold, color = if (selectedReportType == "INCOME") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            }
        }

        val currentTotal = if (selectedReportType == "EXPENSE") totalExpenses else totalIncome
        val currentCategories = if (selectedReportType == "EXPENSE") categoryExpenses else categoryIncomes
        val chartTitle = if (selectedReportType == "EXPENSE") "BIỂU ĐỒ PHÂN BỔ CHI TIÊU" else "BIỂU ĐỒ PHÂN BỔ THU NHẬP"
        val totalLabel = if (selectedReportType == "EXPENSE") "Tổng chi" else "Tổng thu"
        val totalColor = if (selectedReportType == "EXPENSE") Color(0xFFF44336) else Color(0xFF4CAF50)

        if (currentTotal == 0.0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = "Empty Stats",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (selectedReportType == "EXPENSE") "Không phát sinh chi tiêu trong tháng này" else "Không phát sinh thu nhập trong tháng này",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = chartTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Canvas donut chart
                    Box(
                        modifier = Modifier.size(200.dp).testTag("report_canvas_donut_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        val strokeWidthPx = with(density) { 28.dp.toPx() }
                        val radiusPx = with(density) { 160.dp.toPx() / 2f }
                        val innerRadius = radiusPx - strokeWidthPx / 2f
                        val outerRadius = radiusPx + strokeWidthPx / 2f

                        Canvas(
                            modifier = Modifier
                                .size(160.dp)
                                .pointerInput(currentCategories, currentTotal, animProgress.value) {
                                    detectTapGestures { offset ->
                                        val centerX = size.width / 2f
                                        val centerY = size.height / 2f
                                        val dx = offset.x - centerX
                                        val dy = offset.y - centerY
                                        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                                        if (distance in innerRadius..outerRadius) {
                                            var angleRad = Math.atan2(dy.toDouble(), dx.toDouble())
                                            var angleDeg = Math.toDegrees(angleRad).toFloat()
                                            if (angleDeg < 0) {
                                                angleDeg += 360f
                                            }
                                            var adjustedAngle = angleDeg - 270f
                                            if (adjustedAngle < 0) {
                                                adjustedAngle += 360f
                                            }

                                            var accumulatedAngle = 0f
                                            var found = false
                                            for (cat in currentCategories) {
                                                val sweepAngle = (cat.amount / currentTotal * 360f).toFloat() * animProgress.value
                                                if (adjustedAngle >= accumulatedAngle && adjustedAngle < accumulatedAngle + sweepAngle) {
                                                    selectedCategoryName = if (selectedCategoryName == cat.name) null else cat.name
                                                    found = true
                                                    break
                                                }
                                                accumulatedAngle += sweepAngle
                                            }
                                            if (!found) {
                                                selectedCategoryName = null
                                            }
                                        } else {
                                            selectedCategoryName = null
                                        }
                                    }
                                }
                        ) {
                            var startAngle = -90f
                            for (cat in currentCategories) {
                                val sweepAngle = (cat.amount / currentTotal * 360f).toFloat() * animProgress.value
                                val isSelected = selectedCategoryName == cat.name
                                val alpha = if (selectedCategoryName == null || isSelected) 1f else 0.2f
                                drawArc(
                                    color = FormatHelper.parseColor(cat.colorHex).copy(alpha = alpha),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 28.dp.toPx())
                                )
                                startAngle += sweepAngle
                            }
                        }

                        val selectedCat = remember(selectedCategoryName, currentCategories) {
                            currentCategories.firstOrNull { it.name == selectedCategoryName }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = selectedCat?.name ?: totalLabel,
                                fontSize = 14.sp,
                                fontWeight = if (selectedCat != null) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCat != null) FormatHelper.parseColor(selectedCat.colorHex) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Text(
                                text = FormatHelper.formatVND(selectedCat?.amount ?: currentTotal),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selectedCat != null) FormatHelper.parseColor(selectedCat.colorHex) else totalColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentCategories.take(5).forEach { cat ->
                            val isSelected = selectedCategoryName == cat.name
                            val alpha = if (selectedCategoryName == null || isSelected) 1f else 0.3f
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedCategoryName = if (selectedCategoryName == cat.name) null else cat.name
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp)
                                    .alpha(alpha),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(FormatHelper.parseColor(cat.colorHex))
                                    )
                                    Text(
                                        text = cat.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) FormatHelper.parseColor(cat.colorHex) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${String.format("%.1f", cat.percentage)}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) FormatHelper.parseColor(cat.colorHex) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Chi tiết theo danh mục",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Column(
                modifier = Modifier.fillMaxWidth().testTag("report_category_spending_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val displayCategories = if (selectedReportType == "EXPENSE") categoryExpenses else categoryIncomes
                displayCategories.forEach { cat ->
                    val isSelected = selectedCategoryName == cat.name
                    val alpha = if (selectedCategoryName == null || isSelected) 1f else 0.3f
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .clickable {
                                selectedCategoryName = if (selectedCategoryName == cat.name) null else cat.name
                            }
                            .padding(16.dp)
                            .alpha(alpha),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                        .clip(CircleShape)
                                        .background(FormatHelper.parseColor(cat.colorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = IconMapper.getIconByName(cat.iconName),
                                        contentDescription = cat.name,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = cat.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = FormatHelper.formatVND(cat.amount),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Text(
                                        text = "Xem lịch sử ➜",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable {
                                            selectedCategoryForHistory = cat.name
                                        }
                                    )
                                } else {
                                    Text(
                                        text = "${String.format("%.1f", cat.percentage)}%",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        LinearProgressIndicator(
                            progress = { cat.percentage / 100f },
                            color = FormatHelper.parseColor(cat.colorHex),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

data class CategorySpend(
    val name: String,
    val amount: Double,
    val colorHex: String,
    val iconName: String,
    val percentage: Float
)
