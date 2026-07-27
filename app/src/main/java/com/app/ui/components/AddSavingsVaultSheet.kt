package com.app.ui.components

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
import androidx.compose.material.icons.outlined.Savings
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
fun AddSavingsVaultSheet(
    onDismiss: () -> Unit,
    onAddSavingsVault: (name: String, initialBalance: Double, targetAmount: Double?, color: String, icon: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var initialBalanceStr by remember { mutableStateOf("") }
    var targetAmountStr by remember { mutableStateOf("") }

    val colors = listOf("#9C27B0", "#2196F3", "#009688", "#FF9800", "#E91E63", "#4CAF50", "#5C54E5", "#FF2D55", "#00C7BE")
    var selectedColor by remember { mutableStateOf(colors.first()) }

    val icons = listOf("Savings", "AccountBalance", "Home", "DirectionsCar", "Flight", "School", "ShoppingBag", "WorkspacePremium", "Redeem", "MonetizationOn")
    var selectedIcon by remember { mutableStateOf(icons.first()) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        title = "Thêm sổ tiết kiệm mới",
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
                        if (name.isBlank()) {
                            nameError = "Vui lòng nhập tên sổ tiết kiệm!"
                        } else {
                            val initialBal = FormatHelper.parseInputNumber(initialBalanceStr)
                            val targetAmt = if (targetAmountStr.isBlank()) null else FormatHelper.parseInputNumber(targetAmountStr)
                            onAddSavingsVault(name, initialBal, targetAmt, selectedColor, selectedIcon)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("confirm_create_savings_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5C54E5),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Khởi tạo sổ",
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
            // 1. Tên sổ tiết kiệm (*) Input with inline error validation
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) nameError = null
                },
                label = { Text("Tên sổ tiết kiệm (*)") },
                placeholder = { Text("Ví dụ: Quỹ du lịch, Tiền mua xe...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Savings,
                        contentDescription = null,
                        tint = if (nameError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { err -> { Text(err, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_savings_name"),
                shape = RoundedCornerShape(14.dp)
            )

            // 2. Số tiền ban đầu (đ)
            OutlinedTextField(
                value = initialBalanceStr,
                onValueChange = { input ->
                    initialBalanceStr = FormatHelper.formatInputNumber(input)
                },
                label = { Text("Số tiền đã gửi sẵn (VNĐ)") },
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
                    .testTag("input_savings_initial_balance"),
                shape = RoundedCornerShape(14.dp)
            )

            // 3. Mục tiêu cần tích lũy (đ)
            OutlinedTextField(
                value = targetAmountStr,
                onValueChange = { input ->
                    targetAmountStr = FormatHelper.formatInputNumber(input)
                },
                label = { Text("Mục tiêu tiết kiệm (VNĐ) (Không bắt buộc)") },
                placeholder = { Text("Ví dụ: 50.000.000") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = Color(0xFFFF9500)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_savings_target_amount"),
                shape = RoundedCornerShape(14.dp)
            )

            // 4. Chọn màu nhận diện
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Màu chủ đạo sổ tiết kiệm",
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
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color(0xFF9C27B0) }
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

            // 5. Chọn Biểu tượng Icon
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
