package com.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.style.TextOverflow
import com.app.ui.FinanceViewModel
import com.app.ui.FormatHelper
import com.app.ui.NotificationLog
import com.app.ui.IconMapper
import com.app.data.Wallet
import com.app.data.FinanceCategory
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PendingGroup(
    val type: String, // "SINGLE" or "TRANSFER_PAIR"
    val log1: NotificationLog,
    val log2: NotificationLog? = null
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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

    // Group "Thông báo" state
    val notificationReaderEnabled by viewModel.notificationReaderEnabled.collectAsState()
    val isServiceEnabled = remember(context) { isNotificationServiceEnabled(context) }
    var isPermitted by remember { mutableStateOf(isServiceEnabled) }
    
    val postNotificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { /* No-op, just requesting is enough */ }
    )

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isPermitted = isNotificationServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showHistoryPanel by remember { mutableStateOf(true) }

    // Active expanded pending log timestamp tracker
    var expandedPendingTimestamp by remember { mutableStateOf<Long?>(null) }

    var showBulkApproveDialog by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var bulkSelectedWalletId by remember { mutableStateOf<Int?>(null) }

    var activeTab by remember { mutableStateOf(0) }
    
    var isCheckingNotifications by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Partition logs
    val pendingLogs = remember(notificationLogs) {
        notificationLogs.filter { it.status == "PENDING" }
    }

    val pendingGroups = remember(pendingLogs) {
        val groups = mutableListOf<PendingGroup>()
        val processedTimestamps = mutableSetOf<Long>()
        
        for (i in pendingLogs.indices) {
            val logA = pendingLogs[i]
            if (processedTimestamps.contains(logA.timestamp)) continue
            
            var foundPair = false
            for (j in i + 1 until pendingLogs.size) {
                val logB = pendingLogs[j]
                if (processedTimestamps.contains(logB.timestamp)) continue
                
                val timeDiff = Math.abs(logA.timestamp - logB.timestamp)
                val sameAmount = Math.abs(logA.amount - logB.amount) < 0.01
                val oppositeType = (logA.type == "EXPENSE" && logB.type == "INCOME") || 
                                   (logA.type == "INCOME" && logB.type == "EXPENSE")
                
                if (timeDiff <= 120_000 && sameAmount && oppositeType) {
                    val expenseLog = if (logA.type == "EXPENSE") logA else logB
                    val incomeLog = if (logA.type == "EXPENSE") logB else logA
                    
                    groups.add(PendingGroup(type = "TRANSFER_PAIR", log1 = expenseLog, log2 = incomeLog))
                    processedTimestamps.add(logA.timestamp)
                    processedTimestamps.add(logB.timestamp)
                    foundPair = true
                    break
                }
            }
            
            if (!foundPair) {
                groups.add(PendingGroup(type = "SINGLE", log1 = logA))
                processedTimestamps.add(logA.timestamp)
            }
        }
        groups
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

    var showSidebar by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tin nhắn ngân hàng",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Text(
                            text = "Duyệt giao dịch từ thông báo biến động số dư",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    IconButton(onClick = { showSidebar = !showSidebar }, modifier = Modifier.padding(end = 4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF1E2A40), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Cài đặt & Quyền",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Nút Quét Thủ Công ở trên cùng
            item {
                Button(
                    onClick = {
                        if (isCheckingNotifications) return@Button
                        isCheckingNotifications = true
                        coroutineScope.launch {
                            delay(1200)
                            viewModel.scanNotificationsManual(
                                context = context,
                                onSuccess = { count ->
                                    isCheckingNotifications = false
                                    if (count > 0) {
                                        viewModel.showSuccessNotification("Quét thành công! Đã thêm $count giao dịch.")
                                    } else {
                                        viewModel.showWarningNotification("Quét xong! Không tìm thấy thông báo mới.")
                                    }
                                },
                                onError = { errorMessage ->
                                    isCheckingNotifications = false
                                    viewModel.showErrorNotification(errorMessage)
                                }
                            )
                        }
                    },
                    enabled = !isCheckingNotifications,
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("manual_scan_notifications_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    if (isCheckingNotifications) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang quét...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Scan", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Quét thủ công", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- TAB ROW: CHỜ & DUYỆT ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            modifier = Modifier.testTag("tab_pending"),
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PendingActions,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (activeTab == 0) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "CHỜ DUYỆT",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (activeTab == 0) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (pendingLogs.isNotEmpty()) {
                                        Surface(
                                            color = Color(0xFFFFF3E0),
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = pendingLogs.size.toString(),
                                                color = Color(0xFFE65100),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            modifier = Modifier.testTag("tab_approved"),
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (activeTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "ĐÃ DUYỆT",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (activeTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (processedLogs.isNotEmpty()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = processedLogs.size.toString(),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (activeTab == 0) {
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
                                        viewModel.showWarningNotification("Vui lòng tạo ví tài khoản trước!")
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

                    items(pendingGroups, key = { it.log1.timestamp }) { group ->
                        if (group.type == "TRANSFER_PAIR") {
                            PendingTransferPairItem(
                                logExpense = group.log1,
                                logIncome = group.log2!!,
                                wallets = wallets,
                                viewModel = viewModel
                            )
                        } else {
                            PendingLogItem(
                                log = group.log1,
                                wallets = wallets,
                                categories = categories,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            if (activeTab == 1) {
                // Filters & Search display inside History Tab
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
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f))
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
                            viewModel.showSuccessNotification("Đã phê duyệt hàng loạt thành công!")
                            showBulkApproveDialog = false
                        } else {
                            viewModel.showWarningNotification("Vui lòng chọn một ví tài khoản!")
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
                        viewModel.showSuccessNotification("Đã dọn dẹp hàng loạt giao dịch chờ!")
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
    
    // Sidebar Overlay
    AnimatedVisibility(
        visible = showSidebar,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.align(Alignment.CenterEnd)
    ) {
        // CATCH CLICK EVENTS ON BACKGROUND
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { showSidebar = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .align(Alignment.CenterEnd)
                    .pointerInput(Unit) { }, // consume clicks
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header with close button
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cài đặt thông báo", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        IconButton(onClick = { showSidebar = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng")
                        }
                    }

                    // --- SECTION 1: CÀI ĐẶT THÔNG BÁO THỰC TẾ & MÔ PHỎNG ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header & Toggle Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = "Notification Reader",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Đọc tin tự động",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Tự động ghi chép khi nổ thông báo",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = notificationReaderEnabled,
                                    onCheckedChange = { isChecked ->
                                        viewModel.setNotificationReaderEnabled(isChecked)
                                        if (isChecked) {
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                    postNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("notification_reader_switch")
                                )
                            }

                            // Permission Warning Banner if not enabled/permitted
                            if (!isPermitted) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Permission Needed",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Yêu cầu quyền",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                                Text(
                                                    text = "Xin vui lòng bật quyền truy cập thông báo trong cài đặt Android.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    viewModel.showErrorNotification("Không thể mở cài đặt")
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(36.dp).fillMaxWidth()
                                        ) {
                                            Text("Cấp quyền nhanh", fontSize = 12.sp)
                                        }
                                    }
                                }
                            } else if (notificationReaderEnabled) {
                                // Success Banner show status
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Ready Status",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Đã kích hoạt & Đang lắng nghe thông báo biến động số dư!",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
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
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PendingLogItem(
    log: NotificationLog,
    wallets: List<Wallet>,
    categories: List<FinanceCategory>,
    viewModel: FinanceViewModel
) {
    val isExpanded = true

    // Smart recommendation logic
    val recommendation = remember(log, wallets) {
        viewModel.getSmartWalletRecommendation(log, wallets)
    }
    val matchedWallet = recommendation.first
    val confidenceScore = recommendation.second

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

    var customAmountStr by remember(log) {
        mutableStateOf(String.format("%.0f", log.amount))
    }

    var customNoteStr by remember(log) {
        mutableStateOf(log.note)
    }

    val events by viewModel.allEvents.collectAsState()
    
    val activeEvents = remember(log.timestamp, events) {
        val now = log.timestamp
        events.filter {
            now >= it.startDate && (it.endDate == null || now <= it.endDate + 86400000L - 1)
        }.sortedWith(compareBy<com.app.data.Event> {
            if (it.endDate != null) 0 else 1
        }.thenBy {
            if (it.endDate != null) (it.endDate - it.startDate) else Long.MAX_VALUE
        }.thenBy {
            it.endDate ?: Long.MAX_VALUE
        }.thenByDescending {
            it.startDate
        })
    }
    
    var selectedEventId by remember(log, activeEvents) {
        mutableStateOf(activeEvents.firstOrNull()?.id)
    }

    var eventDropdownExpanded by remember(log) { mutableStateOf(false) }

    var walletDropdownExpanded by remember(log) { mutableStateOf(false) }
    var isCategoriesExpanded by remember(log) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Left Color Strip
            Box(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            if (log.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        .align(Alignment.CenterStart)
                )
            }

            // Card Content
            Column(modifier = Modifier.padding(start = 14.dp, top = 10.dp, end = 10.dp, bottom = 10.dp).fillMaxWidth()) {
                // Top Row: Bank Info and Pending Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
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
                                text = log.bankName.ifEmpty { "Ngân hàng" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
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
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(Color(0xFFE65100), CircleShape)
                            )
                            Text(
                                text = "CHỜ DUYỆT",
                                color = Color(0xFFE65100),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Original Message Text with elegant background
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = log.text,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Dynamic amount & Note Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Số tiền phát hiện",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${if (log.type == "INCOME") "+" else "-"}${FormatHelper.formatVND(log.amount)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
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
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(6.dp))

                // Smart suggestion panel trigger row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gợi ý",
                            tint = Color(0xFFF9A825),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Đề xuất ví: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = matchedWallet?.name ?: "Chưa rõ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (confidenceScore > 0) {
                        Surface(
                            color = if (confidenceScore >= 3) Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (confidenceScore >= 3) "🛡️ Tin cậy cao" else "⚡ Đã học",
                                color = if (confidenceScore >= 3) Color(0xFF2E7D32) else Color(0xFF1565C0),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Interactive embedded selection board
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Đề xuất thông tin",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Sửa & kiểm tra lại để lưu",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // WALLET DROPDOWN BOX
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Loại ví:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (wallets.isEmpty()) {
                                Text(
                                    text = "Hãy tạo ít nhất một ví tài khoản trước khi duyệt!",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                ExposedDropdownMenuBox(
                                    expanded = walletDropdownExpanded,
                                    onExpandedChange = { walletDropdownExpanded = !walletDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    val selectedWallet = wallets.find { it.id == selectedWalletId }
                                    val walletColor = try {
                                        Color(android.graphics.Color.parseColor(selectedWallet?.colorHex ?: "#1976D2"))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .menuAnchor(),
                                        colors = CardDefaults.cardColors(containerColor = walletColor),
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                                .fillMaxSize(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (selectedWallet != null) {
                                                    Icon(
                                                        imageVector = IconMapper.getIconByName(selectedWallet.iconName),
                                                        contentDescription = "Icon",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Text(
                                                    text = selectedWallet?.name ?: "Chọn ví...",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Mở rộng",
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    ExposedDropdownMenu(
                                        expanded = walletDropdownExpanded,
                                        onDismissRequest = { walletDropdownExpanded = false }
                                    ) {
                                        wallets.forEach { w ->
                                            val wColor = try {
                                                Color(android.graphics.Color.parseColor(w.colorHex))
                                            } catch (e: Exception) {
                                                MaterialTheme.colorScheme.primary
                                            }
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .background(wColor, CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = IconMapper.getIconByName(w.iconName),
                                                                contentDescription = "Icon",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = w.name,
                                                            fontWeight = if (w.id == selectedWalletId) FontWeight.Bold else FontWeight.Medium,
                                                            fontSize = 13.sp
                                                        )
                                                        if (w.id == matchedWallet?.id) {
                                                            Text(text = "⚡", fontSize = 10.sp)
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    selectedWalletId = w.id
                                                    walletDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // EVENT DROPDOWN BOX
                        if (activeEvents.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Sự kiện:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            ExposedDropdownMenuBox(
                                expanded = eventDropdownExpanded,
                                onExpandedChange = { eventDropdownExpanded = !eventDropdownExpanded },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                val selectedEvent = events.find { it.id == selectedEventId }
                                val eventColor = try {
                                    Color(android.graphics.Color.parseColor(selectedEvent?.colorHex ?: "#757575"))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.secondary
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .menuAnchor(),
                                    colors = CardDefaults.cardColors(containerColor = eventColor),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .fillMaxSize(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Event,
                                                contentDescription = "Event Icon",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = selectedEvent?.name ?: "Không gắn sự kiện",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Mở rộng",
                                            tint = Color.White
                                        )
                                    }
                                }

                                ExposedDropdownMenu(
                                    expanded = eventDropdownExpanded,
                                    onDismissRequest = { eventDropdownExpanded = false }
                                ) {
                                    // Option: No Event
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(Color(0xFF757575), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Block,
                                                        contentDescription = "No Event",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "Không gắn sự kiện",
                                                    fontWeight = if (selectedEventId == null) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedEventId = null
                                            eventDropdownExpanded = false
                                        }
                                    )

                                    // Option: All Active Events
                                    activeEvents.forEachIndexed { idx, ev ->
                                        val evColor = try {
                                            Color(android.graphics.Color.parseColor(ev.colorHex))
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.secondary
                                        }
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .background(evColor, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Event,
                                                            contentDescription = "Event",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                    Column {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = ev.name,
                                                                fontWeight = if (ev.id == selectedEventId) FontWeight.Bold else FontWeight.Medium,
                                                                fontSize = 13.sp
                                                            )
                                                            if (idx == 0) {
                                                                Surface(
                                                                    color = Color(0xFFFFF3E0),
                                                                    shape = RoundedCornerShape(4.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "Gợi ý 🎯",
                                                                        color = Color(0xFFE65100),
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        if (ev.endDate != null) {
                                                            val formatter = SimpleDateFormat("dd/MM", Locale.getDefault())
                                                            Text(
                                                                text = "Hạn: ${formatter.format(Date(ev.startDate))} - ${formatter.format(Date(ev.endDate))}",
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        } else {
                                                            Text(
                                                                text = "Hạn: Vô thời hạn",
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedEventId = ev.id
                                                eventDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                        // CATEGORIES ROW
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Danh mục:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val filteredCategories = categories.filter { it.type == log.type || it.type == "BOTH" }
                            val showExpandButton = !isCategoriesExpanded && filteredCategories.size > 11
                            val displayCategories = if (isCategoriesExpanded) filteredCategories else filteredCategories.take(11)

                            androidx.compose.foundation.layout.FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 0.dp),
                                maxItemsInEachRow = 4,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                displayCategories.forEach { cat ->
                                    val selected = selectedCategoryName == cat.name
                                    val catColor = try {
                                        Color(android.graphics.Color.parseColor(cat.colorHex))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }

                                    Card(
                                        modifier = Modifier
                                            .height(32.dp)
                                            .clickable { selectedCategoryName = cat.name }
                                            .border(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(16.dp)
                                            ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                                .fillMaxHeight(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .background(catColor.copy(alpha = 0.15f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = IconMapper.getIconByName(cat.iconName),
                                                    contentDescription = null,
                                                    tint = catColor,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                            Text(
                                                text = cat.name,
                                                fontSize = 11.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                                
                                if (showExpandButton) {
                                    Card(
                                        modifier = Modifier
                                            .height(32.dp)
                                            .clickable { isCategoriesExpanded = true },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                                .fillMaxHeight(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("Xem thêm", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onTertiary)
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onTertiary)
                                        }
                                    }
                                }
                            }
                        }

                        // AMOUNT & NOTE FIELDS
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 0.dp, bottom = 4.dp)) {
                                val parsedVal = customAmountStr.toDoubleOrNull() ?: 0.0
                                if (parsedVal > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        Text(
                                            text = FormatHelper.formatVND(parsedVal),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Số tiền:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.width(60.dp)
                                    )
                                    BasicTextField(
                                        value = customAmountStr,
                                        onValueChange = { input ->
                                            val filtered = input.filter { it.isDigit() }
                                            if (filtered.length <= 12) {
                                                customAmountStr = filtered
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        decorationBox = { innerTextField ->
                                            Box(
                                                Modifier
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                            ) {
                                                if (customAmountStr.isEmpty()) {
                                                    Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f), fontSize = 12.sp)
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ghi chú:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(60.dp)
                                )
                                BasicTextField(
                                    value = customNoteStr,
                                    onValueChange = { customNoteStr = it },
                                    textStyle = TextStyle(
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            Modifier
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                        ) {
                                            if (customNoteStr.isEmpty()) {
                                                Text("Nhập ghi chú...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f), fontSize = 12.sp)
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // ACTION BUTTONS (Xác nhận, Bỏ qua)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (selectedWalletId == 0) {
                                        viewModel.showWarningNotification("Vui lòng chọn ví tài chính!")
                                        return@Button
                                    }
                                    if (selectedCategoryName.isEmpty()) {
                                        viewModel.showWarningNotification("Vui lòng chọn danh mục!")
                                        return@Button
                                    }
                                    val finalAmount = customAmountStr.toDoubleOrNull() ?: log.amount
                                    viewModel.confirmPendingNotificationLog(
                                        log = log,
                                        walletId = selectedWalletId,
                                        categoryName = selectedCategoryName,
                                        overrideAmount = finalAmount,
                                        overrideNote = customNoteStr,
                                        overrideEventId = selectedEventId
                                    )
                                    viewModel.showSuccessNotification("Xác nhận giao dịch thành công!")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Confirm", modifier = Modifier.size(16.dp))
                                    Text("Duyệt", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.deleteNotificationLog(log)
                                    viewModel.showSuccessNotification("Đã bỏ qua giao dịch quét!")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFC62828)
                                ),
                                border = BorderStroke(1.2.dp, Color(0xFFC62828)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                    Text("Bỏ qua", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PendingTransferPairItem(
    logExpense: NotificationLog,
    logIncome: NotificationLog,
    wallets: List<Wallet>,
    viewModel: FinanceViewModel
) {
    val sourceRec = remember(logExpense, wallets) {
        viewModel.getSmartWalletRecommendation(logExpense, wallets)
    }
    val destRec = remember(logIncome, wallets) {
        viewModel.getSmartWalletRecommendation(logIncome, wallets)
    }
    
    var selectedSourceWalletId by remember(logExpense, sourceRec.first) {
        mutableStateOf(sourceRec.first?.id ?: wallets.firstOrNull()?.id ?: 0)
    }
    var selectedDestWalletId by remember(logIncome, destRec.first) {
        mutableStateOf(destRec.first?.id ?: wallets.firstOrNull { it.id != selectedSourceWalletId }?.id ?: wallets.firstOrNull()?.id ?: 0)
    }
    
    var customAmountStr by remember(logExpense) {
        mutableStateOf(String.format("%.0f", logExpense.amount))
    }
    var customNoteStr by remember(logExpense) {
        mutableStateOf(logExpense.note)
    }
    
    var sourceWalletDropdownExpanded by remember { mutableStateOf(false) }
    var destWalletDropdownExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = Color(0xFF2196F3).copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF2196F3))
                        .align(Alignment.CenterStart)
                )
            }

            Column(modifier = Modifier.padding(start = 14.dp, top = 10.dp, end = 10.dp, bottom = 10.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFFE3F2FD), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = "TRANSFER",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Chuyển khoản nội bộ (Phát hiện)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val timeStr = remember(logExpense.timestamp) {
                                SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(logExpense.timestamp))
                            }
                            Text(
                                text = timeStr,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(Color(0xFF1976D2), CircleShape)
                            ) { }
                            Text(
                                text = "NỘI BỘ",
                                color = Color(0xFF1976D2),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = Color(0xFFFFF3E0).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, Color(0xFFFFB74D).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Rút từ ${logExpense.bankName}: ${logExpense.text}",
                            fontSize = 11.sp,
                            color = Color(0xFFC62828),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFFE8F5E9).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, Color(0xFF81C784).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Nạp vào ${logIncome.bankName}: ${logIncome.text}",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(text = "Ví & Tài khoản liên quan:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        val sourceWallet = wallets.find { it.id == selectedSourceWalletId }
                        OutlinedButton(
                            onClick = { sourceWalletDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Rút từ: ${sourceWallet?.name ?: "Chọn ví"}", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = sourceWalletDropdownExpanded,
                            onDismissRequest = { sourceWalletDropdownExpanded = false }
                        ) {
                            wallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w.name, fontSize = 12.sp) },
                                    onClick = {
                                        selectedSourceWalletId = w.id
                                        sourceWalletDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(16.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        val destWallet = wallets.find { it.id == selectedDestWalletId }
                        OutlinedButton(
                            onClick = { destWalletDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Nạp vào: ${destWallet?.name ?: "Chọn ví"}", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = destWalletDropdownExpanded,
                            onDismissRequest = { destWalletDropdownExpanded = false }
                        ) {
                            wallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w.name, fontSize = 12.sp) },
                                    onClick = {
                                        selectedDestWalletId = w.id
                                        destWalletDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Số tiền:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                                BasicTextField(
                                    value = customAmountStr,
                                    onValueChange = { customAmountStr = it },
                                    textStyle = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    decorationBox = { innerTextField ->
                                        if (customAmountStr.isEmpty()) {
                                            Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp)
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("Ghi chú:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                                BasicTextField(
                                    value = customNoteStr,
                                    onValueChange = { customNoteStr = it },
                                    textStyle = TextStyle(
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1,
                                    decorationBox = { innerTextField ->
                                        if (customNoteStr.isEmpty()) {
                                            Text("Nhập ghi chú...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp)
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (selectedSourceWalletId == 0 || selectedDestWalletId == 0) {
                                viewModel.showWarningNotification("Vui lòng chọn đầy đủ ví nguồn và ví đích!")
                                return@Button
                            }
                            if (selectedSourceWalletId == selectedDestWalletId) {
                                viewModel.showWarningNotification("Ví nguồn và ví đích trùng nhau!")
                                return@Button
                            }
                            val finalAmount = customAmountStr.toDoubleOrNull() ?: logExpense.amount
                            viewModel.confirmPendingInternalTransfer(
                                logExpense = logExpense,
                                logIncome = logIncome,
                                sourceWalletId = selectedSourceWalletId,
                                destWalletId = selectedDestWalletId,
                                amount = finalAmount,
                                note = customNoteStr
                            )
                            viewModel.showSuccessNotification("Duyệt chuyển khoản nội bộ thành công!")
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Confirm", modifier = Modifier.size(16.dp))
                            Text("Duyệt Chuyển Khoản", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.deleteNotificationLogsBulk(listOf(logExpense, logIncome), deleteCompletely = true)
                            viewModel.showSuccessNotification("Đã bỏ qua cặp giao dịch!")
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                        border = BorderStroke(1.2.dp, Color(0xFFC62828)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                            Text("Bỏ qua cả 2", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                    }
                }
            }
        }
    }
}
