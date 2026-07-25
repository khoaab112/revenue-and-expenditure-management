package com.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.app.data.Wallet
import com.app.ui.FinanceViewModel
import com.app.ui.FormatHelper
import com.app.ui.IconMapper
import com.app.ui.components.AppNotificationDialog
import com.app.ui.components.DialogButtonConfig

@Composable
fun WalletManagementScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val wallets by viewModel.allWallets.collectAsState()

    // Reorder Mode State
    var isReorderMode by remember { mutableStateOf(false) }
    val listState = remember(wallets) { wallets.toMutableStateList() }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var driftY by remember { mutableStateOf(0f) }
    val scrollState = rememberScrollState()

    fun onDragReleased() {
        if (draggedIndex != null) {
            viewModel.updateWalletsOrder(listState.toList())
            draggedIndex = null
            driftY = 0f
        }
    }

    // New Wallet Form States
    var showAddForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var walletType by remember { mutableStateOf("CASH") } // CASH, BANK, WALLET, SAVINGS, CREDIT
    var initialBalanceStr by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#2196F3") }
    var selectedIcon by remember { mutableStateOf("AccountBalanceWallet") }
    var isCustomColorActive by remember { mutableStateOf(false) }
    var customColorHex by remember { mutableStateOf("#9C27B0") }
    var walletToDelete by remember { mutableStateOf<Wallet?>(null) }

    // Edit Wallet Dialog States
    var walletToEdit by remember { mutableStateOf<Wallet?>(null) }
    var editName by remember { mutableStateOf("") }
    var editBalanceStr by remember { mutableStateOf("") }
    var editSelectedColor by remember { mutableStateOf("#2196F3") }
    var isEditCustomColorActive by remember { mutableStateOf(false) }
    var editCustomColorHex by remember { mutableStateOf("#9C27B0") }

    val customTopBarTitle by viewModel.customTopBarTitle.collectAsState()

    // Intercept Back Press when in Add Form mode to gracefully return to list
    BackHandler(enabled = showAddForm) {
        showAddForm = false
    }

    // Update TopBar title in MainActivity dynamically
    LaunchedEffect(showAddForm) {
        if (showAddForm) {
            viewModel.setCustomTopBarTitle("THÊM VÍ MỚI")
        } else {
            viewModel.setCustomTopBarTitle(null)
        }
    }

    // If TopBar back button cleared customTopBarTitle while in Add Form, close sub-form
    LaunchedEffect(customTopBarTitle) {
        if (showAddForm && customTopBarTitle == null) {
            showAddForm = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setCustomTopBarTitle(null)
        }
    }

    if (walletToDelete != null) {
        AppNotificationDialog(
            showDialog = walletToDelete != null,
            title = "Xác nhận xóa ví?",
            content = "Bạn có chắc chắn muốn xóa ví '${walletToDelete?.name}'? Hành động này không thể hoàn tác.",
            cancelButton = DialogButtonConfig(
                text = "HỦY",
                action = { walletToDelete = null }
            ),
            confirmButton = DialogButtonConfig(
                text = "XÓA",
                action = {
                    walletToDelete?.let { wallet ->
                        viewModel.deleteWallet(wallet)
                        viewModel.showSuccessNotification("Xóa ví thành công")
                    }
                    walletToDelete = null
                },
                containerColor = Color(0xFFF44336),
                contentColor = Color.White
            ),
            onDismissRequest = { walletToDelete = null }
        )
    }

    val colorPalette = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#FFEB3B",
        "#FF9800", "#795548", "#607D8B", "#455A64"
    )

    if (walletToEdit != null) {
        AlertDialog(
            onDismissRequest = { walletToEdit = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                try { FormatHelper.parseColor(if (isEditCustomColorActive) editCustomColorHex else editSelectedColor).copy(alpha = 0.15f) } 
                                catch (e: Exception) { MaterialTheme.colorScheme.primaryContainer }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = IconMapper.getIconByName(walletToEdit!!.iconName),
                            contentDescription = null,
                            tint = try { FormatHelper.parseColor(if (isEditCustomColorActive) editCustomColorHex else editSelectedColor) } catch (e: Exception) { MaterialTheme.colorScheme.primary },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text("Chỉnh sửa ví", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Tên ví
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Tên ví") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_wallet_name_input")
                    )

                    // 2. Số dư hiện tại (cho phép nhập âm)
                    OutlinedTextField(
                        value = editBalanceStr,
                        onValueChange = { editBalanceStr = it },
                        label = { Text("Số dư hiện tại (đ)") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    editBalanceStr = if (editBalanceStr.startsWith("-")) {
                                        editBalanceStr.removePrefix("-")
                                    } else {
                                        "-$editBalanceStr"
                                    }
                                }
                            ) {
                                Text("±", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("edit_wallet_balance_input")
                    )

                    // 3. Màu đại diện
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Màu đại diện",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val itemsPerRow = 6
                                val totalItems = colorPalette.size + 1
                                val rowsCount = (totalItems + itemsPerRow - 1) / itemsPerRow

                                for (rowIndex in 0 until rowsCount) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (colIndex in 0 until itemsPerRow) {
                                            val itemIndex = rowIndex * itemsPerRow + colIndex
                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (itemIndex < colorPalette.size) {
                                                    val colorHex = colorPalette[itemIndex]
                                                    val isSelected = !isEditCustomColorActive && editSelectedColor == colorHex
                                                    val parsedColor = try {
                                                        FormatHelper.parseColor(colorHex)
                                                    } catch (e: Exception) { Color.Gray }

                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(parsedColor)
                                                            .clickable {
                                                                isEditCustomColorActive = false
                                                                editSelectedColor = colorHex
                                                            }
                                                            .border(
                                                                width = if (isSelected) 3.dp else 0.dp,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                shape = CircleShape
                                                            )
                                                    )
                                                } else if (itemIndex == colorPalette.size) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                try { FormatHelper.parseColor(editCustomColorHex) } catch (e: Exception) { Color.Magenta }
                                                            )
                                                            .clickable { isEditCustomColorActive = true }
                                                            .border(
                                                                width = if (isEditCustomColorActive) 3.dp else 0.dp,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                shape = CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Palette,
                                                            contentDescription = "Màu tùy chỉnh",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (isEditCustomColorActive) {
                                    com.app.ui.components.ColorSliderPicker(
                                        initialColorHex = editSelectedColor,
                                        onColorChanged = { newHex ->
                                            editSelectedColor = newHex
                                            editCustomColorHex = newHex
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = editName.trim()
                        if (trimmedName.isBlank()) {
                            viewModel.showWarningNotification("Vui lòng nhập tên ví!")
                        } else {
                            val isDuplicate = wallets.any {
                                it.id != walletToEdit!!.id && it.name.trim().equals(trimmedName, ignoreCase = true)
                            }
                            if (isDuplicate) {
                                viewModel.showWarningNotification("Tên ví '$trimmedName' đã tồn tại! Vui lòng chọn tên khác.")
                            } else {
                                val cleanStr = editBalanceStr.replace(".", "").replace(",", ".")
                                val parsedBalance = cleanStr.toDoubleOrNull()
                                    ?: try { FormatHelper.evaluateExpression(editBalanceStr) } catch (e: Exception) { 0.0 }

                                val finalColor = if (isEditCustomColorActive) editCustomColorHex else editSelectedColor

                                val updated = walletToEdit!!.copy(
                                    name = trimmedName,
                                    balance = parsedBalance,
                                    colorHex = finalColor
                                )

                                viewModel.updateWallet(updated)
                                viewModel.showSuccessNotification("Cập nhật thông tin ví thành công")
                                walletToEdit = null
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Lưu thay đổi", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { walletToEdit = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Hủy")
                }
            }
        )
    }
    val bankIcons = listOf("AccountBalance", "Business", "Domain", "CurrencyExchange", "AssuredWorkload", "SwapHoriz", "CorporateFare", "AccountBalanceWallet")
    val cashIcons = listOf("Payments", "AccountBalanceWallet", "Money", "AttachMoney", "Wallet", "PriceCheck", "LocalAtm", "PointOfSale")
    val walletIcons = listOf("PhonelinkRing", "Contactless", "QrCode", "PhoneAndroid", "Security", "TapAndPlay", "Nfc", "MobileScreenShare")
    val savingsIcons = listOf("Savings", "Inventory", "CurrencyBitcoin", "MonetizationOn", "Star", "WorkspacePremium", "Redeem", "CardGiftcard")
    val creditIcons = listOf("CreditCard", "CreditScore", "Payment", "Receipt")

    val iconPalette = when (walletType) {
        "BANK" -> bankIcons
        "CASH" -> cashIcons
        "WALLET" -> walletIcons
        "SAVINGS" -> savingsIcons
        "CREDIT" -> creditIcons
        else -> cashIcons
    }
    
    LaunchedEffect(iconPalette) {
        if (!iconPalette.contains(selectedIcon)) {
            selectedIcon = iconPalette.first()
        }
    }

    val typeDisplayName = mapOf("CASH" to "Tiền mặt", "BANK" to "Ngân hàng", "WALLET" to "Ví điện tử", "SAVINGS" to "Tích lũy", "CREDIT" to "Thẻ tín dụng")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFBFBFB))
    ) {
        if (!showAddForm) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Wallets List
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(start = 16.dp, end = if (isReorderMode && scrollState.maxValue > 0) 38.dp else 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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

                        val cardAlpha = if (isReorderMode) {
                            if (isDragged) 1.0f else 0.55f
                        } else 1.0f

                        val cardModifier = Modifier
                            .fillMaxWidth()
                            .zIndex(zIndexValue)
                            .graphicsLayer {
                                translationY = verticalOffset
                                scaleX = scaleValue
                                scaleY = scaleValue
                            }
                            .alpha(cardAlpha)

                        val dragModifier = if (isReorderMode) {
                            cardModifier.pointerInput(index) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggedIndex = index
                                        driftY = 0f
                                    },
                                    onDragEnd = { onDragReleased() },
                                    onDragCancel = { onDragReleased() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val targetIdx = draggedIndex
                                        if (targetIdx != null) {
                                            val itemHeightPx = 76.dp.toPx()
                                            
                                            // Clamping drift to strictly prevent dragging out of list boundaries
                                            val minAllowedDrift = if (targetIdx > 0) -itemHeightPx * 1.1f else 0f
                                            val maxAllowedDrift = if (targetIdx < listState.lastIndex) itemHeightPx * 1.1f else 0f
                                            
                                            driftY = (driftY + dragAmount.y).coerceIn(minAllowedDrift, maxAllowedDrift)

                                            if (driftY > itemHeightPx * 0.7f && targetIdx < listState.lastIndex) {
                                                val next = listState[targetIdx + 1]
                                                listState[targetIdx + 1] = listState[targetIdx]
                                                listState[targetIdx] = next
                                                draggedIndex = targetIdx + 1
                                                driftY -= itemHeightPx
                                            } else if (driftY < -itemHeightPx * 0.7f && targetIdx > 0) {
                                                val prev = listState[targetIdx - 1]
                                                listState[targetIdx - 1] = listState[targetIdx]
                                                listState[targetIdx] = prev
                                                draggedIndex = targetIdx - 1
                                                driftY += itemHeightPx
                                            }
                                            
                                            // Synchronous auto-scroll without flooding main thread coroutines
                                            if (driftY > 50f && scrollState.value < scrollState.maxValue && targetIdx < listState.lastIndex) {
                                                scrollState.dispatchRawDelta(12f)
                                            } else if (driftY < -50f && scrollState.value > 0 && targetIdx > 0) {
                                                scrollState.dispatchRawDelta(-12f)
                                            }
                                        }
                                    }
                                )
                            }
                        } else cardModifier.clickable {
                            walletToEdit = wt
                            editName = wt.name
                            editBalanceStr = if (wt.balance == wt.balance.toLong().toDouble()) {
                                wt.balance.toLong().toString()
                            } else {
                                wt.balance.toString()
                            }
                            editSelectedColor = wt.colorHex
                            isEditCustomColorActive = !colorPalette.contains(wt.colorHex)
                            editCustomColorHex = wt.colorHex
                        }

                        Card(
                            modifier = dragModifier,
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                width = if (isDragged) 2.dp else 1.dp,
                                color = if (isDragged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isDragged) 8.dp else 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(colorValue.copy(alpha = 0.12f))
                                            .clickable(enabled = !isReorderMode) {
                                                walletToEdit = wt
                                                editName = wt.name
                                                editBalanceStr = if (wt.balance == wt.balance.toLong().toDouble()) {
                                                    wt.balance.toLong().toString()
                                                } else {
                                                    wt.balance.toString()
                                                }
                                                editSelectedColor = wt.colorHex
                                                isEditCustomColorActive = !colorPalette.contains(wt.colorHex)
                                                editCustomColorHex = wt.colorHex
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = IconMapper.getIconByName(wt.iconName),
                                            contentDescription = wt.name,
                                            tint = colorValue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = wt.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = FormatHelper.formatVND(wt.balance),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Type badge positioned on the right
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colorValue.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = typeDisplayName[wt.type] ?: wt.type,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorValue
                                        )
                                    }

                                    // Delete Icon (Hidden during reorder mode to prevent accidental deletion)
                                    if (!isReorderMode && listState.size > 1) {
                                        IconButton(
                                            onClick = { walletToDelete = wt },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Xóa ví",
                                                tint = Color(0xFFF44336),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Green Fast Scrollbar Handle (ONLY shown in Reorder Mode)
                if (isReorderMode && scrollState.maxValue > 0) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .wrapContentWidth()
                            .padding(end = 4.dp)
                            .zIndex(50f)
                    ) {
                        val density = LocalDensity.current
                        val availableHeightPx = constraints.maxHeight.toFloat()
                        val thumbHeightPx = with(density) { 72.dp.toPx() }
                        val maxThumbOffsetY = (availableHeightPx - thumbHeightPx).coerceAtLeast(1f)

                        val scrollPercent = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
                        val currentThumbOffsetY = scrollPercent * maxThumbOffsetY

                        Box(
                            modifier = Modifier
                                .offset(y = with(density) { currentThumbOffsetY.toDp() })
                                .width(28.dp)
                                .height(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF00E676))
                                .border(2.dp, Color.White, RoundedCornerShape(14.dp))
                                .pointerInput(scrollState.maxValue, availableHeightPx) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val deltaY = dragAmount.y
                                            val scrollDelta = deltaY * (scrollState.maxValue.toFloat() / maxThumbOffsetY)
                                            scrollState.dispatchRawDelta(scrollDelta)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.width(12.dp).height(3.dp).background(Color.White, CircleShape))
                                Box(modifier = Modifier.width(12.dp).height(3.dp).background(Color.White, CircleShape))
                                Box(modifier = Modifier.width(12.dp).height(3.dp).background(Color.White, CircleShape))
                            }
                        }
                    }
                }
            }

            // Fixed Bottom Action Button(s)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (isReorderMode) {
                    // Single Full Width Red "Đóng" Button when in Reorder Mode
                    Button(
                        onClick = { isReorderMode = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("toggle_reorder_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF3D00),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = "Đóng",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    // Normal State: Side-by-side Green "Sắp xếp" + Blue "Thêm ví"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { isReorderMode = true },
                            modifier = Modifier
                                .weight(0.38f)
                                .height(52.dp)
                                .testTag("toggle_reorder_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E676),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = "Sắp xếp",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Button(
                            onClick = { showAddForm = true },
                            modifier = Modifier
                                .weight(0.62f)
                                .height(52.dp)
                                .testTag("add_wallet_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0088FF),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = "Thêm ví",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Add Form Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên ví/tài khoản (ví dụ: VCB, Ví ăn uống...)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("wallet_name_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Phân loại tài khoản", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    val types = listOf(
                        Triple("CASH", "Tiền mặt", Icons.Default.Payments),
                        Triple("BANK", "Ngân hàng", Icons.Default.AccountBalance),
                        Triple("WALLET", "Ví điện tử", Icons.Default.AccountBalanceWallet),
                        Triple("SAVINGS", "Tích lũy", Icons.Default.Savings),
                        Triple("CREDIT", "Thẻ tín dụng", Icons.Default.CreditCard)
                    )
                    
                    val chunkedTypes = types.chunked(3)
                    chunkedTypes.forEach { rowTypes ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowTypes.forEach { (typeKey, label, icon) ->
                                val isSelected = walletType == typeKey
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(72.dp)
                                        .clickable { walletType = typeKey },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                            if (rowTypes.size < 3) {
                                Spacer(modifier = Modifier.weight((3 - rowTypes.size).toFloat()))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = initialBalanceStr,
                    onValueChange = { initialBalanceStr = it },
                    label = { Text("Số dư ban đầu (đ)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("wallet_balance_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Màu đại diện", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Perfectly aligned 6-column color grid with custom color icon integrated cleanly
                            val itemsPerRow = 6
                            val totalItems = colorPalette.size + 1 // 16 preset colors + 1 custom color button
                            val rowsCount = (totalItems + itemsPerRow - 1) / itemsPerRow

                            for (rowIndex in 0 until rowsCount) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (colIndex in 0 until itemsPerRow) {
                                        val itemIndex = rowIndex * itemsPerRow + colIndex
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (itemIndex < colorPalette.size) {
                                                val colorHex = colorPalette[itemIndex]
                                                val isSelected = !isCustomColorActive && selectedColor == colorHex
                                                val parsedColor = try {
                                                    FormatHelper.parseColor(colorHex)
                                                } catch (e: Exception) { Color.Gray }

                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(parsedColor)
                                                        .clickable {
                                                            isCustomColorActive = false
                                                            selectedColor = colorHex
                                                        }
                                                        .border(
                                                            width = if (isSelected) 3.dp else 0.dp,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            shape = CircleShape
                                                        )
                                                )
                                            } else if (itemIndex == colorPalette.size) {
                                                // Integrated Custom Color Picker Button
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            try { FormatHelper.parseColor(customColorHex) } catch (e: Exception) { Color.Magenta }
                                                        )
                                                        .clickable { isCustomColorActive = true }
                                                        .border(
                                                            width = if (isCustomColorActive) 3.dp else 0.dp,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Palette,
                                                        contentDescription = "Custom color",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (isCustomColorActive) {
                                com.app.ui.components.ColorSliderPicker(
                                    initialColorHex = selectedColor,
                                    onColorChanged = { newHex ->
                                        selectedColor = newHex
                                        customColorHex = newHex
                                    }
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Biểu tượng ví", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            iconPalette.chunked(4).forEach { rowIcons ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    rowIcons.forEach { iconName ->
                                        val isSelected = selectedIcon == iconName
                                        IconButton(
                                            onClick = { selectedIcon = iconName },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = IconMapper.getIconByName(iconName),
                                                contentDescription = iconName,
                                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val balance = initialBalanceStr.toDoubleOrNull() ?: 0.0
                            viewModel.addWallet(name, walletType, balance, selectedColor, selectedIcon)
                            viewModel.showSuccessNotification("Thêm ví/tài khoản thành công!")
                            name = ""
                            initialBalanceStr = ""
                            showAddForm = false
                        } else {
                            viewModel.showWarningNotification("Vui lòng nhập tên ví!")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_wallet_confirm"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0088FF),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tạo ví", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
