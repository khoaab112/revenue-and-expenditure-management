package com.app.ui.components

import com.app.data.*
import com.app.ui.*

import com.app.ui.viewmodels.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        title = "Tạo ví tài khoản mới",
        sheetState = sheetState,
        footer = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Hủy",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
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
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("confirm_create_wallet_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5C54E5),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Tạo ví",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Tên ví / tài khoản (*) Input with inline error validation
            OutlinedTextField(
                value = walletName,
                onValueChange = {
                    walletName = it
                    if (it.isNotBlank()) walletNameError = null
                },
                label = { Text("Tên ví / tài khoản (*)") },
                placeholder = { Text("Ví dụ: Ví Tiền Mặt, MoMo, MB Bank...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = if (walletNameError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                isError = walletNameError != null,
                supportingText = walletNameError?.let { err -> { Text(err, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_wallet_name"),
                shape = RoundedCornerShape(14.dp)
            )

            // 2. Loại tài khoản Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Loại tài khoản",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val walletTypes = listOf(
                        Triple("CASH", "Tiền mặt", Icons.Default.Payments),
                        Triple("BANK", "Ngân hàng", Icons.Default.AccountBalance),
                        Triple("WALLET", "Ví điện tử", Icons.Default.PhonelinkRing),
                        Triple("CREDIT", "Thẻ tín dụng", Icons.Default.CreditCard)
                    )

                    walletTypes.forEach { (typeKey, label, icon) ->
                        val isSelected = selectedType == typeKey
                        val activeColor = Color(0xFF5C54E5)

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedType = typeKey },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) activeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.5.dp, if (isSelected) activeColor else Color.Transparent)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Số dư ban đầu (đ)
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = startingBalanceStr,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isDigit() }
                        startingBalanceStr = filtered
                    },
                    label = { Text("Số dư ban đầu (đ)") },
                    placeholder = { Text("0") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_wallet_starting_balance"),
                    shape = RoundedCornerShape(14.dp)
                )

                if (startingBalanceStr.isNotBlank()) {
                    val num = startingBalanceStr.toDoubleOrNull() ?: 0.0
                    Text(
                        text = "Số dư: ${FormatHelper.formatVND(num)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF34C759),
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            // 4. Màu sắc nhận diện
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Màu chủ đạo ví",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { hex ->
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color(0xFF5C54E5) }
                        val isSelected = hex == selectedColor

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Biểu tượng Icon
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Biểu tượng đại diện",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    icons.forEach { iconName ->
                        val isSelected = iconName == selectedIcon
                        val iconVec = IconMapper.getIconByName(iconName)

                        Box(
                            modifier = Modifier
                                .size(44.dp)
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
        }
    }
}
