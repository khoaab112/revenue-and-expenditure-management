package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FinanceCategory
import com.example.data.Transaction
import com.example.data.Wallet
import com.example.ui.FinanceViewModel
import com.example.ui.FormatHelper
import com.example.ui.IconMapper
import com.example.ui.components.CustomMoneyInputField
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun isNotificationServiceEnabled(context: android.content.Context): Boolean {
    val pkgName = context.packageName
    val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (!flat.isNullOrEmpty()) {
        val names = flat.split(":")
        for (name in names) {
            val cn = android.content.ComponentName.unflattenFromString(name)
            if (cn != null && cn.packageName == pkgName) {
                return true
            }
        }
    }
    return false
}

@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    onNavigateToBankNotificationHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPinEnabled by viewModel.isPinEnabled.collectAsState()
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showWalletManagement by remember { mutableStateOf(false) }
    var showCategoryManagement by remember { mutableStateOf(false) }
    var showSavingsManagement by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    val localBackupLastTime by viewModel.localBackupLastTime.collectAsState()
    val localBackupCount by viewModel.localBackupCount.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val syncProgressLogs by viewModel.syncProgressLogs.collectAsState()

    val notificationReaderEnabled by viewModel.notificationReaderEnabled.collectAsState()
    val notificationLogs by viewModel.notificationLogs.collectAsState()
    val wallets by viewModel.allWallets.collectAsState()
    val categories by viewModel.categoriesList.collectAsState()
    var expandedPendingTimestamp by remember { mutableStateOf<Long?>(null) }
    val pendingLogs = remember(notificationLogs) {
        notificationLogs.filter { it.status == "PENDING" }
    }
    var showBulkApproveDialog by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var bulkSelectedWalletId by remember { mutableStateOf<Int?>(null) }

    val isServiceEnabled = remember(context) { isNotificationServiceEnabled(context) }
    var isPermitted by remember { mutableStateOf(isServiceEnabled) }
    var showSimulationDetails by remember { mutableStateOf(false) }

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

    val backupFilePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            viewModel.importLocalBackup(context, it)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Hệ Thống Cài Đặt",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // 1. BẢO MẬT & RIÊNG TƯ
        Text(
            text = "BẢO MẬT & RIÊNG TƯ",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "PIN Lock",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Bảo mật bằng mã PIN",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Yêu cầu mã 4-số khi mở ứng dụng",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isPinEnabled,
                        onCheckedChange = { check ->
                            if (check) {
                                showPinSetupDialog = true
                            } else {
                                viewModel.disablePin()
                            }
                        },
                        modifier = Modifier.testTag("pin_protection_switch")
                    )
                }
            }
        }

        // 2. QUẢN LÝ CHUYÊN SÂU
        Text(
            text = "QUẢN LÝ CHUYÊN SÂU",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Wallet Management
                ListItem(
                    headlineContent = { Text("Quản lý ví", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Thêm, sửa đổi hoặc xóa tài khoản & ví giao dịch") },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = "Wallets", tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .clickable { showWalletManagement = true }
                        .testTag("manage_wallets_item")
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Category Management
                ListItem(
                    headlineContent = { Text("Quản lý hạng mục chi tiêu", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Quản lý dải danh mục thu chi của bạn") },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.Category, contentDescription = "Categories", tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .clickable { showCategoryManagement = true }
                        .testTag("manage_categories_item")
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Savings Management
                ListItem(
                    headlineContent = { Text("Quản lý bộ tiết kiệm", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Tách riêng các giao dịch và ví tích lũy của hũ độc lập") },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.Savings, contentDescription = "Savings", tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .clickable { showSavingsManagement = true }
                        .testTag("manage_savings_item")
                )
            }
        }

        // 3. TỰ ĐỘNG GHI QUA THÔNG BÁO
        Text(
            text = "TỰ ĐỘNG GHI QUA THÔNG BÁO",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

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
                        onCheckedChange = { viewModel.setNotificationReaderEnabled(it) },
                        modifier = Modifier.testTag("notification_reader_switch")
                    )
                }

                // Permission Warning Banner if not enabled
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
                                        android.widget.Toast.makeText(context, "Không thể mở cài đặt", android.widget.Toast.LENGTH_SHORT).show()
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

                // Simulator Section
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSimulationDetails = !showSimulationDetails }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Simulate icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "MÔ PHỎNG THÔNG BÁO THỬ NGHIỆM",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = if (showSimulationDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Simulation",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (showSimulationDetails) {
                    var testMessageText by remember { mutableStateOf("Vietcombank SD TK 0451 thay doi +500,000 VND luc 12:30. GD: Chuyen khoan luong thang 5") }
                    var selectedBankPreset by remember { mutableStateOf("Vietcombank") }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            Pair("Vietcombank", "Vietcombank SD TK 0451 thay doi +500,000 VND luc 12:30. GD: Chuyen khoan luong thang 5"),
                            Pair("Techcombank", "Techcombank: TK 1903 bien dong -150,000 VND. Noi dung: Mua tra sua HighTea"),
                            Pair("MoMo", "Ban da thanh toan thanh cong so tien -50,000 d cho dich vu GrabFood qua vi MoMo"),
                            Pair("MB Bank", "MB_BANK: GD +200,000 VND luc 15:40. ND: Tien mung sinh nhat")
                        )
                        presets.forEach { (bankName, presetText) ->
                            FilterChip(
                                selected = selectedBankPreset == bankName,
                                onClick = {
                                    selectedBankPreset = bankName
                                    testMessageText = presetText
                                },
                                label = { Text(bankName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = testMessageText,
                        onValueChange = { testMessageText = it },
                        label = { Text("Nội dung tin nhắn giả định") },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Button(
                        onClick = {
                            val pkg = when(selectedBankPreset) {
                                "Vietcombank" -> "com.vietcombank.restyle"
                                "Techcombank" -> "vn.com.techcombank"
                                "MoMo" -> "com.mservice.momo"
                                else -> "com.mbmobile"
                            }
                            viewModel.simulateBankNotification(selectedBankPreset, testMessageText, pkg)
                            android.widget.Toast.makeText(context, "Mô phỏng thành công! Kiểm tra nhật ký bên dưới.", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(40.dp).testTag("simulate_notification_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.BugReport, contentDescription = "Simulate", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chạy thử mô phỏng", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Link button to go to full history log screen (Danh sách nhật ký)
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Lịch sử nhật ký đọc thông báo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onNavigateToBankNotificationHistory,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Mở danh sách", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Manual Notification Scan Button
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                
                Button(
                    onClick = {
                        viewModel.scanNotificationsManual(
                            context = context,
                            onSuccess = { count ->
                                if (count > 0) {
                                    android.widget.Toast.makeText(context, "Quét thành công! Đã tự động thêm $count giao dịch mới.", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Quét xong! Không tìm thấy thông báo giao dịch mới nào trên thanh trạng thái.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            onError = { errorMessage ->
                                android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("manual_scan_notifications_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Scan", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quét thông báo ngân hàng mới (Thủ công)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3B. GIAO DỊCH CHỜ PHÊ DUYỆT
        Text(
            text = "GIAO DỊCH QUÉT ĐƯỢC CHỜ PHÊ DUYỆT (${pendingLogs.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFE65100),
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (pendingLogs.isNotEmpty()) Color(0xFFE65100).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pendingLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = "No Pending Logs",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sạch sẽ! Không có giao dịch chờ duyệt nào",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
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

                    pendingLogs.forEach { log ->
                        val isExpanded = expandedPendingTimestamp == log.timestamp

                        val matchedWallet = remember(log, wallets) {
                            wallets.find { it.name.lowercase().contains(log.bankName.lowercase()) }
                                ?: wallets.find { it.name.lowercase().contains(log.walletName.lowercase()) }
                                ?: wallets.find { it.type == "BANK" }
                                ?: wallets.find { it.type == "CASH" }
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
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isExpanded) 1.5.dp else 1.dp,
                                color = if (isExpanded) Color(0xFFE65100).copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                // Underneath Layer: Normal item content
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Header
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
                                                    .size(30.dp)
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
                                                    fontSize = 12.sp
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
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "CHỜ DUYỆT",
                                                color = Color(0xFFE65100),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Raw SMS Text
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = log.text,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Amount & Notes Summary
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
                                                fontSize = 14.sp,
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
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                // Overlay with a semi-transparent shroud / lớp mờ nhẹ on top
                                if (isExpanded) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .clickable {
                                                // click outside to collapse
                                                expandedPendingTimestamp = null
                                            }
                                    )

                                    // Foreground card to select and click confirm/delete
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.Center)
                                            .padding(10.dp)
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                            .clickable(
                                                enabled = true,
                                                onClick = {} // intercepts clicks
                                            )
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Phê duyệt nhanh: ${if (log.type == "INCOME") "+" else "-"}${FormatHelper.formatVND(log.amount)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
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

                                        // Choose wallet
                                        Text(
                                            text = "Chọn ví tiền:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            wallets.forEach { w ->
                                                val selected = selectedWalletId == w.id
                                                FilterChip(
                                                    selected = selected,
                                                    onClick = { selectedWalletId = w.id },
                                                    label = { Text(w.name, fontSize = 10.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                            }
                                        }

                                        // Choose category
                                        Text(
                                            text = "Chọn danh mục:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            categories.filter { it.type == log.type || it.type == "BOTH" }.forEach { cat ->
                                                val selected = selectedCategoryName == cat.name
                                                FilterChip(
                                                    selected = selected,
                                                    onClick = { selectedCategoryName = cat.name },
                                                    label = { Text(cat.name, fontSize = 10.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                        selectedLabelColor = MaterialTheme.colorScheme.secondary
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // Row containing action buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (selectedWalletId == 0) {
                                                        android.widget.Toast.makeText(context, "Vui lòng chọn ví!", android.widget.Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    if (selectedCategoryName.isEmpty()) {
                                                        android.widget.Toast.makeText(context, "Vui lòng chọn danh mục!", android.widget.Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    viewModel.confirmPendingNotificationLog(log, selectedWalletId, selectedCategoryName)
                                                    android.widget.Toast.makeText(context, "Phê duyệt thành công!", android.widget.Toast.LENGTH_SHORT).show()
                                                    expandedPendingTimestamp = null
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Xác nhận", fontSize = 11.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.deleteNotificationLog(log)
                                                    android.widget.Toast.makeText(context, "Đã xóa tin!", android.widget.Toast.LENGTH_SHORT).show()
                                                    expandedPendingTimestamp = null
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Xóa", fontSize = 11.sp)
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

        // 4. SAO LƯU CỰC BỘ
        Text(
            text = "SAO LƯU CỰC BỘ (LOCAL BACKUP)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

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
                // UI Mode Alert Header
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Backup Mode Status",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AN TOÀN - BẢO MẬT 100%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dữ liệu được nén thành bản sao lưu cục bộ dạng JSON lưu ngay trên máy của bạn. Bạn có thể tự do gửi file sao lưu này qua Drive, Zalo, hoặc lưu trữ bất kỳ đâu tùy thích!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Backup Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Lần sao lưu cuối",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = localBackupLastTime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Tổng số bản sao lưu",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$localBackupCount file",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // The buttons requested by USER: "Mở thư mục", "Sao lưu" and "Khôi phục"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.openBackupFolder(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("open_folder_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Open Folder",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mở thư mục chứa file",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.exportLocalBackup(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("start_backup_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Backup Now",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sao lưu",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Button(
                    onClick = {
                        backupFilePickerLauncher.launch("application/json")
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("restore_backup_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Restore Backup",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Khôi phục dữ liệu từ bản sao .json",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        // 5. THÔNG TIN
        Text(
            text = "THÔNG TIN",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Application Information Row
                ListItem(
                    headlineContent = { Text("Phiên bản", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("v1.0.0 (Bản mẫu Offline)") },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
                    }
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Sandbox mock Seeder function
                ListItem(
                    headlineContent = { Text("Nạp mẫu giao dịch demo", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Thêm tự động các giao dịch ngẫu nhiên để thẩm định biểu đồ") },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.Dataset, contentDescription = "Database", tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .clickable {
                            // Seed multiple transactions for beautiful stats!
                            val now = System.currentTimeMillis()
                            val oneDay = 24L * 60L * 60L * 1000L
                            
                            viewModel.addTransaction(
                                walletId = 1, // Cash
                                type = "EXPENSE",
                                amount = 150000.0,
                                categoryName = "Ăn uống",
                                note = "Ăn tối lẩu cua",
                                timestamp = now - oneDay * 2
                            )
                            viewModel.addTransaction(
                                walletId = 2, // Bank
                                type = "EXPENSE",
                                amount = 450000.0,
                                categoryName = "Mua sắm",
                                note = "Mua giày thể thao",
                                timestamp = now - oneDay * 3
                            )
                            viewModel.addTransaction(
                                walletId = 1,
                                type = "EXPENSE",
                                amount = 50000.0,
                                categoryName = "Di chuyển",
                                note = "GrabBike đi làm",
                                timestamp = now - oneDay
                            )
                            viewModel.addTransaction(
                                walletId = 2,
                                type = "INCOME",
                                amount = 8000000.0,
                                categoryName = "Lương",
                                note = "Nhận lương dự án ngoài",
                                timestamp = now - oneDay * 4
                            )
                            viewModel.addTransaction(
                                walletId = 2,
                                type = "EXPENSE",
                                amount = 1500000.0,
                                categoryName = "Hóa đơn",
                                note = "Thanh toán hoá điện nước",
                                timestamp = now - oneDay
                            )
                            viewModel.addTransaction(
                                walletId = 1,
                                type = "EXPENSE",
                                amount = 120000.0,
                                categoryName = "Giải trí",
                                note = "Vé xem phim CGV",
                                timestamp = now
                            )
                        }
                        .testTag("seed_database_item")
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                ListItem(
                    headlineContent = { Text("Xóa toàn bộ dữ liệu", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                    supportingContent = { Text("Xoá sạch ví, giao dịch, ngân sách để nhập lại từ đầu") },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Clear database", tint = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier
                        .clickable {
                            showClearDataDialog = true
                        }
                        .testTag("clear_database_item")
                )
            }
        }
    }

    // Beautiful Interactive State Dialog for sync feedback
    if (syncStatus.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.clearSyncLogs() },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSyncLogs() }) {
                    Text("Đã hiểu")
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (syncStatus) {
                        "SYNCING" -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        "SUCCESS_CLOUD", "SUCCESS", "SUCCESS_CLEAR" -> Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF0F9D58))
                        "SUCCESS_CLIPBOARD" -> Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copied", tint = Color(0xFF4285F4))
                        "ERROR" -> Icon(imageVector = Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        text = when (syncStatus) {
                            "SYNCING" -> "ĐANG ĐỒNG BỘ..."
                            "SUCCESS_CLOUD", "SUCCESS" -> "ĐỒNG BỘ CLOUD THÀNH CÔNG"
                            "SUCCESS_CLEAR" -> "XÓA DỮ LIỆU THÀNH CÔNG"
                            "SUCCESS_CLIPBOARD" -> "ĐÃ COPY DỮ LIỆU ĐỂ DÁN (PASTE)"
                            else -> "ĐỒNG BỘ THẤT BẠI"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    syncProgressLogs.forEach { log ->
                        Text(
                            text = log,
                            fontSize = 11.sp,
                            color = if (log.contains("Lỗi") || log.contains("Thất bại")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        )
    }

    // Clear Data confirmation dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Bạn có chắc chắn muốn xóa tất cả?") },
            text = { Text("Thao tác này sẽ xóa sạch toàn bộ ví tài chính, toàn bộ các giao dịch, báo cáo ngân sách và các mục tiêu tiết kiệm đang có trên thiết bị của bạn. Dữ liệu không thể khôi phục lại trừ khi bạn đã sao lưu trước đó.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDataDialog = false
                        viewModel.clearAllData(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa hết dữ liệu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
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

    // Secure PIN Setup Dialog
    if (showPinSetupDialog) {
        PinSetupDialog(
            onDismiss = { showPinSetupDialog = false },
            onSavePin = { pin ->
                viewModel.enablePin(pin)
                showPinSetupDialog = false
            }
        )
    }

    // Wallet Management Dialog
    if (showWalletManagement) {
        WalletManagementDialog(
            viewModel = viewModel,
            onDismiss = { showWalletManagement = false }
        )
    }

    // Category Management Dialog
    if (showCategoryManagement) {
        CategoryManagementDialog(
            viewModel = viewModel,
            onDismiss = { showCategoryManagement = false }
        )
    }

    // Savings Management Dialog
    if (showSavingsManagement) {
        SavingsManagementDialog(
            viewModel = viewModel,
            onDismiss = { showSavingsManagement = false }
        )
    }
}

@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit
) {
    var enteredText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("THIẾT LẬP MÃ PIN MỚI") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Vui lòng mã hóa mã PIN mới của bạn (đúng 4 ký tự số):", fontSize = 13.sp)
                OutlinedTextField(
                    value = enteredText,
                    onValueChange = {
                        if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                            enteredText = it
                            errorMessage = ""
                        }
                    },
                    label = { Text("Mã PIN (4 chữ số)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().testTag("setup_pin_text_input")
                )

                if (errorMessage.isNotEmpty()) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (enteredText.length == 4) {
                        onSavePin(enteredText)
                    } else {
                        errorMessage = "Mã PIN phải chứa đúng 4 chữ số!"
                    }
                },
                modifier = Modifier.testTag("confirm_save_pin_btn")
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

// ==========================================
// 1. WALLET MANAGEMENT DIALOG
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletManagementDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val wallets by viewModel.allWallets.collectAsState()
    
    // New Wallet Form States
    var showAddForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var walletType by remember { mutableStateOf("CASH") } // CASH, BANK, WALLET, SAVINGS
    var initialBalanceStr by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#2196F3") }
    var selectedIcon by remember { mutableStateOf("AccountBalanceWallet") }
    var isCustomColorActive by remember { mutableStateOf(false) }
    var customColorHex by remember { mutableStateOf("#9C27B0") }
    
    val colorPalette = listOf("#F44336", "#2196F3", "#4CAF50", "#FFC107", "#9C27B0", "#009688", "#455A64")
    val iconPalette = listOf("AccountBalanceWallet", "AccountBalance", "Payments", "Savings")
    val typeDisplayName = mapOf("CASH" to "Tiền mặt", "BANK" to "Ngân hàng", "WALLET" to "Ví điện tử", "SAVINGS" to "Tích lũy")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .testTag("wallet_management_dialog"),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("QUẢN LÝ VÍ & TÀI KHOẢN", fontSize = 18.sp, fontWeight = FontWeight.Black)
                IconButton(onClick = { if (showAddForm) showAddForm = false else onDismiss() }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!showAddForm) {
                    Button(
                        onClick = { showAddForm = true },
                        modifier = Modifier.fillMaxWidth().testTag("add_wallet_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thêm ví mới")
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("THÊM VÍ MỚI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Tên ví/tài khoản") },
                                modifier = Modifier.fillMaxWidth().testTag("wallet_name_input")
                            )

                            // Type selection (Displayed in a 2x2 grid)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Loại tài khoản", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                val items = listOf("CASH", "BANK", "WALLET", "SAVINGS")
                                val chunked = items.chunked(2)
                                chunked.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowItems.forEach { t ->
                                            FilterChip(
                                                selected = walletType == t,
                                                onClick = { walletType = t },
                                                label = { Text(typeDisplayName[t] ?: t, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = initialBalanceStr,
                                onValueChange = { initialBalanceStr = it },
                                label = { Text("Số dư khởi tạo") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("wallet_balance_input")
                            )

                            // Color selection grid & custom color support
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Mã màu hiển thị", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Predefined colors
                                    colorPalette.forEach { colorStr ->
                                        val isSelected = !isCustomColorActive && selectedColor == colorStr
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(FormatHelper.parseColor(colorStr))
                                                .clickable { 
                                                    isCustomColorActive = false
                                                    selectedColor = colorStr
                                                }
                                                .border(
                                                    BorderStroke(
                                                        width = if (isSelected) 3.dp else 0.dp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                    
                                    // Custom color circle option
                                    val currentCustColor = try {
                                        FormatHelper.parseColor(customColorHex)
                                    } catch (e: Exception) {
                                        Color.Gray
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(currentCustColor)
                                            .clickable { 
                                                isCustomColorActive = true
                                                selectedColor = customColorHex
                                            }
                                            .border(
                                                BorderStroke(
                                                    width = if (isCustomColorActive) 3.dp else 0.dp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                ),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = "Custom color",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (isCustomColorActive) {
                                    com.example.ui.components.ColorSliderPicker(
                                        initialColorHex = selectedColor,
                                        onColorChanged = { newHex ->
                                            selectedColor = newHex
                                            customColorHex = newHex
                                        }
                                    )
                                }
                            }

                            // Icon selection
                            Column {
                                Text("Biểu tượng ví", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    iconPalette.forEach { iconName ->
                                        val isSelected = selectedIcon == iconName
                                        IconButton(
                                            onClick = { selectedIcon = iconName },
                                            modifier = Modifier
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                        ) {
                                            Icon(
                                                imageVector = IconMapper.getIconByName(iconName),
                                                contentDescription = iconName,
                                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                        }
                    }
                }

                Text("TÀI KHOẢN HIỆN TẠI (Giữ và kéo biểu tượng ☰ để thay đổi vị trí)", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                
                SortableWalletList(
                    wallets = wallets,
                    viewModel = viewModel,
                    typeDisplayName = typeDisplayName
                )
            }
        },
        confirmButton = {
            val localContext = androidx.compose.ui.platform.LocalContext.current
            if (showAddForm) {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val balance = initialBalanceStr.toDoubleOrNull() ?: 0.0
                            viewModel.addWallet(name, walletType, balance, selectedColor, selectedIcon)
                            name = ""
                            initialBalanceStr = ""
                            showAddForm = false
                        } else {
                            android.widget.Toast.makeText(localContext, "Vui lòng nhập tên ví!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_wallet_confirm"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Lưu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("close_wallet_management_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Đóng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    )
}

@Composable
fun SortableWalletList(
    wallets: List<Wallet>,
    viewModel: FinanceViewModel,
    typeDisplayName: Map<String, String>
) {
    val listState = remember(wallets) { mutableStateListOf<Wallet>().apply { addAll(wallets) } }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var driftY by remember { mutableStateOf(0f) }

    val onDragReleased = {
        viewModel.updateWalletsOrder(listState.toList())
        draggedIndex = null
        driftY = 0f
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listState.forEachIndexed { index, wt ->
            val isDragged = draggedIndex == index
            val verticalOffset = if (isDragged) driftY else 0f
            val zIndexValue = if (isDragged) 10f else 1f
            val scaleValue = if (isDragged) 1.04f else 1f

            val colorValue = try {
                FormatHelper.parseColor(wt.colorHex)
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            ListItem(
                headlineContent = { Text(wt.name, fontWeight = FontWeight.Bold) },
                supportingContent = {
                    Text(
                        text = "${typeDisplayName[wt.type] ?: wt.type} • ${FormatHelper.formatVND(wt.balance)}",
                        color = colorValue,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = IconMapper.getIconByName(wt.iconName),
                        contentDescription = wt.name,
                        tint = colorValue
                    )
                },
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (listState.size > 1) {
                            IconButton(onClick = { viewModel.deleteWallet(wt) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Kéo để sắp xếp",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(36.dp)
                                .padding(4.dp)
                                .pointerInput(index) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedIndex = index
                                            driftY = 0f
                                        },
                                        onDragEnd = { onDragReleased() },
                                        onDragCancel = { onDragReleased() },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            driftY += dragAmount.y
                                            val itemHeightPx = 64.dp.toPx()
                                            val targetIdx = draggedIndex
                                            if (targetIdx != null) {
                                                if (driftY > itemHeightPx * 0.8f && targetIdx < listState.lastIndex) {
                                                    val next = listState[targetIdx + 1]
                                                    listState[targetIdx + 1] = listState[targetIdx]
                                                    listState[targetIdx] = next
                                                    draggedIndex = targetIdx + 1
                                                    driftY -= itemHeightPx
                                                } else if (driftY < -itemHeightPx * 0.8f && targetIdx > 0) {
                                                    val prev = listState[targetIdx - 1]
                                                    listState[targetIdx - 1] = listState[targetIdx]
                                                    listState[targetIdx] = prev
                                                    draggedIndex = targetIdx - 1
                                                    driftY += itemHeightPx
                                                }
                                            }
                                        }
                                    )
                                }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(zIndexValue)
                    .graphicsLayer {
                        translationY = verticalOffset
                        scaleX = scaleValue
                        scaleY = scaleValue
                    }
                    .background(
                        if (isDragged) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f) 
                        else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp, 
                        if (isDragged) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.outlineVariant, 
                        RoundedCornerShape(12.dp)
                    )
            )
        }
    }
}

// ==========================================
// 2. CATEGORY MANAGEMENT DIALOG
// ==========================================
@Composable
fun CategoryManagementDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val categoriesList by viewModel.categoriesList.collectAsState()
    
    // New Category Form States
    var selectedTypeTab by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME
    var showAddForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var parentName by remember { mutableStateOf<String?>(null) }
    var type by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME, BOTH
    var selectedColor by remember { mutableStateOf("#4CAF50") }
    var selectedIcon by remember { mutableStateOf("ShoppingCart") }
    var isCustomColorActive by remember { mutableStateOf(false) }
    var customColorHex by remember { mutableStateOf("#9C27B0") }
    
    val colorPalette = listOf("#F44336", "#2196F3", "#4CAF50", "#FFC107", "#9C27B0", "#009688", "#E91E63", "#795548")
    val iconPalette = listOf(
        "Restaurant", "DirectionsCar", "ShoppingBag", "Receipt", 
        "SportsEsports", "School", "LocalHospital", "Home", 
        "Work", "CardGiftcard", "Storefront", "Payments", 
        "AccountBalance", "AccountBalanceWallet", "Savings", 
        "TrendingUp", "TrendingDown", "Lock", "Settings",
        // New icons
        "Coffee", "LocalBar", "Flight", "Checkroom", "FitnessCenter",
        "Pets", "ChildCare", "FaceRetouchingNatural", "Spa", "Movie",
        "Theaters", "LibraryMusic", "Headphones", "VideogameAsset",
        "LocalPizza", "LocalCafe", "LocalDining", "Brush", "Palette",
        "Computer", "PhoneIphone", "CameraAlt", "Map", "CrueltyFree",
        "PedalBike", "AutoAwesome", "Celebration", "Cake", "EmojiEmotions",
        "Favorite", "Mood", "SelfImprovement", "EmojiObjects", "RocketLaunch"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .testTag("category_management_dialog"),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("QUẢN LÝ DANH MỤC LƯỚI", fontSize = 18.sp, fontWeight = FontWeight.Black)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Expenses / Incomes tab
                TabRow(
                    selectedTabIndex = if (selectedTypeTab == "EXPENSE") 0 else 1,
                    containerColor = Color.Transparent,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTypeTab == "EXPENSE",
                        onClick = { selectedTypeTab = "EXPENSE"; type = "EXPENSE" },
                        text = { Text("Khoản Chi", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTypeTab == "INCOME",
                        onClick = { selectedTypeTab = "INCOME"; type = "INCOME" },
                        text = { Text("Khoản Thu", fontWeight = FontWeight.Bold) }
                    )
                }

                if (!showAddForm) {
                    Button(
                        onClick = { showAddForm = true },
                        modifier = Modifier.fillMaxWidth().testTag("add_category_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thêm danh mục mới")
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("TẠO DANH MỤC MỚI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Tên danh mục") },
                                modifier = Modifier.fillMaxWidth().testTag("category_name_input")
                            )

                            var categoryDropdownExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = parentName ?: "Không có (Danh mục gốc)",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Trực thuộc (Mục cha)") },
                                    trailingIcon = {
                                        IconButton(onClick = { categoryDropdownExpanded = !categoryDropdownExpanded }) {
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { categoryDropdownExpanded = true }
                                )

                                DropdownMenu(
                                    expanded = categoryDropdownExpanded,
                                    onDismissRequest = { categoryDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Không có (Danh mục gốc)") },
                                        onClick = {
                                            parentName = null
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                    val parentCandidates = categoriesList.filter { (it.type == type || it.type == "BOTH") && it.parentName == null }
                                    parentCandidates.forEach { p ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    val colorValue = try { FormatHelper.parseColor(p.colorHex) } catch(e:Exception){ Color.Gray }
                                                    Icon(IconMapper.getIconByName(p.iconName), contentDescription = p.name, tint = colorValue, modifier = Modifier.size(20.dp))
                                                    Text(p.name, fontSize = 14.sp)
                                                }
                                            },
                                            onClick = {
                                                parentName = p.name
                                                categoryDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Type select description
                            Text("Áp dụng cho: ${if (selectedTypeTab == "EXPENSE") "Hóa đơn & Chi tiêu" else "Thu nhập & Tiền vào"}", fontSize = 13.sp)

                            // Color grid with custom color support
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Mã màu đại diện", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Predefined colors
                                    colorPalette.forEach { colorStr ->
                                        val isSelected = !isCustomColorActive && selectedColor == colorStr
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(FormatHelper.parseColor(colorStr))
                                                .clickable { 
                                                    isCustomColorActive = false
                                                    selectedColor = colorStr
                                                }
                                                .border(
                                                    BorderStroke(
                                                        width = if (isSelected) 3.dp else 0.dp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    shape = CircleShape
                                                )
                                        )
                                    }

                                    // Custom color circle option
                                    val currentCustColor = try {
                                        FormatHelper.parseColor(customColorHex)
                                    } catch (e: Exception) {
                                        Color.Gray
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(currentCustColor)
                                            .clickable { 
                                                isCustomColorActive = true
                                                selectedColor = customColorHex
                                            }
                                            .border(
                                                BorderStroke(
                                                    width = if (isCustomColorActive) 3.dp else 0.dp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                ),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = "Custom color",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (isCustomColorActive) {
                                    com.example.ui.components.ColorSliderPicker(
                                        initialColorHex = selectedColor,
                                        onColorChanged = { newHex ->
                                            selectedColor = newHex
                                            customColorHex = newHex
                                        }
                                    )
                                }
                            }

                            // Icons grid
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Biểu tượng hiển thị (Lưới biểu tượng)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    iconPalette.chunked(6).forEach { rowIcons ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
                                        ) {
                                            rowIcons.forEach { iconName ->
                                                val isSelected = selectedIcon == iconName
                                                IconButton(
                                                    onClick = { selectedIcon = iconName },
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                            else MaterialTheme.colorScheme.surface,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                                    else MaterialTheme.colorScheme.outlineVariant,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                ) {
                                                    Icon(
                                                        imageVector = IconMapper.getIconByName(iconName),
                                                        contentDescription = iconName,
                                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { showAddForm = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Hủy")
                                }
                                Button(
                                    onClick = {
                                        if (name.isNotBlank()) {
                                            viewModel.addCategory(name, selectedIcon, selectedColor, type, parentName)
                                            name = ""
                                            parentName = null
                                            showAddForm = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("save_category_confirm")
                                ) {
                                    Text("Lưu")
                                }
                            }
                        }
                    }
                }

                Text("DANH SÁCH DANH MỤC (Giữ và kéo biểu tượng ☰ để thay đổi vị trí)", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                
                val currentFilterList = categoriesList.filter { it.type == selectedTypeTab || it.type == "BOTH" }
                
                if (currentFilterList.isEmpty()) {
                    Text("Không có danh mục nào. Hãy thêm danh mục mới!", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    SortableCategoryList(
                        categories = currentFilterList,
                        viewModel = viewModel,
                        typeTab = selectedTypeTab,
                        onAddSubcategory = { parentNameValue ->
                            parentName = parentNameValue
                            showAddForm = true
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("close_category_management_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Đóng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    )
}

@Composable
fun SortableCategoryList(
    categories: List<FinanceCategory>,
    viewModel: FinanceViewModel,
    typeTab: String,
    onAddSubcategory: (parentName: String) -> Unit
) {
    val roots = remember(categories) { categories.filter { it.parentName == null } }
    val listState = remember(roots) { mutableStateListOf<FinanceCategory>().apply { addAll(roots) } }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var driftY by remember { mutableStateOf(0f) }

    val onDragReleased = {
        viewModel.updateCategoriesOrder(listState.toList(), typeTab)
        draggedIndex = null
        driftY = 0f
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listState.forEachIndexed { index, cat ->
            val isDragged = draggedIndex == index
            val verticalOffset = if (isDragged) driftY else 0f
            val zIndexValue = if (isDragged) 10f else 1f
            val scaleValue = if (isDragged) 1.04f else 1f

            val colorValue = try {
                FormatHelper.parseColor(cat.colorHex)
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            var isExpanded by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(zIndexValue)
                    .graphicsLayer {
                        translationY = verticalOffset
                        scaleX = scaleValue
                        scaleY = scaleValue
                    }
                    .background(
                        if (isDragged) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f) 
                        else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp, 
                        if (isDragged) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.outlineVariant, 
                        RoundedCornerShape(12.dp)
                    )
            ) {
                ListItem(
                    modifier = Modifier.clickable { isExpanded = !isExpanded },
                    headlineContent = { Text(cat.name, fontWeight = FontWeight.Bold) },
                    leadingContent = {
                        Icon(
                            imageVector = IconMapper.getIconByName(cat.iconName),
                            contentDescription = cat.name,
                            tint = colorValue
                        )
                    },
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (listState.size > 1) {
                                IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Kéo để sắp xếp",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(4.dp)
                                    .pointerInput(index) {
                                        detectDragGestures(
                                            onDragStart = {
                                                draggedIndex = index
                                                driftY = 0f
                                            },
                                            onDragEnd = { onDragReleased() },
                                            onDragCancel = { onDragReleased() },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                driftY += dragAmount.y
                                                val itemHeightPx = 56.dp.toPx()
                                                val targetIdx = draggedIndex
                                                if (targetIdx != null) {
                                                    if (driftY > itemHeightPx * 0.8f && targetIdx < listState.lastIndex) {
                                                        val next = listState[targetIdx + 1]
                                                        listState[targetIdx + 1] = listState[targetIdx]
                                                        listState[targetIdx] = next
                                                        draggedIndex = targetIdx + 1
                                                        driftY -= itemHeightPx
                                                    } else if (driftY < -itemHeightPx * 0.8f && targetIdx > 0) {
                                                        val prev = listState[targetIdx - 1]
                                                        listState[targetIdx - 1] = listState[targetIdx]
                                                        listState[targetIdx] = prev
                                                        draggedIndex = targetIdx - 1
                                                        driftY += itemHeightPx
                                                    }
                                                }
                                            }
                                        )
                                    }
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                // Subcategories
                if (isExpanded) {
                    val subcats = categories.filter { it.parentName == cat.name }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp, bottom = 12.dp, start = 56.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subcats.forEach { subCat ->
                            val subColor = try { FormatHelper.parseColor(subCat.colorHex) } catch(e: Exception) { Color.Gray }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = IconMapper.getIconByName(subCat.iconName),
                                        contentDescription = subCat.name,
                                        tint = subColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = subCat.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteCategory(subCat) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete subcategory",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Dotted add subcategory button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAddSubcategory(cat.name) }
                                .drawBehind {
                                    drawRoundRect(
                                        color = colorValue.copy(alpha = 0.6f),
                                        style = Stroke(
                                            width = 1.5.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                        ),
                                        cornerRadius = CornerRadius(8.dp.toPx())
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add subcategory",
                                    tint = colorValue.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Thêm mục con",
                                    color = colorValue.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. SAVINGS MANAGEMENT DIALOG
// ==========================================
@Composable
fun SavingsManagementDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
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
    
    val totalSavings = remember(savingsWallets) { savingsWallets.sumOf { it.balance } }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.95f)
            .testTag("savings_management_dialog"),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("QUẢN LỸ SỔ & HŨ TIẾT KIỆM", fontSize = 18.sp, fontWeight = FontWeight.Black)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                            OutlinedTextField(
                                value = newSavingsWalletGoalStr,
                                onValueChange = { newSavingsWalletGoalStr = it },
                                label = { Text("Số dư tích lũy ban đầu (đ)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                IconButton(onClick = { viewModel.deleteWallet(wt) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // --- SAVINGS TRANSACTION ACTION FORM ---
                if (savingsWallets.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                            // 1. Double Entry logic for DEPOSIT
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
                                            // 2. Double Entry logic for WITHDRAW
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

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

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
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("close_savings_management_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Đóng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    )
}
