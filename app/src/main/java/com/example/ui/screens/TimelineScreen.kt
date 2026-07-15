package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Transaction
import com.example.ui.FinanceViewModel
import com.example.ui.FormatHelper
import com.example.ui.IconMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    initialDateStr: String
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    
    // Filter for the specific day
    val dayTransactions = remember(allTransactions, initialDateStr) {
        allTransactions
            .filter { FormatHelper.formatDate(it.timestamp) == initialDateStr }
            .sortedByDescending { it.timestamp }
    }
    
    val totalIncome = remember(dayTransactions) {
        dayTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    
    val totalExpense = remember(dayTransactions) {
        dayTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dòng thời gian", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (dayTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = "Chưa có giao dịch", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
            ) {
                item {
                    TimelineDayHeaderUpdated(
                        dateStr = initialDateStr,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TimelineDailySummaryTable(dayTransactions = dayTransactions)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                itemsIndexed(dayTransactions, key = { _, tx -> tx.id }) { index, tx -> 
                    TimelineTransactionUpdated(tx = tx, isLast = index == dayTransactions.size - 1)
                }
            }
        }
    }
}

@Composable
fun TimelineDayHeaderUpdated(dateStr: String, totalIncome: Double, totalExpense: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = dateStr,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Column(horizontalAlignment = Alignment.End) {
            if (totalExpense > 0) {
                Text(
                    text = "-${FormatHelper.formatVND(totalExpense)}",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (totalIncome > 0) {
                Text(
                    text = "+${FormatHelper.formatVND(totalIncome)}",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun TimelineTransactionUpdated(tx: Transaction, isLast: Boolean) {
    val isExpense = tx.type == "EXPENSE"
    val isTransfer = tx.type == "TRANSFER"
    val amountColor = if (isExpense) Color(0xFFF44336) else if (isTransfer) Color(0xFF2196F3) else Color(0xFF4CAF50)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(IntrinsicSize.Min)
    ) {
        // Timeline graphic
        Box(modifier = Modifier.width(48.dp)) {
            // Line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 28.dp) // Start slightly below the center of the circle
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
            // Circle with Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(FormatHelper.parseColor(tx.categoryColor))
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.Center
            ) {
                 Icon(
                     imageVector = IconMapper.getIconByName(tx.categoryIcon),
                     contentDescription = tx.categoryName,
                     tint = Color.White,
                     modifier = Modifier.size(20.dp)
                 )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 32.dp) // Spacing between items
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.categoryName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (tx.note.isNotBlank()) "${tx.walletName} - ${tx.note}" else tx.walletName,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = (if (isExpense) "-" else if (isTransfer) "±" else "+") + FormatHelper.formatVND(tx.amount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatHelper.formatTime(tx.timestamp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineDailySummaryTable(dayTransactions: List<Transaction>) {
    val categoryStats = remember(dayTransactions) {
        dayTransactions.groupBy { Triple(it.categoryName, it.categoryIcon, it.categoryColor) }
            .map { (cat, txs) ->
                val income = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
                val expense = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                Triple(cat, income, expense)
            }
            .filter { it.second > 0 || it.third > 0 }
    }

    val hasIncome = categoryStats.any { it.second > 0 }
    val hasExpense = categoryStats.any { it.third > 0 }

    if (categoryStats.isEmpty()) return

    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(0.8f).fillMaxHeight().padding(8.dp)) 
            
            if (hasIncome) {
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(borderColor))
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Thu", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            if (hasExpense) {
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(borderColor))
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        
        HorizontalDivider(color = borderColor)

        // Data Rows
        categoryStats.forEachIndexed { index, stat ->
            val (cat, income, expense) = stat
            val rowBgColor = try {
                FormatHelper.parseColor(cat.third).copy(alpha = 0.15f)
            } catch (e: Exception) {
                Color.Transparent
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(rowBgColor),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Col
                Row(
                    modifier = Modifier.weight(0.8f).fillMaxHeight().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(FormatHelper.parseColor(cat.third)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = IconMapper.getIconByName(cat.second),
                            contentDescription = cat.first,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = cat.first, 
                        fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }

                if (hasIncome) {
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(borderColor))
                    // Income Col
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = if (income > 0) FormatHelper.formatVND(income) else "",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (hasExpense) {
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(borderColor))
                    // Expense Col
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = if (expense > 0) FormatHelper.formatVND(expense) else "",
                            fontSize = 12.sp,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            if (index < categoryStats.size - 1) {
                HorizontalDivider(color = borderColor)
            }
        }
    }
}
