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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BudgetsSection(viewModel = viewModel, onNavigateBack = onNavigateBack, modifier = modifier)
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
    var showAddDialog by remember { mutableStateOf(false) }
    var budgetToDelete by remember { mutableStateOf<Budget?>(null) }
    val context = LocalContext.current

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
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

            Text(
                text = "Ngân Sách: $displayBudgetMonth",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                    items(filteredBudgets, key = { it.id }) { budget ->
                        BudgetItemCard(
                            budget = budget,
                            onDelete = { budgetToDelete = budget },
                            onToggleRecurring = { viewModel.toggleBudgetRecurring(budget) }
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
                    android.widget.Toast.makeText(context, "Thêm ngân sách thành công!", android.widget.Toast.LENGTH_SHORT).show()
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
                            android.widget.Toast.makeText(context, "Xóa ngân sách thành công!", android.widget.Toast.LENGTH_SHORT).show()
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
    }
}

@Composable
fun BudgetItemCard(
    budget: Budget,
    onDelete: () -> Unit,
    onToggleRecurring: () -> Unit,
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

            // Row 2: Recurring Switch & Spent/Limit
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = budget.isRecurring,
                        onCheckedChange = { onToggleRecurring() },
                        modifier = Modifier.scale(0.8f) // Reduced scale to keep compact
                    )
                    Text(
                        text = "Tự động",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${FormatHelper.formatVND(budget.spentAmount)} / ${FormatHelper.formatVND(budget.limitAmount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Progress Indicator
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
                        text = "VƯỢT QUÁ HẠN MỨC!",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
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
    onAddBudget: (String, Double, Boolean) -> Unit
) {
    var selectedCategory by remember {
        val defaultExpenseCat = categoriesList.firstOrNull { it.type == "EXPENSE" } ?: categoriesList.firstOrNull()
        mutableStateOf(defaultExpenseCat?.name ?: "Khác")
    }
    var limitStr by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }

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
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                    Text("Tự động tạo mới hàng tháng", fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lm = FormatHelper.evaluateExpression(limitStr)
                    if (lm > 0) {
                        onAddBudget(selectedCategory, lm, isRecurring)
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
