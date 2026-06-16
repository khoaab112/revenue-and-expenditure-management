package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FinanceCategory
import com.example.ui.FinanceViewModel
import com.example.ui.FormatHelper
import com.example.ui.IconMapper
import java.util.*

@Composable
fun CategoryTransactionsDialog(
    categoryName: String,
    monthKey: String, // format yyyy-MM
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val dailyTransactions by viewModel.dailyTransactions.collectAsState()
    val categories by viewModel.categoriesList.collectAsState()
    
    // Filter transactions
    val categoryNamesToMatch = mutableSetOf(categoryName)
    val parentCategory = categories.find { it.name == categoryName }
    if (parentCategory != null) {
        val childNames = categories.filter { it.parentName == categoryName }.map { it.name }
        categoryNamesToMatch.addAll(childNames)
    }

    val filteredTxs = dailyTransactions.filter { tx ->
        val cal = Calendar.getInstance()
        cal.timeInMillis = tx.timestamp
        val txMonthStr = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        
        txMonthStr == monthKey && tx.categoryName in categoryNamesToMatch
    }.sortedByDescending { it.timestamp }
    
    val totalAmount = filteredTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val totalIncome = filteredTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
    
    val displayTotal = if (totalAmount > 0) totalAmount else totalIncome

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chi tiết: $categoryName",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Tháng: ${monthKey.split("-")[1]}/${monthKey.split("-")[0]} - Tổng: ${FormatHelper.formatVND(displayTotal)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                
                if (filteredTxs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Không có giao dịch nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredTxs) { tx ->
                            val cat = categories.find { it.name == tx.categoryName }
                            val iconName = cat?.iconName ?: "ic_category_other"
                            val colorHex = cat?.colorHex ?: "#757575"
                            val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color.Gray }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = IconMapper.getIconByName(iconName),
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.categoryName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (tx.note.isNotBlank()) {
                                        Text(
                                            text = tx.note,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    
                                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                                    val dateStr = String.format("%02d/%02d/%04d", txCal.get(Calendar.DAY_OF_MONTH), txCal.get(Calendar.MONTH) + 1, txCal.get(Calendar.YEAR))
                                    Text(
                                        text = dateStr,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                
                                Text(
                                    text = "${if (tx.type == "EXPENSE") "-" else "+"}${FormatHelper.formatVND(tx.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (tx.type == "EXPENSE") Color(0xFFE53935) else Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
