package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FinanceViewModel
import com.example.ui.FormatHelper
import com.example.ui.NotificationLog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankNotificationHistoryScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notificationLogs by viewModel.notificationLogs.collectAsState()
    val wallets by viewModel.allWallets.collectAsState()
    val categories by viewModel.categoriesList.collectAsState()

    var selectedFilterStatus by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Section collapsing states
    var showSimulationPanel by remember { mutableStateOf(false) }
    var showHistoryPanel by remember { mutableStateOf(true) }

    // Simulation inputs state
    var simTitle by remember { mutableStateOf("Vietcombank") }
    var simText by remember { mutableStateOf("TK 1012938475 +5,000,000 VND luc 14:32. ND: Chuyen khoan luong thang 5") }
    var simPackage by remember { mutableStateOf("com.vietcombank.card") }

    // Active expanded pending log timestamp tracker
    var expandedPendingTimestamp by remember { mutableStateOf<Long?>(null) }

    var showBulkApproveDialog by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var bulkSelectedWalletId by remember { mutableStateOf<Int?>(null) }

    // Partition logs
    val pendingLogs = remember(notificationLogs) {
        notificationLogs.filter { it.status == "PENDING" }
    }
    
    val processedLogs = remember(notificationLogs, selectedFilterStatus, searchQuery) {
        notificationLogs.filter { log ->
            if (log.status == "PENDING") return@filter false
            
            val matchesStatus = when (selectedFilterStatus) {
                "ALL" -> true
                else -> log.status == selectedFilterStatus
            }
            val matchesSearch = log.text.lowercase().contains(searchQuery.lowercase()) ||
                    log.bankName.lowercase().contains(searchQuery.lowercase()) ||
                    log.note.lowercase().contains(searchQuery.lowercase())
            matchesStatus && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tin nhắn ngân hàng",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Duyệt giao dịch từ thông báo biến động số dư",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("bank_history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                actions = {
                    if (notificationLogs.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("clear_all_logs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Xóa tất cả nhật ký",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // --- SECTION 1: SIMULATOR TRIGGER (Collapsible Simulation) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSimulationPanel = !showSimulationPanel }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = "Simulate",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Mô phỏng thông báo thử nghiệm",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (showSimulationPanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Panel",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(
                            visible = showSimulationPanel,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = simTitle,
                                    onValueChange = { simTitle = it },
                                    label = { Text("Tiêu đề (Ví dụ: Vietcombank, Momo)", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                )

                                OutlinedTextField(
                                    value = simText,
                                    onValueChange = { simText = it },
                                    label = { Text("Nội dung tin nhắn nhận được", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                )

                                Text(
                                    text = "Tin mẫu nhanh hoặc ngân hàng khác:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val bankTemplates = listOf(
                                        Triple("VCB +5Tr", "TK 10129384 +5,000,000 VND luc 14:32. ND: Chuyen khoan luong thang 5", "com.vietcombank.card"),
                                        Triple("VCB -250k", "TK 10129384 -250,000 VND luc 15:40. ND: Rut tien tai ATM", "com.vietcombank.card"),
                                        Triple("Momo +20k", "Bạn vừa nhận được 20.000đ từ Nguyễn Văn B.", "com.mservice.momo"),
                                        Triple("Momo -55k", "Thanh toán thành công 55.000đ cho GrabFood qua Momo.", "com.mservice.momo"),
                                        Triple("MB Bank +100k", "MB: +100,000 VND vao 12:00. ND: Giao dich chuyen tien don do an", "com.mbbank")
                                    )

                                    bankTemplates.forEach { (label, content, pkg) ->
                                        AssistChip(
                                            onClick = {
                                                simTitle = label.split(" ")[0]
                                                simText = content
                                                simPackage = pkg
                                            },
                                            label = { Text(label, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        viewModel.simulateBankNotification(simTitle, simText, simPackage)
                                        android.widget.Toast.makeText(context, "Đã gửi thông báo mô phỏng dạng PENDING!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SendToMobile,
                                        contentDescription = "Push",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gửi thông báo mô phỏng", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // --- SECTION 2: GIAO DỊCH QUÉT ĐƯỢC (PENDING TRANSACTIONS BLOCK) ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PendingActions,
                        contentDescription = "Pending actions",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GIAO DỊCH QUÉT ĐƯỢC CHỜ DUYỆT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFE65100)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = CircleShape
                    ) {
                        Text(
                            text = pendingLogs.size.toString(),
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (pendingLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = "All done",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sạch sẽ! Không có giao dịch nào cần phê duyệt",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Lịch sử tin nhắn nhận được sẽ hiển thị tại đây để bạn phê duyệt vào ví nhanh chóng.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
                            )
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (wallets.isEmpty()) {
                                    android.widget.Toast.makeText(context, "Vui lòng tạo ví tài khoản trước!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    bulkSelectedWalletId = wallets.firstOrNull()?.id ?: 0
                                    showBulkApproveDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Duyệt hàng loạt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showBulkDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Xóa hàng loạt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(pendingLogs, key = { it.timestamp }) { log ->
                    val isExpanded = expandedPendingTimestamp == log.timestamp

                    // Local state for approval targets
                    val matchedWallet = remember(log, wallets) {
                        wallets.find { it.name.lowercase().contains(log.bankName.lowercase()) }
                            ?: wallets.find { it.name.lowercase().contains(log.walletName.lowercase()) }
                            ?: wallets.find { it.type == "BANK" }
                            ?: wallets.firstOrNull()
                    }

                    var selectedWalletId by remember(log, matchedWallet) {
                        mutableStateOf(matchedWallet?.id ?: 0)
                    }

                    val matchedCategory = remember(log, categories) {
                        categories.find { it.name.lowercase() == log.note.lowercase() }
                            ?: categories.find { it.type == log.type }
                            ?: categories.firstOrNull()
                    }

                    var selectedCategoryName by remember(log, matchedCategory) {
                        mutableStateOf(matchedCategory?.name ?: "")
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedPendingTimestamp = if (isExpanded) null else log.timestamp
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = if (isExpanded) 1.5.dp else 1.dp,
                            color = if (isExpanded) Color(0xFFE65100).copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Underneath Layer: Normal item content
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Top Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    if (log.type == "INCOME") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (log.type == "INCOME") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                contentDescription = log.type,
                                                tint = if (log.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = log.bankName.ifEmpty { "Ngân hàng" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            val timeStr = remember(log.timestamp) {
                                                SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                                            }
                                            Text(
                                                text = timeStr,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        color = Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "CHỜ DUYỆT",
                                            color = Color(0xFFE65100),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // original text SMS
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = log.text,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // amount & note info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Khoản tiền phát hiện",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${if (log.type == "INCOME") "+" else "-"}${FormatHelper.formatVND(log.amount)}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (log.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Nội dung tóm tắt",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = log.note.ifEmpty { "Giao dịch" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Interactive Shroud Overlay Layer: "Phủ một lớp mờ nhẹ"
                            if (isExpanded) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .clickable {
                                            // Clicking outside the selection panel returns normal
                                            expandedPendingTimestamp = null
                                        }
                                )

                                // Foreground focused controls panel
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.Center)
                                        .padding(12.dp)
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                        .clickable(
                                            enabled = true,
                                            onClick = {} // Intercepts clicks inside selection panel
                                        )
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Phê duyệt giao dịch: ${if (log.type == "INCOME") "+" else "-"}${FormatHelper.formatVND(log.amount)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { expandedPendingTimestamp = null },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Đóng",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    // Wallet selection chip scroll
                                    Text(
                                        text = "Chọn ví tài chính áp dụng:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (wallets.isEmpty()) {
                                        Text(
                                            text = "Hãy tạo ít nhất một ví tài khoản trước khi duyệt!",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            wallets.forEach { w ->
                                                val selected = selectedWalletId == w.id
                                                FilterChip(
                                                    selected = selected,
                                                    onClick = { selectedWalletId = w.id },
                                                    label = { Text(w.name, fontSize = 11.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Category selection chip scroll
                                    Text(
                                        text = "Chọn danh mục:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        categories.filter { it.type == log.type || it.type == "BOTH" }.forEach { cat ->
                                            val selected = selectedCategoryName == cat.name
                                            FilterChip(
                                                selected = selected,
                                                onClick = { selectedCategoryName = cat.name },
                                                label = { Text(cat.name, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.secondary
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Three Action buttons: Xác nhận, Xóa, Hủy
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Confirm
                                        Button(
                                            onClick = {
                                                if (selectedWalletId == 0) {
                                                    android.widget.Toast.makeText(context, "Vui lòng chọn ví tài chính!", android.widget.Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (selectedCategoryName.isEmpty()) {
                                                    android.widget.Toast.makeText(context, "Vui lòng chọn danh mục!", android.widget.Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                viewModel.confirmPendingNotificationLog(log, selectedWalletId, selectedCategoryName)
                                                android.widget.Toast.makeText(context, "Đã ghi nhận giao dịch thành công!", android.widget.Toast.LENGTH_SHORT).show()
                                                expandedPendingTimestamp = null
                                            },
                                            modifier = Modifier.weight(1.2f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF2E7D32)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "Confirm", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Xác nhận", fontSize = 12.sp)
                                        }

                                        // Delete / Reject
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.deleteNotificationLog(log)
                                                android.widget.Toast.makeText(context, "Đã từ chối giao dịch quét!", android.widget.Toast.LENGTH_SHORT).show()
                                                expandedPendingTimestamp = null
                                            },
                                            modifier = Modifier.weight(0.9f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFFC62828)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFFC62828)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Xóa", fontSize = 12.sp)
                                        }

                                        // Close detail / Hủy
                                        TextButton(
                                            onClick = { expandedPendingTimestamp = null },
                                            modifier = Modifier.weight(0.8f)
                                        ) {
                                            Text("Hủy", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- SECTION 3: NHẬT KÝ THÔNG BÁO ĐÃ DUYỆT (Collapsible Simulation) ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showHistoryPanel = !showHistoryPanel }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History Logs",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LỊCH SỬ TIN NHẮN ĐÃ XỬ LÝ (3 NGÀY KO GIỚI HẠN)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ) {
                            Text(
                                text = processedLogs.size.toString(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = if (showHistoryPanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle History",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showHistoryPanel) {
                // Filters & Search display inside Collapsible History Block
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Tìm trong nhật ký đã xử lý...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 50.dp)
                                .testTag("bank_logs_search_input"),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val statusFilters = listOf(
                                Pair("ALL", "Tất cả (${processedLogs.size})"),
                                Pair("AUTO_ADDED", "Đã duyệt"),
                                Pair("FAILED_PARSE", "Lỗi cú pháp"),
                                Pair("NO_WALLET", "Trống ví")
                            )

                            statusFilters.forEach { (statusCode, statusLabel) ->
                                val count = if (statusCode == "ALL") processedLogs.size
                                else processedLogs.count { it.status == statusCode }

                                FilterChip(
                                    selected = selectedFilterStatus == statusCode,
                                    onClick = { selectedFilterStatus = statusCode },
                                    label = { Text("$statusLabel ($count)", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }

                if (processedLogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Inbox,
                                    contentDescription = "None",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty() || selectedFilterStatus != "ALL")
                                        "Không có kết quả lọc phù hợp"
                                    else "Không có lịch sử đã xử lý nào phong phú",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(processedLogs, key = { "processed_" + it.timestamp + "_" + it.text.hashCode() }) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(
                                                    if (log.type == "INCOME") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (log.type == "INCOME") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                contentDescription = log.type,
                                                tint = if (log.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "${log.bankName} (${log.walletName})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            val timeStr = remember(log.timestamp) {
                                                SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                                            }
                                            Text(
                                                text = timeStr,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        color = when (log.status) {
                                            "AUTO_ADDED" -> Color(0xFFE8F5E9)
                                            "FAILED_PARSE" -> MaterialTheme.colorScheme.errorContainer
                                            else -> Color(0xFFECEFF1)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = when (log.status) {
                                                "AUTO_ADDED" -> "ĐÃ TỰ GHI"
                                                "FAILED_PARSE" -> "SAI CÚ PHÁP"
                                                else -> "CHƯA XỬ LÝ"
                                            },
                                            color = when (log.status) {
                                                "AUTO_ADDED" -> Color(0xFF2E7D32)
                                                "FAILED_PARSE" -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = log.text,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }

                                if (log.amount > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${if (log.type == "INCOME") "+" else "-"}${FormatHelper.formatVND(log.amount)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (log.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )

                                        Text(
                                            text = log.note.ifEmpty { "Giao dịch" },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Xác nhận xóa lịch sử?") },
            text = { Text("Thao tác này sẽ xóa sạch nhật ký đọc tin nhắn ngân hàng hiện tại. Bạn có chắc không?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearNotificationLogs()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Đồng ý xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Hủy bỏ")
                }
            }
        )
    }

    // Bulk approval dialog
    if (showBulkApproveDialog) {
        val targetWalletId = bulkSelectedWalletId
        AlertDialog(
            onDismissRequest = { showBulkApproveDialog = false },
            title = { Text("Duyệt hàng loạt (${pendingLogs.size} giao dịch)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tất cả giao dịch chờ sẽ được tự động phân loại theo danh mục tối ưu nhất và nạp vào ví bạn chọn dưới đây:")
                    
                    Text("Chọn ví thụ hưởng:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        wallets.forEach { w ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { bulkSelectedWalletId = w.id }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = targetWalletId == w.id,
                                    onClick = { bulkSelectedWalletId = w.id }
                                )
                                Text(w.name, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalWalletId = bulkSelectedWalletId
                        if (finalWalletId != null && finalWalletId != 0) {
                            viewModel.confirmPendingNotificationLogsBulk(pendingLogs, finalWalletId)
                            android.widget.Toast.makeText(context, "Đã phê duyệt hàng loạt thành công!", android.widget.Toast.LENGTH_SHORT).show()
                            showBulkApproveDialog = false
                        } else {
                            android.widget.Toast.makeText(context, "Vui lòng chọn một ví tài khoản!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Xác nhận duyệt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkApproveDialog = false }) {
                    Text("Hủy bỏ")
                }
            }
        )
    }

    // Bulk delete dialog
    if (showBulkDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text("Xóa hàng loạt (${pendingLogs.size} giao dịch)") },
            text = { Text("Các giao dịch chờ duyệt này sẽ được dọn dẹp sạch khỏi danh sách phê duyệt. Vì lựa chọn dọn dẹp hoàn toàn, bạn hoàn toàn có thể quét lại những thông báo này trên thanh trạng thái sau này nếu cần thiết.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNotificationLogsBulk(pendingLogs, deleteCompletely = true)
                        android.widget.Toast.makeText(context, "Đã dọn dẹp hàng loạt giao dịch chờ!", android.widget.Toast.LENGTH_SHORT).show()
                        showBulkDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xác nhận xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = false }) {
                    Text("Hủy bỏ")
                }
            }
        )
    }
}
