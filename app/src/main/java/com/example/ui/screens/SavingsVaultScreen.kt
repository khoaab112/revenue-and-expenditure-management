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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    
    // Transaction Panel State
    var selectedWalletId by remember { mutableStateOf<Int?>(null) }
    var isDeposit by remember { mutableStateOf(true) } // true for Deposit (Gửi), false for withdraw (Rút)
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDailyWalletId by remember { mutableStateOf<Int?>(null) } // Everyday transaction source / target
    
    var savingsWalletToDelete by remember { mutableStateOf<com.example.data.Wallet?>(null) }

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
                            android.widget.Toast.makeText(context, "Xóa hũ tiết kiệm thành công", android.widget.Toast.LENGTH_SHORT).show()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

            if (showQuickAddWallet) {
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
                                    android.widget.Toast.makeText(context, "Khởi tạo hũ tích lũy thành công!", android.widget.Toast.LENGTH_SHORT).show()
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

            if (savingsWallets.isEmpty()) {
                Text("Chưa có hũ tiết kiệm nào. Vui lòng bấm dấu (+) bên trên để khởi tạo hũ!", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                savingsWallets.forEach { wt ->
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
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // --- SAVINGS TRANSACTION ACTION FORM ---
            if (savingsWallets.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("THỰC HIỆN GIAO DỊCH TIẾT KIỆM", fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        
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

                        // Choose Savings target wallet
                        Column {
                            Text("Lựa Chọn Hũ Tiết Kiệm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (selectedWalletId == null && savingsWallets.isNotEmpty()) {
                                    selectedWalletId = savingsWallets.first().id
                                }
                                
                                savingsWallets.forEach { wt ->
                                    FilterChip(
                                        selected = selectedWalletId == wt.id,
                                        onClick = { selectedWalletId = wt.id },
                                        label = { Text(wt.name) }
                                    )
                                }
                            }
                        }

                        // Amount input
                        CustomMoneyInputField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = "Số tiền",
                            modifier = Modifier.fillMaxWidth().testTag("savings_amount_input")
                        )

                        // Note input
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Nội dung ghi chú") },
                            modifier = Modifier.fillMaxWidth().testTag("savings_note_input")
                        )

                        // Source Wallet option: Trích xuất từ ví hằng ngày hay không?
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
                                val tgtWalletId = selectedWalletId
                                if (amount > 0.0 && tgtWalletId != null) {
                                    val now = System.currentTimeMillis()
                                    
                                    if (isDeposit) {
                                        // Add Income transaction on the Savings Wallet
                                        viewModel.addTransaction(
                                            walletId = tgtWalletId,
                                            type = "INCOME",
                                            amount = amount,
                                            categoryName = "Tiết kiệm",
                                            note = note.ifEmpty { "Gửi tiền hũ tiết kiệm" },
                                            timestamp = now
                                        )
                                        
                                        // Subtract Expense on the Everyday Source Wallet (if selected)
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
                                        // Add Expense transaction on the Savings Wallet
                                        viewModel.addTransaction(
                                            walletId = tgtWalletId,
                                            type = "EXPENSE",
                                            amount = amount,
                                            categoryName = "Tiết kiệm",
                                            note = note.ifEmpty { "Rút tiền hũ tiết kiệm" },
                                            timestamp = now
                                        )
                                        
                                        // Add Income on the Everyday Dest Wallet (if selected)
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
                                    
                                    // Reset inputs
                                    amountStr = ""
                                    note = ""
                                    selectedDailyWalletId = null
                                    
                                    android.widget.Toast.makeText(context, "Thực hiện giao dịch thành công!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("add_savings_transaction_confirm"),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDeposit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        ) {
                            Text(if (isDeposit) "Gửi Tiết Kiệm" else "Rút Tiết Kiệm")
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // --- SAVINGS TRANSACTION TIMELINE HISTORY ---
            Text("LỊCH SỬ GIAO DỊCH TIẾT KIỆM", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            
            if (savingsTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Không có lịch sử biến động hũ tiết kiệm.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val groupedTxs = remember(savingsTransactions) {
                    savingsTransactions.groupBy { FormatHelper.formatDate(it.timestamp) }
                }
                groupedTxs.forEach { (dateStr, txList) ->
                    // Timeline milestone / date group header
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

                    txList.forEach { tx ->
                        ListItem(
                            headlineContent = { 
                                Text(
                                    text = tx.note, 
                                    fontWeight = FontWeight.Medium, 
                                    fontSize = 13.sp, 
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                ) 
                            },
                            supportingContent = {
                                Text(
                                    text = "${tx.walletName} • ${SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(tx.timestamp)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = if (tx.type == "INCOME") Icons.Default.Add else Icons.Default.Remove,
                                    contentDescription = tx.type,
                                    tint = if (tx.type == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            },
                            trailingContent = {
                                Text(
                                    text = "${if (tx.type == "INCOME") "+" else "-"}${FormatHelper.formatVND(tx.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (tx.type == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            },
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}
