package com.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.ui.FormatHelper
import com.app.ui.IconMapper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddWalletSheet(
    onDismiss: () -> Unit,
    onAddWallet: (name: String, type: String, startingBalance: Double, color: String, icon: String) -> Unit,
    initialType: String = "CASH"
) {
    val focusManager = LocalFocusManager.current
    var walletName by remember { mutableStateOf("") }
    var walletNameError by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf(if (initialType == "SAVINGS") "CASH" else initialType) }
    var startingBalanceStr by remember { mutableStateOf("") }

    val colors = listOf(
        "#5C54E5", "#FF2D55", "#FF3B30", "#FF9500", "#FFCC00", "#34C759", "#00C7BE",
        "#2196F3", "#3F51B5", "#9C27B0", "#E91E63", "#795548", "#607D8B", "#455A64"
    )
    var selectedColor by remember { mutableStateOf(colors.first()) }

    val bankIcons = listOf("AccountBalance", "Business", "Domain", "CurrencyExchange", "AssuredWorkload", "SwapHoriz", "CorporateFare", "AccountBalanceWallet")
    val cashIcons = listOf("Payments", "AccountBalanceWallet", "Money", "AttachMoney", "Wallet", "PriceCheck", "LocalAtm", "PointOfSale")
    val walletIcons = listOf("PhonelinkRing", "Contactless", "QrCode", "PhoneAndroid", "Security", "TapAndPlay", "Nfc", "MobileScreenShare")
    val creditIcons = listOf("CreditCard", "CreditScore", "Payment", "Receipt")

    val icons = when (selectedType) {
        "BANK" -> bankIcons
        "CASH" -> cashIcons
        "WALLET" -> walletIcons
        "CREDIT" -> creditIcons
        else -> cashIcons
    }
    var selectedIcon by remember { mutableStateOf(icons.first()) }

    LaunchedEffect(icons) {
        if (!icons.contains(selectedIcon)) {
            selectedIcon = icons.first()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // Header: Title & standard close X icon button (system format)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tạo ví tài khoản mới",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Tên ví / Tài khoản Input with inline error validation
                OutlinedTextField(
                    value = walletName,
                    onValueChange = {
                        walletName = it
                        if (it.isNotBlank()) walletNameError = null
                    },
                    placeholder = { Text("Tên ví / Tài khoản (*)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (walletNameError != null) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                tint = if (walletNameError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    isError = walletNameError != null,
                    supportingText = if (walletNameError != null) {
                        { Text(text = walletNameError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF5C54E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("new_wallet_name_input")
                )

                // 2. LOẠI VÍ Section (Daily Wallets Only)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "LOẠI VÍ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val typeOptions = listOf(
                        Triple("CASH", "Tiền mặt", Icons.Default.Payments to Color(0xFF5C54E5)),
                        Triple("BANK", "Ngân hàng", Icons.Default.AccountBalance to Color(0xFF34C759)),
                        Triple("WALLET", "Ví điện tử", Icons.Default.AccountBalanceWallet to Color(0xFF30B0C7)),
                        Triple("CREDIT", "Thẻ tín dụng", Icons.Default.CreditCard to Color(0xFFFF2D55))
                    )

                    val rows = typeOptions.chunked(2)

                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { (typeKey, label, iconColorPair) ->
                                val isSelected = selectedType == typeKey
                                val (iconVec, iconColor) = iconColorPair
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedType = typeKey },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF5C54E5).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF5C54E5)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = iconVec,
                                                contentDescription = label,
                                                tint = if (isSelected) Color(0xFF5C54E5) else iconColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = label,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color(0xFF5C54E5) else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                        }
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(3.dp)
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF5C54E5)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. SỐ DƯ BAN ĐẦU (VND) Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SỐ DƯ BAN ĐẦU (VND)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = startingBalanceStr,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || it == '.' }) {
                                startingBalanceStr = input
                            }
                        },
                        placeholder = { Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Paid,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Máy tính",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5C54E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("new_wallet_balance_input")
                    )
                }

                // 4. MÀU SẮC ĐẠI DIỆN Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "MÀU SẮC ĐẠI DIỆN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        colors.forEach { hex ->
                            val color = FormatHelper.parseColor(hex)
                            val isSelected = selectedColor == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.5.dp, Color(0xFF5C54E5), CircleShape)
                                        } else Modifier
                                    )
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Chọn màu",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. BIỂU TƯỢNG Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "BIỂU TƯỢNG",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        icons.forEach { iconName ->
                            val isSelected = selectedIcon == iconName
                            val iconVec = IconMapper.getIconByName(iconName)

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Color(0xFF5C54E5).copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(1.5.dp, Color(0xFF5C54E5), RoundedCornerShape(12.dp))
                                        } else Modifier
                                    )
                                    .clickable { selectedIcon = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVec,
                                    contentDescription = iconName,
                                    tint = if (isSelected) Color(0xFF5C54E5) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 6. Action Buttons: Hủy & Tạo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = "Hủy",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        if (walletName.isBlank()) {
                            walletNameError = "Vui lòng nhập tên ví / tài khoản!"
                        } else {
                            val bal = startingBalanceStr.toDoubleOrNull() ?: 0.0
                            onAddWallet(walletName, selectedType, bal, selectedColor, selectedIcon)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("confirm_create_wallet_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5C54E5),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Tạo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
