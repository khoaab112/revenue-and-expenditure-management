package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Wallet
import com.example.ui.FinanceViewModel
import com.example.ui.FormatHelper
import com.example.ui.IconMapper

@Composable
fun WalletsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val wallets by viewModel.allWallets.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedWalletForTransactions by remember { mutableStateOf<Wallet?>(null) }

    // Select the first wallet by default if not set
    LaunchedEffect(wallets) {
        if (selectedWalletForTransactions == null && wallets.isNotEmpty()) {
            selectedWalletForTransactions = wallets.first()
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

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_wallet_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm ví")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thêm ví", fontSize = 14.sp)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    wallets.forEach { wallet ->
                        val isSelected = selectedWalletForTransactions?.id == wallet.id
                        WalletBigCard(
                            wallet = wallet,
                            isSelected = isSelected,
                            onSelect = { selectedWalletForTransactions = wallet },
                            onDelete = { viewModel.deleteWallet(wallet) }
                        )
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
                        RecentTransactionItem(tx = tx)
                    }
                }
            }
        }

        // Add Wallet Dialog
        if (showAddDialog) {
            AddWalletDialog(
                onDismiss = { showAddDialog = false },
                onAddWallet = { name, type, startingBalance, color, icon ->
                    viewModel.addWallet(name, type, startingBalance, color, icon)
                    showAddDialog = false
                }
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
    modifier: Modifier = Modifier
) {
    val cardColor = FormatHelper.parseColor(wallet.colorHex)
    val outlineBorder = if (isSelected) BorderStroke(3.dp, cardColor)
                        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    Card(
        modifier = modifier
            .width(260.dp)
            .height(150.dp)
            .clickable { onSelect() }
            .testTag("wallet_big_${wallet.id}"),
        colors = CardDefaults.cardColors(
            containerColor = cardColor.copy(alpha = if (isSelected) 0.3f else 0.12f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = outlineBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = IconMapper.getIconByName(wallet.iconName),
                            contentDescription = wallet.name,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = wallet.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (wallet.type) {
                                "CASH" -> "Tiền mặt"
                                "BANK" -> "Tài khoản ngân hàng"
                                "WALLET" -> "Ví điện tử"
                                "SAVINGS" -> "Hũ tiết kiệm"
                                else -> wallet.type
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Delete Icon (If not the last default wallet to avoid empty database issues)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp).testTag("delete_wallet_${wallet.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa ví",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "Số dư tài khoản",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FormatHelper.formatVND(wallet.balance),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
    var walletName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("CASH") }
    var startingBalanceStr by remember { mutableStateOf("") }

    val colors = listOf("#FF5722", "#2196F3", "#E91E63", "#4CAF50", "#9C27B0", "#FFC107", "#009688", "#607D8B")
    var selectedColor by remember { mutableStateOf(colors.first()) }

    val icons = listOf("Payments", "AccountBalance", "AccountBalanceWallet", "Savings")
    var selectedIcon by remember { mutableStateOf(icons.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("TẠO VÍ TÀI KHOẢN MỚI") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = walletName,
                    onValueChange = { walletName = it },
                    label = { Text("Tên ví / Tài khoản") },
                    modifier = Modifier.fillMaxWidth().testTag("new_wallet_name_input")
                )

                // Type select
                Column {
                    Text("Loại Ví", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val types = listOf(
                            "CASH" to "Tiền mặt",
                            "BANK" to "Ngân hàng",
                            "WALLET" to "Ví điện tử",
                            "SAVINGS" to "Tiết kiệm"
                        )
                        types.forEach { (typeVal, label) ->
                            FilterChip(
                                selected = selectedType == typeVal,
                                onClick = { selectedType = typeVal },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = startingBalanceStr,
                    onValueChange = { startingBalanceStr = it },
                    label = { Text("Số dư ban đầu (VND)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("new_wallet_balance_input")
                )

                // Color picker
                Column {
                    Text("Màu Sắc Đại Diện", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        colors.forEach { colHex ->
                            val col = FormatHelper.parseColor(colHex)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = if (selectedColor == colHex) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colHex }
                            )
                        }
                    }
                }

                // Icon picker
                Column {
                    Text("Biểu Tượng", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        icons.forEach { iconName ->
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
        },
        confirmButton = {
            Button(
                onClick = {
                    if (walletName.isNotEmpty()) {
                        val bal = startingBalanceStr.toDoubleOrNull() ?: 0.0
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
