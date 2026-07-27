package com.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
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
import com.app.ui.FinanceViewModel
import com.app.ui.FormatHelper
import com.app.ui.IconMapper
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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

    // Parse month/year for time range calculations
    val monthParts = monthKey.split("-")
    val year = monthParts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
    val month = monthParts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)

    val calRange = Calendar.getInstance()
    calRange.set(Calendar.YEAR, year)
    calRange.set(Calendar.MONTH, month - 1)
    calRange.set(Calendar.DAY_OF_MONTH, 1)

    val startDateStr = String.format("%02d/%02d/%04d", 1, month, year)
    val maxDay = calRange.getActualMaximum(Calendar.DAY_OF_MONTH)
    val endDateStr = String.format("%02d/%02d/%04d", maxDay, month, year)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // Header: System format title & close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chi tiết: $categoryName",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tháng: ${String.format("%02d/%04d", month, year)} • Tổng: ${FormatHelper.formatVND(displayTotal)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            // Highlighted Time Range Banner (Bắt đầu ➔ Kết thúc)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Kỳ hạn mức:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = startDateStr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "➔",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = endDateStr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Transaction List Content
            if (filteredTxs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không có giao dịch nào trong kỳ hạn mức này.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTxs) { tx ->
                        val cat = categories.find { it.name == tx.categoryName }
                        val iconName = cat?.iconName ?: "ic_category_other"
                        val colorHex = cat?.colorHex ?: "#757575"
                        val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color.Gray }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = IconMapper.getIconByName(iconName),
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.categoryName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (tx.note.isNotBlank()) {
                                    Text(
                                        text = tx.note,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }

                                val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                                val dateStr = String.format("%02d/%02d/%04d", txCal.get(Calendar.DAY_OF_MONTH), txCal.get(Calendar.MONTH) + 1, txCal.get(Calendar.YEAR))
                                Text(
                                    text = dateStr,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Text(
                                text = "${if (tx.type == "EXPENSE") "-" else "+"}${FormatHelper.formatVND(tx.amount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (tx.type == "EXPENSE") Color(0xFFE53935) else Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }
    }
}
