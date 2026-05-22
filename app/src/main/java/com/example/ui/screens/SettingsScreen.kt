package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FinanceViewModel

@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val isPinEnabled by viewModel.isPinEnabled.collectAsState()
    var showPinSetupDialog by remember { mutableStateOf(false) }

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
