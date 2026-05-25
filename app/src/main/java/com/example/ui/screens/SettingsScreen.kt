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
import androidx.compose.ui.graphics.Color
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val isPinEnabled by viewModel.isPinEnabled.collectAsState()
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showWalletManagement by remember { mutableStateOf(false) }
    var showCategoryManagement by remember { mutableStateOf(false) }
    var showSavingsManagement by remember { mutableStateOf(false) }

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

        // Security Category
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

        // Financial Management Category
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

        // Configuration Information Category
        Text(
            text = "THÔNG TIN DỮ LIỆU",
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
            }
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
                                            val balance = initialBalanceStr.toDoubleOrNull() ?: 0.0
                                            viewModel.addWallet(name, walletType, balance, selectedColor, selectedIcon)
                                            name = ""
                                            initialBalanceStr = ""
                                            showAddForm = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("save_wallet_confirm")
                                ) {
                                    Text("Lưu")
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
    var type by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME, BOTH
    var selectedColor by remember { mutableStateOf("#4CAF50") }
    var selectedIcon by remember { mutableStateOf("ShoppingCart") }
    var isCustomColorActive by remember { mutableStateOf(false) }
    var customColorHex by remember { mutableStateOf("#9C27B0") }
    
    val colorPalette = listOf("#F44336", "#2196F3", "#4CAF50", "#FFC107", "#9C27B0", "#009688", "#E91E63", "#795548")
    val iconPalette = listOf("Restaurant", "DirectionsCar", "ShoppingBag", "Receipt", "SportsEsports", "School", "LocalHospital", "Home", "Work", "CardGiftcard", "Storefront")

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
                            Column {
                                Text("Biểu tượng hiển thị", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                            viewModel.addCategory(name, selectedIcon, selectedColor, type)
                                            name = ""
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
                        typeTab = selectedTypeTab
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
    typeTab: String
) {
    val listState = remember(categories) { mutableStateListOf<FinanceCategory>().apply { addAll(categories) } }
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

            ListItem(
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
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDeposit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isDeposit) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.TrendingUp, contentDescription = "In")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Nạp Tiết Kiệm")
                                }
                                Button(
                                    onClick = { isDeposit = false },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!isDeposit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (!isDeposit) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.TrendingDown, contentDescription = "Out")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rút Tiết Kiệm")
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
                            OutlinedTextField(
                                value = amountStr,
                                onValueChange = { amountStr = it },
                                label = { Text("Số tiền thực thi (đ)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    savingsTransactions.forEach { tx ->
                        ListItem(
                            headlineContent = { Text(tx.note, fontWeight = FontWeight.Bold, maxLines = 1) },
                            supportingContent = {
                                Text("${tx.walletName} • ${SimpleDateFormat("HH:mm dd/MM/yyyy", Locale("vi", "VN")).format(tx.timestamp)}")
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = if (tx.type == "INCOME") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = tx.type,
                                    tint = if (tx.type == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            },
                            trailingContent = {
                                Text(
                                    text = "${if (tx.type == "INCOME") "+" else "-"}${FormatHelper.formatVND(tx.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (tx.type == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}
