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
    BudgetsSection(viewModel = viewModel, modifier = modifier)
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
