package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.with
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Wallet
import com.example.data.Transaction
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.FinanceViewModel
import com.example.ui.FormatHelper
import com.example.ui.IconMapper

@Composable
fun WalletsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val wallets by viewModel.dailyWallets.collectAsState()
    val transactions by viewModel.dailyTransactions.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedWalletForTransactions by remember { mutableStateOf<Wallet?>(null) }
    var walletToDelete by remember { mutableStateOf<Wallet?>(null) }
    var walletToEdit by remember { mutableStateOf<Wallet?>(null) }
    var showSelectWalletDialog by remember { mutableStateOf(false) }
    var pinnedWalletId by remember { mutableStateOf<Int?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val focusedWalletId by viewModel.focusedWalletId.collectAsState()

    if (walletToDelete != null) {
        AlertDialog(
            onDismissRequest = { walletToDelete = null },
            title = { Text("Xác nhận xóa ví?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa ví '${walletToDelete?.name}'? Hành động này cũng sẽ ảnh hưởng đến các dữ liệu liên quan và không thể hoàn tác.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        walletToDelete?.let { wallet ->
                            viewModel.deleteWallet(wallet)
                            viewModel.showSuccessNotification("Xóa ví thành công")
                        }
                        walletToDelete = null
                    }
                ) {
                    Text("XÓA", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { walletToDelete = null }) {
                    Text("HỦY")
                }
            }
        )
    }

    // Select the focused wallet or first wallet by default
    LaunchedEffect(wallets, focusedWalletId) {
        if (focusedWalletId != null) {
            val wallet = wallets.find { it.id == focusedWalletId }
            if (wallet != null) {
                selectedWalletForTransactions = wallet
                pinnedWalletId = focusedWalletId
            }
            viewModel.setFocusedWalletId(null) // Consume
        } else if (selectedWalletForTransactions == null && wallets.isNotEmpty()) {
            selectedWalletForTransactions = wallets.first()
            pinnedWalletId = wallets.first().id
        } else if (pinnedWalletId == null && selectedWalletForTransactions != null) {
            pinnedWalletId = selectedWalletForTransactions?.id
        }
    }

    Box(
        modifier = modifier.fillMaxSize().testTag("wallets_screen_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ví & Tài Khoản",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showSelectWalletDialog = true },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)).size(40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Chỉnh số dư", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            .size(40.dp)
                            .testTag("add_wallet_fab")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm ví", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wallets list (horizontal scroll with selection)
            if (wallets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Vui lòng thêm tài khoản ví đầu tiên của bạn!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                val sortedWallets = remember(wallets, pinnedWalletId) {
                    val pinnedId = pinnedWalletId
                    if (pinnedId != null) {
                        val pinnedWallet = wallets.find { it.id == pinnedId }
                        if (pinnedWallet != null) {
                            listOf(pinnedWallet) + wallets.filter { it.id != pinnedId }
                        } else {
                            wallets
                        }
                    } else {
                        wallets
                    }
                }

                val walletsListState = rememberLazyListState()
                
                LazyRow(
                    state = walletsListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedWallets) { wallet ->
                        val isSelected = selectedWalletForTransactions?.id == wallet.id
                        WalletBigCard(
                            wallet = wallet,
                            isSelected = isSelected,
                            onSelect = { selectedWalletForTransactions = wallet },
                            onDelete = { walletToDelete = wallet },
                            showDeleteButton = true,
                            modifier = Modifier.width(170.dp)
                        )
                    }
                }

                if (sortedWallets.size > 1) {
                    val currentIndex by remember {
                        derivedStateOf { walletsListState.firstVisibleItemIndex }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(sortedWallets.size) { index ->
                            val isSelected = currentIndex == index
                            val size = if (isSelected) 8.dp else 5.dp
                            val color = if (isSelected) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(size)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Associated transactions
            Text(
                text = "Lịch sử của ví: ${selectedWalletForTransactions?.name ?: ""}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            val walletTxs = transactions.filter { it.walletId == (selectedWalletForTransactions?.id ?: -1) }

            if (walletTxs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ví này chưa có giao dịch nào",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(walletTxs) { tx ->
                        WalletRecentTransactionItem(tx = tx)
                    }
                }
            }
        }

        // Add Wallet Dialog
        val context = androidx.compose.ui.platform.LocalContext.current
        if (showAddDialog) {
            AddWalletDialog(
                onDismiss = { showAddDialog = false },
                onAddWallet = { name, type, startingBalance, color, icon ->
                    viewModel.addWallet(name, type, startingBalance, color, icon)
                    viewModel.showSuccessNotification("Thêm ví/tài khoản thành công!")
                    showAddDialog = false
                }
            )
        }

        if (showSelectWalletDialog) {
            AdjustWalletFlowDialog(
                wallets = wallets,
                onDismiss = { showSelectWalletDialog = false },
                onSave = { walletId, actualBalance ->
                    viewModel.reconcileWallet(walletId, actualBalance)
                    showSelectWalletDialog = false
                }
            )
        }
    }
}

@Composable
fun WalletRecentTransactionItem(
    tx: Transaction,
    modifier: Modifier = Modifier
) {
    val isTransfer = tx.type == "TRANSFER"
    val itemBgColor = if (isTransfer) Color(0xFF2196F3).copy(alpha = 0.08f)
                      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(itemBgColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isTransfer) Color(0xFF2196F3) else FormatHelper.parseColor(tx.categoryColor)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isTransfer) Icons.AutoMirrored.Filled.CompareArrows else IconMapper.getIconByName(tx.categoryIcon),
                contentDescription = if (isTransfer) "Nội bộ" else tx.categoryName,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isTransfer) "Nội bộ" else tx.categoryName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isTransfer) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (tx.note.isNotBlank()) "${tx.walletName} - ${tx.note}" else tx.walletName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (tx.type == "EXPENSE") "-${FormatHelper.formatVND(tx.amount)}"
                       else if (tx.type == "TRANSFER") "±${FormatHelper.formatVND(tx.amount)}"
                       else "+${FormatHelper.formatVND(tx.amount)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (tx.type == "EXPENSE") Color(0xFFF44336) else if (tx.type == "TRANSFER") Color(0xFF2196F3) else Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${FormatHelper.formatDate(tx.timestamp)} ${FormatHelper.formatTime(tx.timestamp)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WalletBigCard(
    wallet: Wallet,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    showDeleteButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val cardColor = FormatHelper.parseColor(wallet.colorHex)
    val outlineBorder = if (isSelected) BorderStroke(2.5.dp, cardColor)
                        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    Card(
        modifier = modifier
            .height(115.dp)
            .clickable { onSelect() }
            .testTag("wallet_big_${wallet.id}"),
        colors = CardDefaults.cardColors(
            containerColor = cardColor.copy(alpha = if (isSelected) 0.25f else 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = outlineBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(cardColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = IconMapper.getIconByName(wallet.iconName),
                            contentDescription = wallet.name,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = wallet.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when (wallet.type) {
                                "CASH" -> "Tiền mặt"
                                "BANK" -> "Ngân hàng"
                                "WALLET" -> "Ví điện tử"
                                "SAVINGS" -> "Tiết kiệm"
                                "CREDIT" -> "Tín dụng"
                                else -> wallet.type
                            },
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (showDeleteButton) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(20.dp).testTag("delete_wallet_${wallet.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xóa ví",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = "Số dư",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FormatHelper.formatVND(wallet.balance),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AddWalletDialog(
    onDismiss: () -> Unit,
    onAddWallet: (String, String, Double, String, String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var walletName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("CASH") }
    var startingBalanceStr by remember { mutableStateOf("") }

    val colors = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#FFEB3B",
        "#FF9800", "#795548", "#607D8B", "#455A64"
    )
    var selectedColor by remember { mutableStateOf(colors.first()) }
    var isCustomColorActive by remember { mutableStateOf(false) }
    var customColorHex by remember { mutableStateOf("#9C27B0") }

    val bankIcons = listOf("AccountBalance", "Business", "Domain", "CurrencyExchange", "AssuredWorkload", "SwapHoriz", "CorporateFare", "AccountBalanceWallet")
    val cashIcons = listOf("Payments", "AccountBalanceWallet", "Money", "AttachMoney", "Wallet", "PriceCheck", "LocalAtm", "PointOfSale")
    val walletIcons = listOf("PhonelinkRing", "Contactless", "QrCode", "PhoneAndroid", "Security", "TapAndPlay", "Nfc", "MobileScreenShare")
    val savingsIcons = listOf("Savings", "Inventory", "CurrencyBitcoin", "MonetizationOn", "Star", "WorkspacePremium", "Redeem", "CardGiftcard")
    val creditIcons = listOf("CreditCard", "CreditScore", "Payment", "Receipt")

    val icons = when (selectedType) {
        "BANK" -> bankIcons
        "CASH" -> cashIcons
        "WALLET" -> walletIcons
        "SAVINGS" -> savingsIcons
        "CREDIT" -> creditIcons
        else -> cashIcons
    }
    var selectedIcon by remember { mutableStateOf(icons.first()) }

    LaunchedEffect(icons) {
        if (!icons.contains(selectedIcon)) {
            selectedIcon = icons.first()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("TẠO VÍ TÀI KHOẢN MỚI") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                        })
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = walletName,
                    onValueChange = { walletName = it },
                    label = { Text("Tên ví / Tài khoản") },
                    modifier = Modifier.fillMaxWidth().testTag("new_wallet_name_input")
                )

                // Type select in a 2x2 grid
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Loại Ví", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val types = listOf(
                        "CASH" to "Tiền mặt",
                        "BANK" to "Ngân hàng",
                        "WALLET" to "Ví điện tử",
                        "SAVINGS" to "Tiết kiệm",
                        "CREDIT" to "Thẻ tín dụng"
                    )
                    val chunked = types.chunked(3)
                    chunked.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { (typeVal, label) ->
                                FilterChip(
                                    selected = selectedType == typeVal,
                                    onClick = { selectedType = typeVal },
                                    label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                com.example.ui.components.CustomMoneyInputField(
                    value = startingBalanceStr,
                    onValueChange = { startingBalanceStr = it },
                    label = "Số dư ban đầu (VND)",
                    testTag = "new_wallet_balance_input"
                )

                // Color picker & custom color support
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Màu Sắc Đại Diện", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row 1: First 8 colors
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            colors.take(8).forEach { colHex ->
                                val isSelected = !isCustomColorActive && selectedColor == colHex
                                val col = FormatHelper.parseColor(colHex)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                        .clickable { 
                                            isCustomColorActive = false
                                            selectedColor = colHex
                                        }
                                )
                            }
                        }

                        // Row 2: Next 7 colors + Custom color circle (the 8th element)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            colors.drop(8).take(7).forEach { colHex ->
                                val isSelected = !isCustomColorActive && selectedColor == colHex
                                val col = FormatHelper.parseColor(colHex)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                        .clickable { 
                                            isCustomColorActive = false
                                            selectedColor = colHex
                                        }
                                )
                            }

                            // Custom color circle option (ends Row 2)
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

                // Icon picker
                Column {
                    Text("Biểu Tượng", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        icons.chunked(4).forEach { rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowIcons.forEach { iconName ->
                                    val isSelected = selectedIcon == iconName
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { selectedIcon = iconName },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = IconMapper.getIconByName(iconName),
                                            contentDescription = iconName,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                                                   else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (walletName.isNotEmpty()) {
                        val bal = FormatHelper.evaluateExpression(startingBalanceStr)
                        onAddWallet(walletName, selectedType, bal, selectedColor, selectedIcon)
                    }
                },
                modifier = Modifier.testTag("dialog_confirm_add_wallet")
            ) {
                Text("Tạo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

enum class AdjustWalletStep { SELECT_WALLET, EDIT_WALLET }

@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun AdjustWalletFlowDialog(
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onSave: (Int, Double) -> Unit
) {
    var currentStep by remember { mutableStateOf(AdjustWalletStep.SELECT_WALLET) }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().animateContentSize()
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState == AdjustWalletStep.EDIT_WALLET) {
                        (androidx.compose.animation.slideInHorizontally { width -> width } + androidx.compose.animation.fadeIn()).with(androidx.compose.animation.slideOutHorizontally { width -> -width } + androidx.compose.animation.fadeOut())
                    } else {
                        (androidx.compose.animation.slideInHorizontally { width -> -width } + androidx.compose.animation.fadeIn()).with(androidx.compose.animation.slideOutHorizontally { width -> width } + androidx.compose.animation.fadeOut())
                    }
                },
                label = "AdjustWalletTransition"
            ) { step ->
                when (step) {
                    AdjustWalletStep.SELECT_WALLET -> {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Chọn ví cần điều chỉnh", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            if (wallets.isEmpty()) {
                                Text("Bạn chưa có ví nào. Vui lòng thêm ví trước.")
                            } else {
                                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(wallets.size) { index ->
                                        val wallet = wallets[index]
                                        WalletBigCard(
                                            wallet = wallet,
                                            isSelected = false,
                                            onSelect = { 
                                                selectedWallet = wallet
                                                currentStep = AdjustWalletStep.EDIT_WALLET 
                                            },
                                            onDelete = { },
                                            showDeleteButton = false,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = onDismiss) { Text("Đóng") }
                            }
                        }
                    }
                    AdjustWalletStep.EDIT_WALLET -> {
                        val wallet = selectedWallet!!
                        val focusManager = LocalFocusManager.current
                        var actualBalanceStr by remember { mutableStateOf("") }
                        var showEmptyConfirmDialog by remember { mutableStateOf(false) }

                        if (showEmptyConfirmDialog) {
                            AlertDialog(
                                onDismissRequest = { showEmptyConfirmDialog = false },
                                title = { Text("Xác nhận số dư 0đ") },
                                text = { Text("Bạn để trống số dư thực tế. Điều này đồng nghĩa số dư của ví sẽ được điều chỉnh về 0đ. Bạn có chắc chắn không?") },
                                confirmButton = {
                                    Button(onClick = {
                                        showEmptyConfirmDialog = false
                                        onSave(wallet.id, 0.0)
                                    }) {
                                        Text("Chắc chắn (0đ)")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showEmptyConfirmDialog = false }) {
                                        Text("Hủy, tôi nhập lại")
                                    }
                                }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = {
                                        focusManager.clearFocus()
                                    })
                                },
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { currentStep = AdjustWalletStep.SELECT_WALLET }) {
                                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở lại")
                                    }
                                    Text("ĐIỀU CHỈNH SỐ DƯ", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }

                            Text(
                                text = "Ví: ${wallet.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            
                            Text(
                                text = "Số dư hiện tại: ${FormatHelper.formatVND(wallet.balance)}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            com.example.ui.components.CustomMoneyInputField(
                                value = actualBalanceStr,
                                onValueChange = { actualBalanceStr = it },
                                label = "Số dư thực tế",
                                testTag = "edit_wallet_balance_input"
                            )

                            val actualBalance = try {
                                FormatHelper.evaluateExpression(actualBalanceStr)
                            } catch (e: Exception) {
                                0.0
                            }

                            val diff = actualBalance - wallet.balance
                            if (diff != 0.0) {
                                val diffText = if (diff > 0) "+${FormatHelper.formatVND(diff)}" else "-${FormatHelper.formatVND(Math.abs(diff))}"
                                val diffColor = if (diff > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                val actionText = if (diff > 0) "Sẽ tạo giao dịch 'Điều chỉnh tăng số dư ví'" else "Sẽ tạo giao dịch 'Điều chỉnh giảm số dư ví'"

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(horizontal = 12.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Chênh lệch:", fontSize = 14.sp)
                                        Text(diffText, color = diffColor, fontWeight = FontWeight.Bold)
                                    }
                                    Text(actionText, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = onDismiss) { Text("Hủy") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (actualBalanceStr.isBlank()) {
                                            showEmptyConfirmDialog = true
                                        } else {
                                            val finalBalance = try {
                                                FormatHelper.evaluateExpression(actualBalanceStr)
                                            } catch (e: Exception) {
                                                0.0
                                            }
                                            onSave(wallet.id, finalBalance)
                                        }
                                    },
                                    modifier = Modifier.testTag("dialog_confirm_edit_wallet")
                                ) {
                                    Text("Lưu")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
