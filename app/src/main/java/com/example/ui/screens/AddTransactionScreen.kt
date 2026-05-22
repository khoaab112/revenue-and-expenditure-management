package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Categories
import com.example.ui.FinanceViewModel
import com.example.ui.FormatHelper
import com.example.ui.IconMapper
import java.util.Calendar

@Composable
fun AddTransactionScreen(
    viewModel: FinanceViewModel,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wallets by viewModel.allWallets.collectAsState()

    var amountStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME
    var selectedWalletId by remember { mutableStateOf<Int?>(null) }
    var selectedCategoryName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    // Recurring Setup
    var isRecurring by remember { mutableStateOf(false) }
    var recurrencePeriod by remember { mutableStateOf("DAILY") } // DAILY, WEEKLY, MONTHLY

    // Date Picker Setup
    val calendar = remember { Calendar.getInstance() }
    var selectedTimestamp by remember { mutableStateOf(calendar.timeInMillis) }
    var dateLabel by remember { mutableStateOf("Hôm nay") }

    // Auto-select first wallet if available
    LaunchedEffect(wallets) {
        if (selectedWalletId == null && wallets.isNotEmpty()) {
            selectedWalletId = wallets.first().id
        }
    }

    // Filter categories depending on type
    val filteredCategories = remember(selectedType) {
        val list = Categories.list.filter { it.type == selectedType || it.type == "BOTH" }
        if (list.isNotEmpty()) {
            selectedCategoryName = list.first().name
        }
        list
    }

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

                val todayCal = Calendar.getInstance()
                val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

                dateLabel = if (newCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    newCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)) {
                    "Hôm nay"
                } else if (newCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                    newCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)) {
                    "Hôm qua"
                } else {
                    String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form Title
        Text(
            text = "Thêm Giao Dịch Mới",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Type Switch Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { selectedType = "EXPENSE" },
                modifier = Modifier.weight(1f).testTag("select_expense_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == "EXPENSE") Color(0xFFF44336)
                                     else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selectedType == "EXPENSE") Color.White
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Chi")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Khoản Chi")
            }

            Button(
                onClick = { selectedType = "INCOME" },
                modifier = Modifier.weight(1f).testTag("select_income_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == "INCOME") Color(0xFF4CAF50)
                                     else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selectedType == "INCOME") Color.White
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Thu")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Khoản Thu")
            }
        }

        // --- Large Money Amount Entry Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SỐ TIỀN PHÁT SINH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { newVal -> amountStr = newVal },
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == "EXPENSE") Color(0xFFF44336) else Color(0xFF4CAF50),
                            textAlign = TextAlign.Center
                        ),
                        placeholder = {
                            Text(
                                text = "0",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_amount_text_field"),
                        singleLine = true
                    )

                    Text(
                        text = "₫",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        // --- Wallet Selector ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Tài khoản thanh toán / nhận tiền",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (wallets.isEmpty()) {
                Text(
                    text = "Không tìm thấy ví khả dụng. Chọn mục Tài khoản để tạo mới.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    wallets.forEach { wt ->
                        val isSelected = selectedWalletId == wt.id
                        val borderColor = if (isSelected) FormatHelper.parseColor(wt.colorHex)
                                          else MaterialTheme.colorScheme.outlineVariant
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedWalletId = wt.id },
                            label = {
                                Text("${wt.name} (${FormatHelper.formatVND(wt.balance)})")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = IconMapper.getIconByName(wt.iconName),
                                    contentDescription = wt.name,
                                    tint = if (isSelected) Color.White else FormatHelper.parseColor(wt.colorHex),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FormatHelper.parseColor(wt.colorHex).copy(alpha = 0.25f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = FormatHelper.parseColor(wt.colorHex),
                                borderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.testTag("tx_wallet_chip_${wt.id}")
                        )
                    }
                }
            }
        }

        // --- Category Selector (Visual grid) ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Hạng mục / Danh mục chi thu",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredCategories.forEach { cat ->
                    val isSelected = selectedCategoryName == cat.name
                    val categoryColor = FormatHelper.parseColor(cat.colorHex)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { selectedCategoryName = cat.name }
                            .padding(vertical = 4.dp)
                            .testTag("category_select_${cat.name}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) categoryColor
                                    else categoryColor.copy(alpha = 0.15f)
                                )
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else categoryColor,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = IconMapper.getIconByName(cat.iconName),
                                contentDescription = cat.name,
                                tint = if (isSelected) Color.White else categoryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cat.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // --- Notes Input field ---
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Ghi chú hóa đơn / mô tả") },
            leadingIcon = { Icon(imageVector = Icons.Default.EditNote, contentDescription = "Note") },
            modifier = Modifier.fillMaxWidth().testTag("tx_note_input"),
            singleLine = true
        )

        // --- Dates Inline Selection Row ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Thời gian phát sinh",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Today Button
                FilterChip(
                    selected = dateLabel == "Hôm nay",
                    onClick = {
                        selectedTimestamp = Calendar.getInstance().timeInMillis
                        dateLabel = "Hôm nay"
                    },
                    label = { Text("Hôm nay") }
                )

                // Yesterday Button
                FilterChip(
                    selected = dateLabel == "Hôm qua",
                    onClick = {
                        selectedTimestamp = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
                        dateLabel = "Hôm qua"
                    },
                    label = { Text("Hôm qua") }
                )

                // Calendar Dialog Button
                FilterChip(
                    selected = dateLabel != "Hôm nay" && dateLabel != "Hôm qua",
                    onClick = { datePickerDialog.show() },
                    label = { Text(if (dateLabel == "Hôm nay" || dateLabel == "Hôm qua") "Khác..." else dateLabel) },
                    leadingIcon = { Icon(imageVector = Icons.Default.DateRange, contentDescription = "Calendar", modifier = Modifier.size(16.dp)) }
                )
            }
        }

        // --- Recurring Transaction Setup Section ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Repeat, contentDescription = "Recurring", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Thiết lập giao dịch định kỳ", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "Tự động sao chép phát sinh trong tương lai", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it },
                    modifier = Modifier.testTag("tx_recurring_switch")
                )
            }

            if (isRecurring) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "DAILY" to "Hàng ngày",
                        "WEEKLY" to "Hàng tuần",
                        "MONTHLY" to "Hàng tháng"
                    ).forEach { (freq, name) ->
                        FilterChip(
                            selected = recurrencePeriod == freq,
                            onClick = { recurrencePeriod = freq },
                            label = { Text(name) },
                            modifier = Modifier.testTag("tx_recurrence_$freq")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Button(
            onClick = {
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                val walletId = selectedWalletId
                if (amount > 0 && walletId != null) {
                    viewModel.addTransaction(
                        walletId = walletId,
                        type = selectedType,
                        amount = amount,
                        categoryName = selectedCategoryName,
                        note = note,
                        timestamp = selectedTimestamp,
                        isRecurring = isRecurring,
                        recurrencePeriod = if (isRecurring) recurrencePeriod else "NONE"
                    )
                    onSuccess()
                }
            },
            enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0.0 && selectedWalletId != null,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_transaction_btn"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "LƯU GIAO DỊCH",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
