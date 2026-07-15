package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Debt
import com.example.data.Wallet
import com.example.ui.FinanceViewModel
import com.example.ui.FormatHelper
import java.text.SimpleDateFormat
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtBookScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit
) {
    val debts by viewModel.allDebts.collectAsState()
    val wallets by viewModel.allWallets.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Đi Vay", "Cho Vay")
    
    var showAddDialog by remember { mutableStateOf(false) }
    var debtToPay by remember { mutableStateOf<Debt?>(null) }
    var debtToIncrease by remember { mutableStateOf<Debt?>(null) }

    val filteredDebts = debts.filter { debt ->
        if (selectedTab == 0) debt.type == "DEBT" else debt.type == "LOAN"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Sổ Nợ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Debt")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (filteredDebts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Empty",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Không có khoản nợ nào",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDebts) { debt ->
                        DebtItemCard(
                            debt = debt,
                            onPayClick = { debtToPay = it },
                            onIncreaseClick = { debtToIncrease = it }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDebtDialog(
            viewModel = viewModel,
            wallets = wallets,
            defaultType = if (selectedTab == 0) "DEBT" else "LOAN",
            onDismiss = { showAddDialog = false },
            onAdd = { personName, amount, type, note, dueDate, walletId, repaymentType, periodicAmount, periodType ->
                viewModel.addDebt(personName, amount, type, note, dueDate, walletId, repaymentType, periodicAmount, periodType)
                showAddDialog = false
            }
        )
    }

    debtToPay?.let { debt ->
        PayDebtDialog(
            debt = debt,
            wallets = wallets,
            onDismiss = { debtToPay = null },
            onPay = { amount, walletId, note ->
                viewModel.payDebt(debt, amount, walletId, note)
                debtToPay = null
            }
        )
    }

    debtToIncrease?.let { debt ->
        IncreaseDebtDialog(
            debt = debt,
            wallets = wallets,
            onDismiss = { debtToIncrease = null },
            onIncrease = { amount, walletId, note ->
                viewModel.increaseDebt(debt, amount, walletId, note)
                debtToIncrease = null
            }
        )
    }
}

@Composable
fun DebtItemCard(
    debt: Debt,
    onPayClick: (Debt) -> Unit,
    onIncreaseClick: (Debt) -> Unit
) {
    val formatter = SimpleDateFormat("dd/MM/yyyy", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build())
    val progress = if (debt.totalAmount > 0) ((debt.totalAmount - debt.remainingAmount) / debt.totalAmount).toFloat() else 1f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (debt.type == "DEBT") Color(0xFF9C27B0) else Color(0xFFFF9800),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = debt.personName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = (if (debt.type == "DEBT") "Chủ nợ" else "Người vay") + " • " + when (debt.repaymentType) {
                                "ONE_TIME" -> "Trả 1 lần"
                                "INSTALLMENT" -> "Trả nhiều kỳ"
                                "FLEXIBLE" -> "Linh hoạt"
                                "PERIODIC_FLEXIBLE" -> "Định kỳ"
                                "ACCUMULATING" -> "Nợ cộng dồn"
                                else -> "Linh hoạt"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatHelper.formatVND(debt.remainingAmount),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = if (debt.remainingAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tổng: ${FormatHelper.formatVND(debt.totalAmount)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (debt.dueDate != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hạn: ${formatter.format(debt.dueDate)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (debt.note.isNotBlank()) {
                        Text(
                            text = debt.note,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
                
                if (debt.status == "ACTIVE") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (debt.repaymentType == "ACCUMULATING") {
                            OutlinedButton(
                                onClick = { onIncreaseClick(debt) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Ghi Thêm", fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = { onPayClick(debt) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(if (debt.type == "DEBT") "Trả Nợ" else "Thu Nợ", fontSize = 12.sp)
                        }
                    }
                } else {
                    SuggestionChip(
                        onClick = { },
                        label = { Text("Đã xong", fontSize = 11.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFFE8F5E9),
                            labelColor = Color(0xFF2E7D32)
                        ),
                        border = null
                    )
                }
            }
        }
    }
}

@Composable
fun AddDebtDialog(
    viewModel: FinanceViewModel,
    wallets: List<Wallet>,
    defaultType: String,
    onDismiss: () -> Unit,
    onAdd: (personName: String, amount: Double, type: String, note: String, dueDate: Long?, walletId: Int, repaymentType: String, periodicAmount: Double?, periodType: String?) -> Unit
) {
    val context = LocalContext.current
    var personName by remember { mutableStateOf("") }
    var rawAmount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(defaultType) }
    var note by remember { mutableStateOf("") }
    var dueDateTimestamp by remember { mutableStateOf<Long?>(null) }
    var selectedWalletId by remember { mutableStateOf<Int?>(wallets.firstOrNull()?.id) }
    
    var repaymentType by remember { mutableStateOf("FLEXIBLE") }
    var rawPeriodicAmount by remember { mutableStateOf("") }
    var periodType by remember { mutableStateOf("MONTHLY") }
    
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build()) }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Các loại hình trả nợ", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "• Trả một lần: Có tổng số tiền nợ, thanh toán một lần là kết thúc. (Ví dụ: Mượn bạn 5 triệu, cuối tháng trả đủ 5 triệu)", fontSize = 14.sp)
                    Text(text = "• Trả nhiều kỳ (Installment): Chia thành nhiều kỳ trả cố định. (Ví dụ: Mượn 12 triệu, mỗi tháng trả 1 triệu trong 12 tháng)", fontSize = 14.sp)
                    Text(text = "• Linh hoạt: Không có lịch cố định, mỗi lần trả bao nhiêu cũng được miễn còn dư nợ. (Ví dụ: Khi nào có tiền thì trả 2 triệu, 5 triệu...)", fontSize = 14.sp)
                    Text(text = "• Định kỳ linh hoạt: Có kỳ hạn (hàng tháng) nhưng số tiền mỗi kỳ không cố định, chỉ cần đạt điều kiện tối thiểu. (Ví dụ: Mỗi tháng trả tối thiểu 500k hoặc nhiều hơn)", fontSize = 14.sp)
                    Text(text = "• Nợ cộng dồn: Nợ tăng dần theo thời gian, có thể ghi thêm nợ. (Ví dụ: Ghi sổ mua thêm hàng mỗi ngày)", fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Đã hiểu")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Thêm khoản nợ", fontWeight = FontWeight.Bold)
                IconButton(onClick = { showInfoDialog = true }) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Thông tin", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Switch
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "DEBT",
                        onClick = { type = "DEBT" },
                        label = { Text("Đi Vay") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "LOAN",
                        onClick = { type = "LOAN" },
                        label = { Text("Cho Vay") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text(if (type == "DEBT") "Người cho vay" else "Người vay") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = rawAmount,
                    onValueChange = { rawAmount = it },
                    label = { Text("Số tiền") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Wallet Selector (simplified)
                if (wallets.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedWalletName = wallets.find { it.id == selectedWalletId }?.name ?: "Chọn ví"
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedWalletName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (type == "DEBT") "Tiền chuyển vào ví" else "Lấy tiền từ ví") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { expanded = true }
                        )
                        
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            wallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w.name) },
                                    onClick = { selectedWalletId = w.id; expanded = false }
                                )
                            }
                        }
                    }
                }
                
                // Due Date
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dueDateTimestamp?.let { dateFormatter.format(it) } ?: "Không có",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hạn trả") },
                        trailingIcon = {
                            if (dueDateTimestamp != null) {
                                IconButton(onClick = { dueDateTimestamp = null }) {
                                    Icon(Icons.Default.Close, "Xóa")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(end = 48.dp)
                            .background(Color.Transparent)
                            .clickable {
                                val cal = Calendar.getInstance()
                                dueDateTimestamp?.let { cal.timeInMillis = it }
                                DatePickerDialog(context, { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                    dueDateTimestamp = newCal.timeInMillis
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            }
                    )
                }
                
                // Repayment Type Selector
                var expandedRepayment by remember { mutableStateOf(false) }
                val repaymentMap = mapOf(
                    "ONE_TIME" to "Trả 1 lần",
                    "INSTALLMENT" to "Trả nhiều kỳ",
                    "FLEXIBLE" to "Linh hoạt",
                    "PERIODIC_FLEXIBLE" to "Định kỳ linh hoạt",
                    "ACCUMULATING" to "Nợ cộng dồn"
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = repaymentMap[repaymentType] ?: "Linh hoạt",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Loại hình trả nợ") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedRepayment = true }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Transparent)
                            .clickable { expandedRepayment = true }
                    )
                    
                    DropdownMenu(expanded = expandedRepayment, onDismissRequest = { expandedRepayment = false }) {
                        repaymentMap.forEach { (key, value) ->
                            DropdownMenuItem(
                                text = { Text(value) },
                                onClick = { repaymentType = key; expandedRepayment = false }
                            )
                        }
                    }
                }

                if (repaymentType == "PERIODIC_FLEXIBLE" || repaymentType == "INSTALLMENT") {
                    OutlinedTextField(
                        value = rawPeriodicAmount,
                        onValueChange = { rawPeriodicAmount = it },
                        label = { Text("Số tiền mỗi kỳ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    var expandedPeriod by remember { mutableStateOf(false) }
                    val periodMap = mapOf(
                        "WEEKLY" to "Hàng tuần",
                        "MONTHLY" to "Hàng tháng",
                        "YEARLY" to "Hàng năm"
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = periodMap[periodType] ?: "Hàng tháng",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kỳ hạn") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedPeriod = true }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { expandedPeriod = true }
                        )
                        DropdownMenu(expanded = expandedPeriod, onDismissRequest = { expandedPeriod = false }) {
                            periodMap.forEach { (key, value) ->
                                DropdownMenuItem(
                                    text = { Text(value) },
                                    onClick = { periodType = key; expandedPeriod = false }
                                )
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = rawAmount.toDoubleOrNull() ?: 0.0
                    val periodicAmt = rawPeriodicAmount.toDoubleOrNull()
                    if (personName.isNotBlank() && amt > 0 && selectedWalletId != null) {
                        onAdd(personName, amt, type, note, dueDateTimestamp, selectedWalletId!!, repaymentType, periodicAmt, periodType)
                    } else {
                        viewModel.showWarningNotification("Vui lòng điền đủ tên, số tiền và chọn ví")
                    }
                }
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun PayDebtDialog(
    debt: Debt,
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onPay: (amount: Double, walletId: Int, note: String) -> Unit
) {
    var rawAmount by remember { mutableStateOf(debt.remainingAmount.toLong().toString()) }
    var selectedWalletId by remember { mutableStateOf<Int?>(wallets.firstOrNull()?.id) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (debt.type == "DEBT") "Trả nợ cho ${debt.personName}" else "Thu nợ từ ${debt.personName}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Số tiền còn lại: ${FormatHelper.formatVND(debt.remainingAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                OutlinedTextField(
                    value = rawAmount,
                    onValueChange = { rawAmount = it },
                    label = { Text("Số tiền trả") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Wallet Selector (simplified)
                if (wallets.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedWalletName = wallets.find { it.id == selectedWalletId }?.name ?: "Chọn ví"
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedWalletName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (debt.type == "DEBT") "Trừ tiền từ ví" else "Cộng tiền vào ví") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { expanded = true }
                        )
                        
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            wallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w.name) },
                                    onClick = { selectedWalletId = w.id; expanded = false }
                                )
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = rawAmount.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && selectedWalletId != null) {
                        onPay(amt, selectedWalletId!!, note)
                    }
                }
            ) {
                Text("Xác nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun IncreaseDebtDialog(
    debt: Debt,
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onIncrease: (amount: Double, walletId: Int, note: String) -> Unit
) {
    var rawAmount by remember { mutableStateOf("") }
    var selectedWalletId by remember { mutableStateOf<Int?>(wallets.firstOrNull()?.id) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (debt.type == "DEBT") "Ghi thêm nợ từ ${debt.personName}" else "Cho ${debt.personName} vay thêm", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Dư nợ hiện tại: ${FormatHelper.formatVND(debt.remainingAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                OutlinedTextField(
                    value = rawAmount,
                    onValueChange = { rawAmount = it },
                    label = { Text("Số tiền phát sinh") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (wallets.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedWalletName = wallets.find { it.id == selectedWalletId }?.name ?: "Chọn ví"
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedWalletName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (debt.type == "DEBT") "Tiền được chuyển vào ví" else "Trích tiền từ ví") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { expanded = true }
                        )
                        
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            wallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w.name) },
                                    onClick = { selectedWalletId = w.id; expanded = false }
                                )
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = rawAmount.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && selectedWalletId != null) {
                        onIncrease(amt, selectedWalletId!!, note)
                    }
                }
            ) {
                Text("Ghi Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
