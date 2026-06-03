package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FinanceViewModel
import com.example.ui.FormatHelper
import com.example.ui.components.CustomMoneyInputField
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsVaultScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val dailyWallets by viewModel.dailyWallets.collectAsState()
    val savingsWallets by viewModel.savingsWallets.collectAsState()
    val savingsTransactions by viewModel.savingsTransactions.collectAsState()
    
    // Quick Add Savings Wallet State
    var showQuickAddWallet by remember { mutableStateOf(false) }
    var newSavingsWalletName by remember { mutableStateOf("") }
    var newSavingsWalletGoalStr by remember { mutableStateOf("") }
    
    var savingsWalletToDelete by remember { mutableStateOf<com.example.data.Wallet?>(null) }

    // Detail View State
    var selectedVaultDetails by remember { mutableStateOf<com.example.data.Wallet?>(null) }
    var selectedTabIndex by remember { mutableStateOf(0) } // 0 = Lịch sử, 1 = Giao dịch
    
    // Transaction Panel State
    var isDeposit by remember { mutableStateOf(true) } // true for Deposit (Gửi), false for withdraw (Rút)
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDailyWalletId by remember { mutableStateOf<Int?>(null) } // Everyday transaction source / target

    if (savingsWalletToDelete != null) {
        AlertDialog(
            onDismissRequest = { savingsWalletToDelete = null },
            title = { Text("Xác nhận xóa hũ tiết kiệm?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa hũ tiết kiệm '${savingsWalletToDelete?.name}'? Hành động này cũng sẽ xóa lịch sử tích lũy liên quan và không thể phục hồi.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        savingsWalletToDelete?.let { wallet ->
                            viewModel.deleteWallet(wallet)
                            viewModel.showSuccessNotification("Xóa hũ tiết kiệm thành công")
                            if (selectedVaultDetails?.id == wallet.id) {
                                selectedVaultDetails = null
                            }
                        }
                        savingsWalletToDelete = null
                    }
                ) {
                    Text("XÓA", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { savingsWalletToDelete = null }) {
                    Text("HỦY")
                }
            }
        )
    }

    val totalSavings = remember(savingsWallets) { savingsWallets.sumOf { it.balance } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QUẢN LÝ SỔ & HŨ TIẾT KIỆM", fontSize = 18.sp, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // SAVINGS KPI TOTAL ACCUMULATOR
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Tổng quỹ tiết kiệm tích lũy", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = FormatHelper.formatVND(totalSavings),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                // --- SAVINGS WALLETS COLUMN ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CÁC HŨ TIẾT KIỆM HIỆN CÓ", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { showQuickAddWallet = !showQuickAddWallet }) {
                        Icon(imageVector = if (showQuickAddWallet) Icons.Default.Remove else Icons.Default.AddCircle, contentDescription = "Toggle add", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (showQuickAddWallet) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("KHỞI TẠO HŨ TIẾT KIỆM MỚI", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            OutlinedTextField(
                                value = newSavingsWalletName,
                                onValueChange = { newSavingsWalletName = it },
                                label = { Text("Tên hũ tích lũy") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            CustomMoneyInputField(
                                value = newSavingsWalletGoalStr,
                                onValueChange = { newSavingsWalletGoalStr = it },
                                label = "Số dư tích lũy ban đầu (đ)",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (newSavingsWalletName.isNotBlank()) {
                                        val initialBalance = newSavingsWalletGoalStr.toDoubleOrNull() ?: 0.0
                                        viewModel.addWallet(
                                            name = newSavingsWalletName,
                                            type = "SAVINGS",
                                            initialBalance = initialBalance,
                                            colorHex = "#9C27B0", // Savings Purple standard
                                            iconName = "Savings"
                                        )
                                        viewModel.showSuccessNotification("Khởi tạo hũ tích lũy thành công!")
                                        newSavingsWalletName = ""
                                        newSavingsWalletGoalStr = ""
                                        showQuickAddWallet = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Khởi tạo hũ")
                            }
                        }
                    }
                }
            }

            if (savingsWallets.isEmpty()) {
                item {
                    Text("Chưa có hũ tiết kiệm nào. Vui lòng bấm dấu (+) bên trên để khởi tạo hũ!", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(savingsWallets, key = { it.id }) { wt ->
                    val isSelected = selectedVaultDetails?.id == wt.id
                    ListItem(
                        headlineContent = { Text(wt.name, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Hũ Tích Lũy • ${FormatHelper.formatVND(wt.balance)}") },
                        leadingContent = {
                            Icon(imageVector = Icons.Default.Savings, contentDescription = "Savings", tint = FormatHelper.parseColor(wt.colorHex))
                        },
                        trailingContent = {
                            IconButton(onClick = { savingsWalletToDelete = wt }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { 
                                if (isSelected) {
                                    selectedVaultDetails = null
                                } else {
                                    selectedVaultDetails = wt
                                    selectedTabIndex = 0
                                }
                            }
                    )
                }
            }

            // TABS AND CONTENT FOR SELECTED WALLET
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
                
                if (selectedVaultDetails != null) {
                    val vaultDetails = savingsWallets.find { it.id == selectedVaultDetails?.id } ?: selectedVaultDetails!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HŨ ĐANG CHỌN: ${vaultDetails.name.uppercase()}", 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Black, 
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Số dư: ${FormatHelper.formatVND(vaultDetails.balance)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                TabRow(selectedTabIndex = selectedTabIndex, modifier = Modifier.padding(top = 8.dp)) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Lịch sử", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Giao dịch", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (selectedVaultDetails == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = "Empty",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Hãy chọn hũ để xem chi tiết",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val vaultDetails = savingsWallets.find { it.id == selectedVaultDetails?.id } ?: selectedVaultDetails!!
                val specificVaultTxs = savingsTransactions.filter { it.walletId == vaultDetails.id }

                if (selectedTabIndex == 0) {
                    if (specificVaultTxs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Không có lịch sử biến động.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        val groupedTxs = specificVaultTxs.groupBy { FormatHelper.formatDate(it.timestamp) }
                        groupedTxs.forEach { (dateStr, txList) ->
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Date",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = dateStr,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            items(txList, key = { it.id }) { tx ->
                                val isIncome = tx.type == "INCOME"
                                val statusColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
                                ListItem(
                                    headlineContent = { 
                                        Text(
                                            text = tx.categoryName, 
                                            fontWeight = FontWeight.Medium, 
                                            fontSize = 13.sp, 
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ) 
                                    },
                                    supportingContent = {
                                        Text(
                                            text = "${tx.walletName}${if(tx.note.isNotBlank()) " • " + tx.note else ""} • ${SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(tx.timestamp)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    },
                                    leadingContent = {
                                        Icon(
                                            imageVector = if (isIncome) Icons.Default.Add else Icons.Default.Remove,
                                            contentDescription = tx.type,
                                            tint = statusColor
                                        )
                                    },
                                    trailingContent = {
                                        Text(
                                            text = "${if (isIncome) "+" else "-"}${FormatHelper.formatVND(tx.amount)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = statusColor
                                        )
                                    },
                                    modifier = Modifier
                                        .padding(vertical = 2.dp)
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                } else if (selectedTabIndex == 1) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Gửi hoặc Rút Tab rows
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { isDeposit = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDeposit) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isDeposit) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Text("Nạp", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Button(
                                        onClick = { isDeposit = false },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!isDeposit) Color(0xFFF44336) else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (!isDeposit) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Text("Rút", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }

                                CustomMoneyInputField(
                                    value = amountStr,
                                    onValueChange = { amountStr = it },
                                    label = "Số tiền",
                                    modifier = Modifier.fillMaxWidth().testTag("savings_amount_input")
                                )

                                OutlinedTextField(
                                    value = note,
                                    onValueChange = { note = it },
                                    label = { Text("Nội dung ghi chú") },
                                    modifier = Modifier.fillMaxWidth().testTag("savings_note_input")
                                )

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isDeposit) "Trích xuất từ ví thường:" else "Chuyển tiền về ví thường:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        val checkedVal = selectedDailyWalletId != null
                                        Switch(
                                            checked = checkedVal,
                                            onCheckedChange = { isChecked ->
                                                if (isChecked && dailyWallets.isNotEmpty()) {
                                                    selectedDailyWalletId = dailyWallets.first().id
                                                } else {
                                                    selectedDailyWalletId = null
                                                }
                                            }
                                        )
                                    }
                                    
                                    if (selectedDailyWalletId != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            dailyWallets.forEach { wt ->
                                                FilterChip(
                                                    selected = selectedDailyWalletId == wt.id,
                                                    onClick = { selectedDailyWalletId = wt.id },
                                                    label = { Text("${wt.name} (${FormatHelper.formatVND(wt.balance)})") }
                                                )
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                                        val tgtWalletId = vaultDetails.id
                                        if (amount > 0.0) {
                                            val now = System.currentTimeMillis()
                                            
                                            if (isDeposit) {
                                                viewModel.addTransaction(
                                                    walletId = tgtWalletId,
                                                    type = "INCOME",
                                                    amount = amount,
                                                    categoryName = "Tiết kiệm",
                                                    note = note.ifEmpty { "Gửi tiền hũ tiết kiệm" },
                                                    timestamp = now
                                                )
                                                
                                                selectedDailyWalletId?.let { srcId ->
                                                    viewModel.addTransaction(
                                                        walletId = srcId,
                                                        type = "EXPENSE",
                                                        amount = amount,
                                                        categoryName = "Tiết kiệm",
                                                        note = note.ifEmpty { "Nạp quỹ tiết kiệm" },
                                                        timestamp = now
                                                    )
                                                }
                                            } else {
                                                viewModel.addTransaction(
                                                    walletId = tgtWalletId,
                                                    type = "EXPENSE",
                                                    amount = amount,
                                                    categoryName = "Tiết kiệm",
                                                    note = note.ifEmpty { "Rút tiền hũ tiết kiệm" },
                                                    timestamp = now
                                                )
                                                
                                                selectedDailyWalletId?.let { destId ->
                                                    viewModel.addTransaction(
                                                        walletId = destId,
                                                        type = "INCOME",
                                                        amount = amount,
                                                        categoryName = "Tiết kiệm",
                                                        note = note.ifEmpty { "Nhận tiền từ hũ" },
                                                        timestamp = now
                                                    )
                                                }
                                            }
                                            
                                            amountStr = ""
                                            note = ""
                                            selectedDailyWalletId = null
                                            focusManager.clearFocus()
                                            viewModel.showSuccessNotification("Thực hiện giao dịch thành công!")
                                        } else {
                                            viewModel.showWarningNotification("Vui lòng nhập số tiền lớn hơn 0")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("add_savings_transaction_confirm"),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isDeposit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                ) {
                                    Text(if (isDeposit) "Thực hiện Nạp" else "Thực hiện Rút", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp)) // bottom padding
                }
            }
        }
    }
}
