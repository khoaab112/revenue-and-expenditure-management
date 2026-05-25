package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Budget
import com.example.data.Categories
import com.example.data.FinanceCategory
import com.example.data.SavingsGoal
import com.example.ui.FinanceViewModel
import com.example.ui.FormatHelper
import com.example.ui.IconMapper
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
fun BudgetGoalScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Budgets, 1 = Savings Goals
    val tabs = listOf("Hạn Mức Chi Tiêu", "Mục Tiêu Tiết Kiệm")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Headers
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("budget_goal_tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Content
        when (selectedTab) {
            0 -> BudgetsSection(viewModel = viewModel)
            1 -> SavingsGoalsSection(viewModel = viewModel)
        }
    }
}

// =================== BUDGETS SECTION ===================
@Composable
fun BudgetsSection(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val budgets by viewModel.allBudgets.collectAsState()
    val activeMonth by viewModel.activeMonth.collectAsState()
    val categoriesList by viewModel.categoriesList.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val displayBudgetMonth = remember(activeMonth) {
        if (activeMonth.length >= 7) {
            "Tháng ${activeMonth.substring(5)}/${activeMonth.substring(0, 4)}"
        } else {
            activeMonth
        }
    }

    // Filter budgets for the active month
    val filteredBudgets = remember(budgets, activeMonth) {
        budgets.filter { it.month == activeMonth }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ngân Sách: $displayBudgetMonth",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_budget_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Budget")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thiết lập", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredBudgets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = "Empty Budgets",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chưa cấu hình ngân sách tháng này!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("budgets_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBudgets) { budget ->
                        BudgetItemCard(
                            budget = budget,
                            onDelete = { viewModel.deleteBudget(budget) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddBudgetDialog(
                categoriesList = categoriesList,
                onDismiss = { showAddDialog = false },
                onAddBudget = { category, limit ->
                    viewModel.addBudget(category, limit, activeMonth)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun BudgetItemCard(
    budget: Budget,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ratio = if (budget.limitAmount > 0) (budget.spentAmount / budget.limitAmount).toFloat() else 0f
    
    // Choose progress color depending on danger ratios
    val progressColor = when {
        ratio >= 1.0f -> Color(0xFFF44336) // Red (overspent)
        ratio >= 0.8f -> Color(0xFFFF9800) // Orange (danger)
        else -> Color(0xFF4CAF50) // Green
    }

    val catColor = FormatHelper.parseColor(budget.categoryColor)

    Card(
        modifier = modifier.fillMaxWidth().testTag("budget_card_${budget.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            .background(catColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = IconMapper.getIconByName(budget.categoryIcon),
                            contentDescription = budget.categoryName,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = budget.categoryName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp).testTag("delete_budget_btn_${budget.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Đã dùng: ${FormatHelper.formatVND(budget.spentAmount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = progressColor
                )
                Text(
                    text = "Hạn mức: ${FormatHelper.formatVND(budget.limitAmount)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = ratio.coerceIn(0f, 1f),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                if (ratio >= 1.0f) {
                    Text(
                        text = "VƯỢT QUÁ HẠN MỨC CHO PHÉP!",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                } else if (ratio >= 0.8f) {
                    Text(
                        text = "Cảnh báo chi vượt quá 80% hạn mức!",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

@Composable
fun AddBudgetDialog(
    categoriesList: List<FinanceCategory>,
    onDismiss: () -> Unit,
    onAddBudget: (String, Double) -> Unit
) {
    var selectedCategory by remember {
        val defaultExpenseCat = categoriesList.firstOrNull { it.type == "EXPENSE" } ?: categoriesList.firstOrNull()
        mutableStateOf(defaultExpenseCat?.name ?: "Khác")
    }
    var limitStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("THIẾT LẬP HẠN MỨC MỚI") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category Pick row
                Column {
                    Text("Danh Mục Chi Tiêu", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoriesList.filter { it.type == "EXPENSE" || it.type == "BOTH" }.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat.name,
                                onClick = { selectedCategory = cat.name },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }

                com.example.ui.components.CustomMoneyInputField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = "Hạn mức tối đa (VND)",
                    testTag = "budget_limit_input"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lm = FormatHelper.evaluateExpression(limitStr)
                    if (lm > 0) {
                        onAddBudget(selectedCategory, lm)
                    }
                },
                modifier = Modifier.testTag("confirm_add_budget_btn")
            ) {
                Text("Cấu hình")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}


// =================== SAVINGS GOALS SECTION ===================
@Composable
fun SavingsGoalsSection(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.allSavingsGoals.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    
    // Supporting adding funds dialog
    var showAddFundsDialogForGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mục Tiêu Tiết Kiệm",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_savings_goal_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Goal")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tạo mục tiêu", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (goals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.GolfCourse,
                            contentDescription = "Empty Goals",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chưa đăng ký mục tiêu tiết kiệm!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("savings_goals_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(goals) { goal ->
                        SavingsGoalCard(
                            goal = goal,
                            onUpdateFunds = { showAddFundsDialogForGoal = goal },
                            onDelete = { viewModel.deleteSavingsGoal(goal) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddSavingsGoalDialog(
                onDismiss = { showAddDialog = false },
                onAddGoal = { name, target, starting, date, notes ->
                    viewModel.addSavingsGoal(name, target, starting, date, notes)
                    showAddDialog = false
                }
            )
        }

        // Tích lũy thêm quỹ tiệm kiệm
        if (showAddFundsDialogForGoal != null) {
            val gl = showAddFundsDialogForGoal!!
            AddFundsGoalDialog(
                goal = gl,
                onDismiss = { showAddFundsDialogForGoal = null },
                onAddFunds = { amount ->
                    val newAm = gl.currentAmount + amount
                    viewModel.updateSavingsGoal(gl.copy(currentAmount = newAm))
                    showAddFundsDialogForGoal = null
                }
            )
        }
    }
}

@Composable
fun SavingsGoalCard(
    goal: SavingsGoal,
    onUpdateFunds: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ratio = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
    
    val daysRemaining = remember(goal.targetDate) {
        val diff = goal.targetDate - System.currentTimeMillis()
        if (diff > 0) TimeUnit.MILLISECONDS.toDays(diff) else 0L
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("savings_goal_card_${goal.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = goal.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Còn lại $daysRemaining ngày • Hạn chót: ${FormatHelper.formatDate(goal.targetDate)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onUpdateFunds,
                        modifier = Modifier.size(24.dp).testTag("add_funds_goal_${goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Tích lũy",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp).testTag("delete_goal_${goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            if (goal.note.isNotEmpty()) {
                Text(
                    text = goal.note,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Đã gom: ${FormatHelper.formatVND(goal.currentAmount)} (${(ratio * 100).toInt()}%)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Mục tiêu: ${FormatHelper.formatVND(goal.targetAmount)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = ratio.coerceIn(0f, 1f),
                color = if (ratio >= 1.0f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            if (ratio >= 1.0f) {
                Text(
                    text = "🎉 CHÚC MỪNG BẠN ĐÃ ĐẠT ĐƯỢC MỤC TIÊU!",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AddSavingsGoalDialog(
    onDismiss: () -> Unit,
    onAddGoal: (String, Double, Double, Long, String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("") }
    var currentStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val calendar = remember { Calendar.getInstance() }
    var selectedTimestamp by remember { mutableStateOf(calendar.timeInMillis + (30L * 24L * 60L * 60L * 1000L)) } // Default 30 days ahead
    var dateLabel by remember { mutableStateOf(FormatHelper.formatDate(selectedTimestamp)) }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedTimestamp = newCal.timeInMillis
                dateLabel = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("TẠO MỤC TIÊU TIẾT KIỆM MỚI") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên mục tiêu (Ví dụ: Mua xe máy, du lịch...)") },
                    modifier = Modifier.fillMaxWidth().testTag("goal_name")
                )

                com.example.ui.components.CustomMoneyInputField(
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = "Số tiền mục tiêu (VND)",
                    testTag = "goal_target"
                )

                com.example.ui.components.CustomMoneyInputField(
                    value = currentStr,
                    onValueChange = { currentStr = it },
                    label = "Đã tích lũy sẵn (VND)",
                    testTag = "goal_current"
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Chi tiết ghi chú / Lý do") },
                    modifier = Modifier.fillMaxWidth().testTag("goal_notes")
                )

                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Goal Date")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ngày đạt mục tiêu: $dateLabel")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tg = FormatHelper.evaluateExpression(targetStr)
                    val cr = FormatHelper.evaluateExpression(currentStr)
                    if (name.isNotEmpty() && tg > 0) {
                        onAddGoal(name, tg, cr, selectedTimestamp, notes)
                    }
                },
                modifier = Modifier.testTag("confirm_add_goal_btn")
            ) {
                Text("Tạo mục tiêu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun AddFundsGoalDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onAddFunds: (Double) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("TÍCH LŨY THÊM") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Đóng góp ngân sách tích lũy tích cực cho: ${goal.name}", fontSize = 13.sp)
                com.example.ui.components.CustomMoneyInputField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = "Số tiền tích lũy thêm (VND)",
                    testTag = "add_funds_input"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val am = FormatHelper.evaluateExpression(amountStr)
                    if (am > 0) {
                        onAddFunds(am)
                    }
                },
                modifier = Modifier.testTag("confirm_add_funds_btn")
            ) {
                Text("Nộp vào")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
