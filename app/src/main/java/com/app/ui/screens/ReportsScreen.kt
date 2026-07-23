package com.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.ui.FinanceViewModel
import com.app.ui.FormatHelper
import com.app.ui.IconMapper
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.alpha
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.dailyTransactions.collectAsState()
    val activeMonth by viewModel.activeMonth.collectAsState() // Format: "YYYY-MM"

    // Parse activeMonth date safely
    val currentYear = remember(activeMonth) {
        try {
            activeMonth.substring(0, 4).toInt()
        } catch (e: Exception) {
            Calendar.getInstance().get(Calendar.YEAR)
        }
    }
    val currentMonth = remember(activeMonth) {
        try {
            activeMonth.substring(5, 7).toInt()
        } catch (e: Exception) {
            Calendar.getInstance().get(Calendar.MONTH) + 1
        }
    }

    // Helpers to toggle months
    fun adjustMonth(diff: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth - 1 + diff)
        }
        val formatted = String.format("%04d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
        viewModel.setActiveMonth(formatted)
    }

    // Filter transaction for the active month - optimized to reuse a single Calendar instance
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

    // Group expenses by category
    val categoryExpenses = remember(monthTransactions, savingsWalletIds, totalExpenses) {
        monthTransactions
            .filter { tx ->
                val catName = tx.categoryName.trim()
                val isAdjustment = tx.type == "ADJUSTMENT" || catName.contains("Điều chỉnh số dư", ignoreCase = true)
                val isInternalTransfer = tx.type == "TRANSFER"
                val isSavingsDeposit = catName.contains("Gửi tiết kiệm", ignoreCase = true) || catName.contains("Cất quỹ", ignoreCase = true) || (tx.destinationWalletId != null && tx.destinationWalletId in savingsWalletIds)
                tx.type == "EXPENSE" && !isAdjustment && !isInternalTransfer && !isSavingsDeposit
            }
            .groupBy { it.categoryName }
            .map { (catName, txs) ->
                val sum = txs.sumOf { it.amount }
                CategorySpend(
                    name = catName,
                    amount = sum,
                    colorHex = txs.first().categoryColor,
                    iconName = txs.first().categoryIcon,
                    percentage = if (totalExpenses > 0) (sum / totalExpenses * 100).toFloat() else 0f
                )
            }.sortedByDescending { it.amount }
    }

    // Group incomes by category
    val categoryIncomes = remember(monthTransactions, savingsWalletIds, totalIncome) {
        monthTransactions
            .filter { tx ->
                val catName = tx.categoryName.trim()
                val isAdjustment = tx.type == "ADJUSTMENT" || catName.contains("Điều chỉnh số dư", ignoreCase = true)
                val isInternalTransfer = tx.type == "TRANSFER"
                val isSavingsWithdraw = catName.contains("Đóng hũ", ignoreCase = true) || catName.contains("Rút từ tiết kiệm", ignoreCase = true) || catName.contains("Hoàn quỹ", ignoreCase = true) || (tx.walletId in savingsWalletIds)
                tx.type == "INCOME" && !isAdjustment && !isInternalTransfer && !isSavingsWithdraw
            }
            .groupBy { it.categoryName }
            .map { (catName, txs) ->
                val sum = txs.sumOf { it.amount }
                CategorySpend(
                    name = catName,
                    amount = sum,
                    colorHex = txs.first().categoryColor,
                    iconName = txs.first().categoryIcon,
                    percentage = if (totalIncome > 0) (sum / totalIncome * 100).toFloat() else 0f
                )
            }.sortedByDescending { it.amount }
    }

    var selectedCategoryForHistory by remember { mutableStateOf<String?>(null) }
    var selectedReportType by remember { mutableStateOf("EXPENSE") } // or INCOME
    var selectedCategoryName by remember { mutableStateOf<String?>(null) }
    
    val animProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(activeMonth, selectedReportType) {
        selectedCategoryName = null
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 800,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
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
        modifier = modifier
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
            // Income Indicator Card
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

            // Expense Indicator Card
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

        // --- Selector tab row ---
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))) {
            Box(modifier = Modifier.weight(1f).clickable { selectedReportType = "EXPENSE" }.background(if (selectedReportType == "EXPENSE") MaterialTheme.colorScheme.primary else Color.Transparent).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text("Khoản chi", fontWeight = FontWeight.Bold, color = if (selectedReportType == "EXPENSE") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            }
            Box(modifier = Modifier.weight(1f).clickable { selectedReportType = "INCOME" }.background(if (selectedReportType == "INCOME") MaterialTheme.colorScheme.primary else Color.Transparent).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text("Khoản thu", fontWeight = FontWeight.Bold, color = if (selectedReportType == "INCOME") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

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

                    // Canvas-based donut chart
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

                        // Display selected category's info in center if any, otherwise global total
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

                    // Interactive legend grid
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

            // Category detail breakdown list
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

                        // Linear progress indicator
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
