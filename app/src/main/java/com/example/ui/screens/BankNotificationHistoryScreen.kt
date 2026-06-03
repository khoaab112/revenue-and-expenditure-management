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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.FinanceViewModel
import com.example.ui.FormatHelper
import com.example.ui.NotificationLog
import com.example.ui.IconMapper
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            
            // --- SECTION 1: CÀI ĐẶT THÔNG BÁO THỰC TẾ & MÔ PHỎNG ---
            item {
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
                                        text = "Đọc tin ngân hàng tự động",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tự ghi chép khi nổ thông báo app ngân hàng, MoMo...",
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
                                Row(
                                    modifier = Modifier.padding(12.dp),
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
                                            text = "Yêu cầu quyền hệ thống",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "Để đọc tự động, vui lòng bật quyền truy cập thông báo trong cài đặt Android.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
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
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Cấp quyền", fontSize = 11.sp)
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
                                        text = "TRẠNG THÁI: Đã kích hoạt & Đang lắng nghe thông báo biến động số dư!",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        // Manual Notification Scan Button
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        
                        Button(
                            onClick = {
                                if (isCheckingNotifications) return@Button
                                isCheckingNotifications = true
                                coroutineScope.launch {
                                    delay(1200) // Assure a minimum period of loading state indicator is visible to the user
                                    viewModel.scanNotificationsManual(
                                        context = context,
                                        onSuccess = { count ->
                                            isCheckingNotifications = false
                                            if (count > 0) {
                                                viewModel.showSuccessNotification("Quét thành công! Đã tự động thêm $count giao dịch mới.")
                                            } else {
                                                viewModel.showWarningNotification("Quét xong! Không tìm thấy thông báo giao dịch mới nào trên thanh trạng thái.")
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
                                Text("Đang quét tìm thông báo...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Scan", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Quét thông báo (Thủ công)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
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

                    items(pendingLogs, key = { it.timestamp }) { log ->
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
                            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                                // Left Color Strip
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(4.dp)
                                        .background(
                                            if (log.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                )

                                // Card Content
                                Column(modifier = Modifier.padding(10.dp).weight(1f)) {
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

                                            // WALLET CHIPS ROW WITH PINNED SELECTED WALLET FIRST (No more details about balance)
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
                                                    val sortedWallets = remember(wallets) {
                                                        wallets
                                                    }

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .horizontalScroll(rememberScrollState())
                                                            .padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        sortedWallets.forEach { w ->
                                                            val selected = selectedWalletId == w.id
                                                            val walletColor = try {
                                                                Color(android.graphics.Color.parseColor(w.colorHex))
                                                            } catch (e: Exception) {
                                                                MaterialTheme.colorScheme.primary
                                                            }

                                                            Card(
                                                                modifier = Modifier
                                                                    .width(110.dp)
                                                                    .height(38.dp)
                                                                    .clickable { selectedWalletId = w.id }
                                                                    .border(
                                                                        width = if (selected) 2.dp else 1.dp,
                                                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                                                        shape = RoundedCornerShape(8.dp)
                                                                    ),
                                                                colors = CardDefaults.cardColors(
                                                                    containerColor = walletColor
                                                                ),
                                                                shape = RoundedCornerShape(8.dp),
                                                                elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 0.dp)
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier
                                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                        .fillMaxSize(),
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Icon(
                                                                        imageVector = IconMapper.getIconByName(w.iconName),
                                                                        contentDescription = "Icon",
                                                                        tint = Color.White.copy(alpha = 0.9f),
                                                                        modifier = Modifier.size(13.dp)
                                                                    )
                                                                    Text(
                                                                        text = w.name,
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 11.sp,
                                                                        color = Color.White,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis,
                                                                        modifier = Modifier.weight(1f)
                                                                    )
                                                                    if (selected) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.CheckCircle,
                                                                            contentDescription = "Selected",
                                                                            tint = Color.White,
                                                                            modifier = Modifier.size(12.dp)
                                                                        )
                                                                    } else if (w.id == matchedWallet?.id) {
                                                                        Text(
                                                                            text = "⚡",
                                                                            fontSize = 10.sp,
                                                                            color = Color.White
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // CATEGORIES ROW (Sửa để gọn hơn)
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
                                                        text = "Phân loại Danh Mục:",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .horizontalScroll(rememberScrollState())
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    categories.filter { it.type == log.type || it.type == "BOTH" }.forEach { cat ->
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
                                                }
                                            }

                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                            // AMOUNT EDITING FIELD (Số tiền đổi thành VND và chuyển xuống cuối dòng kèm trợ giúp định dạng)
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = "VND:",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        OutlinedTextField(
                                                            value = customAmountStr,
                                                            onValueChange = { input ->
                                                                val filtered = input.filter { it.isDigit() }
                                                                if (filtered.length <= 12) {
                                                                    customAmountStr = filtered
                                                                }
                                                            },
                                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                                            ),
                                                            modifier = Modifier
                                                                .fillMaxWidth(),
                                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            singleLine = true,
                                                            shape = RoundedCornerShape(6.dp),
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                                            )
                                                        )

                                                        if (customAmountStr.isNotEmpty()) {
                                                            val parsedVal = customAmountStr.toDoubleOrNull() ?: 0.0
                                                            Text(
                                                                text = FormatHelper.formatVND(parsedVal),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                                            )
                                                        }
                                                    }
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
                                                        viewModel.confirmPendingNotificationLog(log, selectedWalletId, selectedCategoryName, finalAmount)
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
}
