package com.app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.painterResource
import com.app.R
import com.app.ui.FinanceViewModel
import com.app.ui.FormatHelper
import com.app.ui.IconMapper
import com.app.ui.components.CustomMoneyInputField
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
    
    val sortedVaults = remember(savingsWallets) {
        savingsWallets.sortedWith(compareBy<com.app.data.Wallet> { it.isClosed }.thenByDescending { it.createdAt })
    }
    
    var isBalanceVisible by remember { mutableStateOf(true) }

    // Quick Add Savings Wallet State
    var showQuickAddWallet by remember { mutableStateOf(false) }
    var newSavingsWalletName by remember { mutableStateOf("") }
    var newSavingsWalletInitialBalanceStr by remember { mutableStateOf("") }
    var newSavingsWalletTargetAmountStr by remember { mutableStateOf("") }
    var selectedAddColor by remember { mutableStateOf("#9C27B0") }
    var selectedAddIcon by remember { mutableStateOf("Savings") }
    
    var savingsWalletToClose by remember { mutableStateOf<com.app.data.Wallet?>(null) }
    var savingsWalletToDelete by remember { mutableStateOf<com.app.data.Wallet?>(null) }
    var closeVaultTargetWalletId by remember { mutableStateOf<Int?>(null) }
    var showBottomSheetWallet by remember { mutableStateOf<com.app.data.Wallet?>(null) }

    // Detail View State
    var selectedVaultDetails by remember { mutableStateOf<com.app.data.Wallet?>(null) }
    var selectedTabIndex by remember { mutableStateOf(0) } // 0 = Lịch sử, 1 = Giao dịch
    
    // Transaction Panel State
    var isDeposit by remember { mutableStateOf(true) } // true for Deposit (Nạp), false for withdraw (Rút)
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDailyWalletId by remember { mutableStateOf<Int?>(null) }

    // Modal Bottom Sheet Đóng hũ
    if (savingsWalletToClose != null) {
        val vault = savingsWalletToClose!!
        val hasBalance = vault.balance > 0
        
        LaunchedEffect(vault.id) {
            if (dailyWallets.isNotEmpty()) {
                closeVaultTargetWalletId = dailyWallets.first().id
            } else {
                closeVaultTargetWalletId = null
            }
        }

        ModalBottomSheet(
            onDismissRequest = { savingsWalletToClose = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Đóng hũ tiết kiệm",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { savingsWalletToClose = null }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                if (hasBalance) {
                    Text(
                        text = "Chọn ví để nhận lại toàn bộ số tiền còn lại (${FormatHelper.formatVND(vault.balance)}) của hũ '${vault.name}':",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dailyWallets.size) { index ->
                            val w = dailyWallets[index]
                            val isWalletSelected = closeVaultTargetWalletId == w.id
                            val wColor = try { Color(android.graphics.Color.parseColor(w.colorHex)) } catch (e: Exception) { Color(0xFF6C5CE7) }
                            
                            Surface(
                                onClick = { closeVaultTargetWalletId = w.id },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isWalletSelected) Color(0xFF6C5CE7).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isWalletSelected) 1.5.dp else 1.dp,
                                    color = if (isWalletSelected) Color(0xFF6C5CE7) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(wColor.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = IconMapper.getIconByName(w.iconName),
                                            contentDescription = w.name,
                                            tint = wColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = w.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isWalletSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF6C5CE7),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Bạn có chắc chắn muốn đóng hũ tiết kiệm '${vault.name}'? Hũ tiết kiệm này hiện không có số dư. Hũ sẽ được đóng trực tiếp.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { savingsWalletToClose = null },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEFEFF4),
                            contentColor = Color(0xFF555555)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Hủy", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Button(
                        onClick = {
                            if (!hasBalance || closeVaultTargetWalletId != null) {
                                viewModel.closeSavingsVault(vault, closeVaultTargetWalletId)
                                if (selectedVaultDetails?.id == vault.id) {
                                    selectedVaultDetails = null
                                }
                                savingsWalletToClose = null
                            }
                        },
                        enabled = !hasBalance || closeVaultTargetWalletId != null,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C5CE7),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Xác nhận đóng hũ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }

    // Dialog Xóa hũ
    if (savingsWalletToDelete != null) {
        val vault = savingsWalletToDelete!!
        AlertDialog(
            onDismissRequest = { savingsWalletToDelete = null },
            title = { Text("Xác nhận xóa?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Bạn có chắc chắn muốn xóa hũ tiết kiệm '${vault.name}'? Thao tác này không thể hoàn tác.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWallet(vault)
                        if (selectedVaultDetails?.id == vault.id) {
                            selectedVaultDetails = null
                        }
                        savingsWalletToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("XÓA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { savingsWalletToDelete = null }) {
                    Text("HỦY")
                }
            }
        )
    }

    // Dialog Thêm sổ tiết kiệm mới
    if (showQuickAddWallet) {
        val colorOptions = listOf("#9C27B0", "#2196F3", "#009688", "#FF9800", "#E91E63", "#4CAF50")
        val iconOptions = listOf("Savings", "AccountBalance", "Home", "DirectionsCar", "Flight", "School")

        Dialog(onDismissRequest = { showQuickAddWallet = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Thêm sổ tiết kiệm mới",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = newSavingsWalletName,
                        onValueChange = { newSavingsWalletName = it },
                        label = { Text("Tên sổ tiết kiệm (*)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    CustomMoneyInputField(
                        value = newSavingsWalletInitialBalanceStr,
                        onValueChange = { newSavingsWalletInitialBalanceStr = it },
                        label = "Số tiền gửi ban đầu (đ)",
                        modifier = Modifier.fillMaxWidth()
                    )

                    CustomMoneyInputField(
                        value = newSavingsWalletTargetAmountStr,
                        onValueChange = { newSavingsWalletTargetAmountStr = it },
                        label = "Mục tiêu tích lũy (đ)",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Chọn màu sắc", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colorOptions.forEach { hex ->
                            val colorVal = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color(0xFF9C27B0) }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colorVal)
                                    .clickable { selectedAddColor = hex }
                                    .border(
                                        2.dp,
                                        if (selectedAddColor == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        CircleShape
                                    )
                            )
                        }
                    }

                    Text("Chọn biểu tượng", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        iconOptions.forEach { icName ->
                            val isSel = selectedAddIcon == icName
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) Color(0xFF6C5CE7).copy(alpha = 0.15f) else Color(0xFFF5F5F7))
                                    .clickable { selectedAddIcon = icName }
                                    .border(1.dp, if (isSel) Color(0xFF6C5CE7) else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = IconMapper.getIconByName(icName),
                                    contentDescription = icName,
                                    tint = if (isSel) Color(0xFF6C5CE7) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showQuickAddWallet = false },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEFEFF4),
                                contentColor = Color(0xFF555555)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Hủy", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Button(
                            onClick = {
                                if (newSavingsWalletName.isNotBlank()) {
                                    val initialBalance = newSavingsWalletInitialBalanceStr.toDoubleOrNull() ?: 0.0
                                    val targetAmount = newSavingsWalletTargetAmountStr.toDoubleOrNull()
                                    viewModel.addWallet(
                                        name = newSavingsWalletName,
                                        type = "SAVINGS",
                                        initialBalance = initialBalance,
                                        colorHex = selectedAddColor,
                                        iconName = selectedAddIcon,
                                        targetAmount = targetAmount
                                    )
                                    viewModel.showSuccessNotification("Thêm sổ tiết kiệm thành công!")
                                    newSavingsWalletName = ""
                                    newSavingsWalletInitialBalanceStr = ""
                                    newSavingsWalletTargetAmountStr = ""
                                    showQuickAddWallet = false
                                }
                            },
                            modifier = Modifier.weight(1.4f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6C5CE7),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Khởi tạo sổ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet cho Nút 3 chấm (⋮)
    if (showBottomSheetWallet != null) {
        val targetWallet = showBottomSheetWallet!!
        ModalBottomSheet(
            onDismissRequest = { showBottomSheetWallet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = targetWallet.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6C5CE7)
                    )
                    IconButton(onClick = { showBottomSheetWallet = null }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                ListItem(
                    headlineContent = { Text("Chi tiết & Giao dịch", fontWeight = FontWeight.Medium) },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF6C5CE7)) },
                    modifier = Modifier.clickable {
                        selectedVaultDetails = targetWallet
                        selectedTabIndex = 0
                        showBottomSheetWallet = null
                    }
                )

                if (!targetWallet.isClosed) {
                    ListItem(
                        headlineContent = { Text("Nạp / Rút tiền", fontWeight = FontWeight.Medium) },
                        leadingContent = { Icon(Icons.Default.SwapVert, contentDescription = null, tint = Color(0xFF4CAF50)) },
                        modifier = Modifier.clickable {
                            selectedVaultDetails = targetWallet
                            selectedTabIndex = 1
                            showBottomSheetWallet = null
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Đóng sổ tiết kiệm", fontWeight = FontWeight.Medium) },
                        leadingContent = { Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            savingsWalletToClose = targetWallet
                            showBottomSheetWallet = null
                        }
                    )
                }

                ListItem(
                    headlineContent = { Text("Xóa sổ tiết kiệm", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        savingsWalletToDelete = targetWallet
                        showBottomSheetWallet = null
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    val totalSavings = remember(savingsWallets) { savingsWallets.sumOf { it.balance } }

    // Auto select first vault if none selected
    LaunchedEffect(sortedVaults) {
        if (selectedVaultDetails == null && sortedVaults.isNotEmpty()) {
            selectedVaultDetails = sortedVaults.first()
        }
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =============================================================
            // SECTION 1: HEADER GRADIENT CARD (Tổng quỹ tiết kiệm)
            // =============================================================
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF7C69EF), Color(0xFFA29BFE))
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Wallet",
                                        tint = Color(0xFF6C5CE7),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.clickable { isBalanceVisible = !isBalanceVisible }
                                    ) {
                                        Text(
                                            text = "Tổng quỹ tiết kiệm",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                        Icon(
                                            imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle Balance",
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isBalanceVisible) FormatHelper.formatVND(totalSavings) else "******** đ",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${sortedVaults.size} sổ tiết kiệm",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            // 3D Vault Illustration Box
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_safe_square),
                                    contentDescription = "Safe Vault",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            // =============================================================
            // SECTION 2: SECTION TITLE & PLUS BUTTON
            // =============================================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DANH SÁCH SỔ TIẾT KIỆM",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0EEFF))
                            .clickable { showQuickAddWallet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Thêm sổ tiết kiệm",
                            tint = Color(0xFF6C5CE7),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // =============================================================
            // SECTION 3: SAVINGS CARDS LIST
            // =============================================================
            if (sortedVaults.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = "Empty",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color(0xFF6C5CE7).copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Chưa có sổ tiết kiệm nào. Bấm nút (+) để tạo mới!",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(sortedVaults, key = { it.id }) { wt ->
                    val isSelected = selectedVaultDetails?.id == wt.id
                    val cardColor = remember(wt.colorHex) {
                        try { Color(android.graphics.Color.parseColor(wt.colorHex)) } catch (e: Exception) { Color(0xFF6C5CE7) }
                    }
                    val dateStr = remember(wt.createdAt) {
                        if (wt.createdAt > 0) {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date(wt.createdAt))
                        } else "22/07/2026"
                    }
                                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (wt.isClosed) Modifier.alpha(0.65f) else Modifier)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { selectedVaultDetails = if (isSelected) null else wt },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // LEFT SIDE: ICON + NAME + BALANCE + GOAL
                                Row(
                                    modifier = Modifier.weight(1.3f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(cardColor.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = IconMapper.getIconByName(wt.iconName),
                                            contentDescription = wt.name,
                                            tint = cardColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = wt.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (wt.isClosed) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text("ĐÃ ĐÓNG", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                                }
                                            }
                                        }
                                        Text(
                                            text = "Hũ Tích Lũy",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = FormatHelper.formatVND(wt.balance),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = cardColor
                                        )
                                        if (wt.targetAmount != null && wt.targetAmount > 0.0) {
                                            Text(
                                                text = "Mục tiêu: ${FormatHelper.formatVND(wt.targetAmount)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }

                                // VERTICAL SEPARATOR LINE
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(64.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                )

                                // RIGHT SIDE: DATE + ACTION BUTTONS (ĐÓNG HŨ & XÓA)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Date Row
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(Color(0xFFF0F4FF), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = "Date",
                                                tint = Color(0xFF6C5CE7),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Column {
                                            Text("Ngày tạo", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(dateStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }

                                    // Actions Row: Close Vault Button [Archive] & Delete Button [🗑]
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Close Vault Button [Archive]
                                        if (!wt.isClosed) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(Color(0xFFF5F5F7), RoundedCornerShape(10.dp))
                                                    .clickable { savingsWalletToClose = wt },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Archive,
                                                    contentDescription = "Đóng hũ tiết kiệm",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))
                                        }

                                        // Delete Button [🗑]
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(Color(0xFFFFEAEA), RoundedCornerShape(10.dp))
                                                .clickable { savingsWalletToDelete = wt },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Xóa hũ",
                                                tint = Color(0xFFE53935),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // INLINE TABS FOR SELECTED VAULT
                            if (isSelected) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )

                                VaultInlineDetailsContent(
                                    vault = wt,
                                    savingsTransactions = savingsTransactions,
                                    dailyWallets = dailyWallets,
                                    selectedTabIndex = selectedTabIndex,
                                    onTabSelected = { selectedTabIndex = it },
                                    isDeposit = isDeposit,
                                    onDepositChange = { isDeposit = it },
                                    amountStr = amountStr,
                                    onAmountChange = { amountStr = it },
                                    note = note,
                                    onNoteChange = { note = it },
                                    selectedDailyWalletId = selectedDailyWalletId,
                                    onSelectedDailyWalletIdChange = { selectedDailyWalletId = it },
                                    focusManager = focusManager,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun VaultInlineDetailsContent(
    vault: com.app.data.Wallet,
    savingsTransactions: List<com.app.data.Transaction>,
    dailyWallets: List<com.app.data.Wallet>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    isDeposit: Boolean,
    onDepositChange: (Boolean) -> Unit,
    amountStr: String,
    onAmountChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    selectedDailyWalletId: Int?,
    onSelectedDailyWalletIdChange: (Int?) -> Unit,
    focusManager: FocusManager,
    viewModel: FinanceViewModel
) {
    val specificVaultTxs = remember(savingsTransactions, vault) {
        savingsTransactions.filter { it.walletId == vault.id || it.destinationWalletId == vault.id }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF6C5CE7),
            indicator = { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFF6C5CE7),
                        height = 3.dp
                    )
                }
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { onTabSelected(0) },
                text = { Text("Lịch sử", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { onTabSelected(1) },
                text = { Text("Giao dịch", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTabIndex == 0) {
            if (specificVaultTxs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hũ '${vault.name}' chưa có giao dịch nào.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val groupedTxs = specificVaultTxs.groupBy { FormatHelper.formatDate(it.timestamp) }
                groupedTxs.forEach { (dStr, txList) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Date",
                            tint = Color(0xFF6C5CE7),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = dStr,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6C5CE7)
                        )
                    }

                    txList.forEach { tx ->
                        val isIncrease = tx.type == "INCOME" || (tx.type == "TRANSFER" && tx.destinationWalletId == vault.id)
                        val statusColor = if (isIncrease) Color(0xFF4CAF50) else Color(0xFFF44336)
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // LEADING ICON
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(statusColor.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isIncrease) Icons.Default.Add else Icons.Default.Remove,
                                        contentDescription = tx.type,
                                        tint = statusColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // CENTER COLUMN: Category Name + Note/Wallet
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.categoryName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val subText = "${tx.walletName}${if (tx.note.isNotBlank()) " • " + tx.note else ""}"
                                    Text(
                                        text = subText,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // RIGHT COLUMN: Amount (Top line) + Time (Bottom line, right aligned)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (isIncrease) "+" else "-"}${FormatHelper.formatVND(tx.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = statusColor
                                    )
                                    val timeStr = remember(tx.timestamp) {
                                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(tx.timestamp))
                                    }
                                    Text(
                                        text = timeStr,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // TAB GIAO DỊCH (NẠP / RÚT TIỀN CHO HŨ ĐƯỢC CHỌN)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onDepositChange(true) },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDeposit) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isDeposit) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Nạp tiền", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Button(
                        onClick = { onDepositChange(false) },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isDeposit) Color(0xFFF44336) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isDeposit) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Rút tiền", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomMoneyInputField(
                        value = amountStr,
                        onValueChange = onAmountChange,
                        label = "Số tiền",
                        modifier = Modifier.weight(1f)
                    )
                    if (!isDeposit) {
                        Button(
                            onClick = { onAmountChange(vault.balance.toLong().toString()) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Rút hết", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    label = { Text("Ghi chú") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isDeposit) "Trích từ ví thường:" else "Chuyển về ví thường:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        val checkedVal = selectedDailyWalletId != null
                        Switch(
                            checked = checkedVal,
                            onCheckedChange = { isChecked ->
                                if (isChecked && dailyWallets.isNotEmpty()) {
                                    onSelectedDailyWalletIdChange(dailyWallets.first().id)
                                } else {
                                    onSelectedDailyWalletIdChange(null)
                                }
                            }
                        )
                    }
                    
                    if (selectedDailyWalletId != null) {
                        @OptIn(ExperimentalLayoutApi::class)
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            dailyWallets.forEach { dWallet ->
                                val isSel = selectedDailyWalletId == dWallet.id
                                Surface(
                                    onClick = { onSelectedDailyWalletIdChange(dWallet.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6C5CE7)) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = IconMapper.getIconByName(dWallet.iconName),
                                            contentDescription = dWallet.name,
                                            tint = try { FormatHelper.parseColor(dWallet.colorHex) } catch (e: Exception) { Color(0xFF6C5CE7) },
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "${dWallet.name} (${FormatHelper.formatVND(dWallet.balance)})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        val tgtWalletId = vault.id
                        if (amount > 0.0) {
                            val now = System.currentTimeMillis()
                            
                            if (isDeposit) {
                                if (selectedDailyWalletId != null) {
                                    viewModel.addTransaction(
                                        walletId = selectedDailyWalletId,
                                        type = "TRANSFER",
                                        amount = amount,
                                        categoryName = "Chuyển tiền",
                                        note = note.ifEmpty { "Nạp quỹ tiết kiệm '${vault.name}'" },
                                        timestamp = now,
                                        destinationWalletId = tgtWalletId
                                    )
                                } else {
                                    viewModel.addTransaction(
                                        walletId = tgtWalletId,
                                        type = "INCOME",
                                        amount = amount,
                                        categoryName = "Tiết kiệm",
                                        note = note.ifEmpty { "Gửi tiền hũ tiết kiệm" },
                                        timestamp = now
                                    )
                                }
                            } else {
                                if (selectedDailyWalletId != null) {
                                    viewModel.addTransaction(
                                        walletId = tgtWalletId,
                                        type = "TRANSFER",
                                        amount = amount,
                                        categoryName = "Chuyển tiền",
                                        note = note.ifEmpty { "Rút tiền từ hũ '${vault.name}' về ví" },
                                        timestamp = now,
                                        destinationWalletId = selectedDailyWalletId
                                    )
                                } else {
                                    viewModel.addTransaction(
                                        walletId = tgtWalletId,
                                        type = "EXPENSE",
                                        amount = amount,
                                        categoryName = "Tiết kiệm",
                                        note = note.ifEmpty { "Rút tiền hũ tiết kiệm" },
                                        timestamp = now
                                    )
                                }
                            }
                            
                            onAmountChange("")
                            onNoteChange("")
                            onSelectedDailyWalletIdChange(null)
                            focusManager.clearFocus()
                            viewModel.showSuccessNotification("Giao dịch thành công!")
                        } else {
                            viewModel.showWarningNotification("Vui lòng nhập số tiền hợp lệ")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDeposit) Color(0xFF4CAF50) else Color(0xFFF44336)),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text(if (isDeposit) "Thực hiện Nạp vào '${vault.name}'" else "Thực hiện Rút từ '${vault.name}'", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
