package com.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.data.Budget
import com.app.data.Categories
import com.app.data.FinanceCategory
import com.app.data.SavingsGoal
import com.app.ui.FinanceViewModel
import com.app.ui.FormatHelper
import com.app.ui.IconMapper
import com.app.ui.components.StripedProgressIndicator
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
fun BudgetGoalScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BudgetsSection(viewModel = viewModel, onNavigateBack = onNavigateBack, modifier = modifier)
}

private fun isTimestampInMonth(timestamp: Long, monthStr: String): Boolean {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val checkMonth = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    return checkMonth == monthStr
}

// =================== BUDGETS SECTION ===================
@Composable
fun BudgetsSection(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budgets by viewModel.allBudgets.collectAsState()
    val activeMonth by viewModel.activeMonth.collectAsState()
    val categoriesList by viewModel.categoriesList.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val savingsWallets by viewModel.savingsWallets.collectAsState()
    val savingsWalletIds = remember(savingsWallets) { savingsWallets.map { it.id }.toSet() }

    var showAddDialog by remember { mutableStateOf(false) }
    var showMonthPickerDialog by remember { mutableStateOf(false) }
    var budgetToDelete by remember { mutableStateOf<Budget?>(null) }
    var selectedCategoryForHistory by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val currentRealMonthStr = remember {
        Calendar.getInstance().let { cal ->
            String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }
    }

    val displayBudgetMonth = remember(activeMonth) {
        if (activeMonth.length >= 7) {
            "Tháng ${activeMonth.substring(5)}/${activeMonth.substring(0, 4)}"
        } else {
            activeMonth
        }
    }

    fun navigateMonth(delta: Int) {
        try {
            val parts = activeMonth.split("-")
            var year = parts[0].toInt()
            var month = parts[1].toInt()
            month += delta
            if (month > 12) {
                month = 1
                year++
            } else if (month < 1) {
                month = 12
                year--
            }
            val newMonth = String.format("%04d-%02d", year, month)
            viewModel.setActiveMonth(newMonth)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Filter budgets for activeMonth & compute live spentAmount based on real non-virtual expenses of that month
    val filteredBudgets = remember(budgets, activeMonth, allTransactions, categoriesList, savingsWalletIds) {
        val listForMonth = budgets.filter { it.month == activeMonth }
        listForMonth.map { budget ->
            val familyCategories = mutableSetOf(budget.categoryName)
            var added = true
            while (added) {
                added = false
                for (c in categoriesList) {
                    if (c.name !in familyCategories && c.parentName in familyCategories) {
                        familyCategories.add(c.name)
                        added = true
                    }
                }
            }

            val dynamicSpent = allTransactions.filter { tx ->
                val catName = tx.categoryName.trim()
                val isAdjustment = tx.type == "ADJUSTMENT" || catName.contains("Điều chỉnh số dư", ignoreCase = true)
                val isInternalTransfer = tx.type == "TRANSFER"
                val isSavingsDeposit = catName.contains("Gửi tiết kiệm", ignoreCase = true) || catName.contains("Cất quỹ", ignoreCase = true) || (tx.destinationWalletId != null && tx.destinationWalletId in savingsWalletIds)

                tx.type == "EXPENSE" &&
                !isAdjustment &&
                !isInternalTransfer &&
                !isSavingsDeposit &&
                tx.categoryName in familyCategories &&
                isTimestampInMonth(tx.timestamp, activeMonth)
            }.sumOf { it.amount }

            budget.copy(spentAmount = dynamicSpent)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Row (Back Button + Add Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("budget_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại"
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .testTag("add_budget_btn")
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm ngân sách")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thêm", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Month Navigation Control Bar (< Ngân Sách: Tháng MM/YYYY >)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navigateMonth(-1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tháng trước",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showMonthPickerDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Chọn tháng",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ngân Sách: $displayBudgetMonth",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { navigateMonth(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Tháng sau",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (activeMonth != currentRealMonthStr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    AssistChip(
                        onClick = { viewModel.setActiveMonth(currentRealMonthStr) },
                        label = { Text("Về tháng hiện tại", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = "Current Month",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = "Empty Budgets",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Chưa cấu hình ngân sách cho $displayBudgetMonth",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { viewModel.copyBudgetsFromPreviousMonth(activeMonth) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Sao chép ngân sách",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sao chép ngân sách từ tháng trước", fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Thêm ngân sách mới",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Thêm ngân sách mới cho $displayBudgetMonth", fontSize = 13.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("budgets_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBudgets, key = { it.id }) { budget ->
                        BudgetItemCard(
                            budget = budget,
                            onDelete = { budgetToDelete = budget },
                            onToggleRecurring = { viewModel.toggleBudgetRecurring(budget) },
                            onClick = { selectedCategoryForHistory = budget.categoryName }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddBudgetDialog(
                categoriesList = categoriesList,
                onDismiss = { showAddDialog = false },
                onAddBudget = { category, limit, isRecurring ->
                    viewModel.addBudget(category, limit, activeMonth, isRecurring)
                    showAddDialog = false
                    viewModel.showSuccessNotification("Thêm ngân sách thành công!")
                }
            )
        }

        if (showMonthPickerDialog) {
            BudgetMonthPickerDialog(
                currentMonthStr = activeMonth,
                onDismiss = { showMonthPickerDialog = false },
                onMonthSelected = { selectedMonthStr ->
                    viewModel.setActiveMonth(selectedMonthStr)
                    showMonthPickerDialog = false
                }
            )
        }

        budgetToDelete?.let { budget ->
            AlertDialog(
                onDismissRequest = { budgetToDelete = null },
                title = { Text("Xác nhận xóa") },
                text = { Text("Bạn có chắc chắn muốn xóa ngân sách cho '${budget.categoryName}' không?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteBudget(budget)
                            budgetToDelete = null
                            viewModel.showSuccessNotification("Xóa ngân sách thành công!")
                        }
                    ) {
                        Text("Xóa", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { budgetToDelete = null }) {
                        Text("Hủy")
                    }
                }
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
    }
}

@Composable
fun BudgetMonthPickerDialog(
    currentMonthStr: String,
    onDismiss: () -> Unit,
    onMonthSelected: (String) -> Unit
) {
    val parts = currentMonthStr.split("-")
    var selectedYear by remember { mutableStateOf(parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedYear-- }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Năm trước")
                }
                Text("Năm $selectedYear", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = { selectedYear++ }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Năm sau")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val monthsList = (1..12).toList()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                ) {
                    items(monthsList) { m ->
                        val isSelected = m == selectedMonth
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedMonth = m },
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Tháng $m",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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
                    val formatted = String.format("%04d-%02d", selectedYear, selectedMonth)
                    onMonthSelected(formatted)
                }
            ) {
                Text("Chọn")
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
fun BudgetItemCard(
    budget: Budget,
    onDelete: () -> Unit,
    onToggleRecurring: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ratio = if (budget.limitAmount > 0) (budget.spentAmount / budget.limitAmount).toFloat() else 0f
    
    val catColor = FormatHelper.parseColor(budget.categoryColor)

    // Choose progress color depending on danger ratios
    val progressColor = when {
        ratio >= 1.0f -> Color(0xFFF44336) // Red (overspent)
        ratio >= 0.8f -> Color(0xFFFF9800) // Orange (danger)
        else -> catColor // Category color
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("budget_card_${budget.id}").clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Header (Icon, Name, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
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
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = budget.categoryName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
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

            // Row 2: Recurring Switch & Amount display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("budget_recurring_toggle_${budget.id}")
                ) {
                    Switch(
                        checked = budget.isRecurring,
                        onCheckedChange = { onToggleRecurring() },
                        modifier = Modifier.scale(0.75f)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Tự động",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${FormatHelper.formatVND(budget.spentAmount)} / ${FormatHelper.formatVND(budget.limitAmount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Row 3: Striped Progress Bar
            StripedProgressIndicator(
                progress = ratio.coerceAtMost(1.0f),
                color = progressColor,
                modifier = Modifier.fillMaxWidth().height(10.dp)
            )

            // Overlimit alert warning
            if (ratio >= 1.0f) {
                Text(
                    text = "VƯỢT QUÁ HẠN MỨC!",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun AddBudgetDialog(
    categoriesList: List<FinanceCategory>,
    onDismiss: () -> Unit,
    onAddBudget: (categoryName: String, limitAmount: Double, isRecurring: Boolean) -> Unit
) {
    var selectedCategoryName by remember { mutableStateOf("") }
    var limitInput by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(true) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val expenseCategories = remember(categoriesList) {
        categoriesList.filter { it.type == "EXPENSE" }
    }

    LaunchedEffect(expenseCategories) {
        if (expenseCategories.isNotEmpty() && selectedCategoryName.isBlank()) {
            selectedCategoryName = expenseCategories.first().name
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm hạn mức Ngân sách", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category selection dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hạng mục chi tiêu") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                modifier = Modifier.clickable { categoryDropdownExpanded = true }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { categoryDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        expenseCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryName = cat.name
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Amount input
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = FormatHelper.formatInputNumber(it) },
                    label = { Text("Hạn mức (VNĐ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_budget_limit_input")
                )

                // Recurring switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tự động lặp lại hàng tháng", fontSize = 13.sp)
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = FormatHelper.parseInputNumber(limitInput)
                    if (selectedCategoryName.isNotBlank() && limit > 0) {
                        onAddBudget(selectedCategoryName, limit, isRecurring)
                    }
                },
                modifier = Modifier.testTag("save_budget_confirm_btn")
            ) {
                Text("Lưu Ngân sách")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
