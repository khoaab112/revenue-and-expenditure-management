package com.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.data.Debt
import com.app.data.Wallet
import com.app.ui.FinanceViewModel
import com.app.ui.FormatHelper
import com.app.ui.components.CustomMoneyInputField
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtBookScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit
) {
    val debts by viewModel.allDebts.collectAsState()
    val wallets by viewModel.allWallets.collectAsState()
    val dailyTransactions by viewModel.dailyTransactions.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Đi Vay", "Cho Vay")
    
    var showAddDialog by remember { mutableStateOf(false) }
    var debtToPay by remember { mutableStateOf<Debt?>(null) }
    var debtToIncrease by remember { mutableStateOf<Debt?>(null) }
    var debtForHistory by remember { mutableStateOf<Debt?>(null) }

    val filteredDebts = remember(debts, selectedTab) {
        debts.filter { debt ->
            if (selectedTab == 0) debt.type == "DEBT" else debt.type == "LOAN"
        }
    }

    // Sort order:
    // 1. ACTIVE overdue debts
    // 2. ACTIVE debts coming due soonest (dueDate ascending)
    // 3. ACTIVE debts without due date
    // 4. COMPLETED debts last
    val sortedDebts = remember(filteredDebts) {
        val now = System.currentTimeMillis()
        filteredDebts.sortedWith(
            compareBy<Debt> { debt ->
                val isDone = debt.status == "COMPLETED" || debt.remainingAmount <= 0.01
                val isOverdue = debt.dueDate != null && debt.dueDate < now && !isDone
                when {
                    isDone -> 4
                    isOverdue -> 1
                    debt.dueDate != null -> 2
                    else -> 3
                }
            }.thenBy { debt ->
                debt.dueDate ?: Long.MAX_VALUE
            }.thenByDescending { debt ->
                debt.creationDate
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (sortedDebts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Empty",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Không có khoản nợ nào",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, 
                        end = 16.dp, 
                        top = 16.dp, 
                        bottom = 80.dp // extra padding to scroll past the FAB
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(sortedDebts, key = { it.id }) { debt ->
                        DebtItemCard(
                            debt = debt,
                            onPayClick = { debtToPay = it },
                            onIncreaseClick = { debtToIncrease = it },
                            onHistoryClick = { debtForHistory = it }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Debt")
        }
    }

    if (showAddDialog) {
        AddDebtDialog(
            viewModel = viewModel,
            wallets = wallets,
            defaultType = if (selectedTab == 0) "DEBT" else "LOAN",
            onDismiss = { showAddDialog = false },
            onAdd = { personName, amount, type, note, dueDate, walletId, repaymentType, periodicAmount, periodType ->
                viewModel.addDebt(personName, amount, type, note, dueDate, walletId, repaymentType, periodicAmount, periodType)
                showAddDialog = false
            }
        )
    }

    debtToPay?.let { debt ->
        PayDebtDialog(
            debt = debt,
            wallets = wallets,
            onDismiss = { debtToPay = null },
            onPay = { amount, walletId, note ->
                viewModel.payDebt(debt, amount, walletId, note)
                debtToPay = null
            }
        )
    }

    debtToIncrease?.let { debt ->
        IncreaseDebtDialog(
            debt = debt,
            wallets = wallets,
            onDismiss = { debtToIncrease = null },
            onIncrease = { amount, walletId, note ->
                viewModel.increaseDebt(debt, amount, walletId, note)
                debtToIncrease = null
            }
        )
    }

    debtForHistory?.let { debt ->
        DebtHistoryBottomSheet(
            debt = debt,
            allTransactions = dailyTransactions,
            onDismiss = { debtForHistory = null }
        )
    }
}

@Composable
fun DebtItemCard(
    debt: Debt,
    onPayClick: (Debt) -> Unit,
    onIncreaseClick: (Debt) -> Unit,
    onHistoryClick: (Debt) -> Unit
) {
    val now = System.currentTimeMillis()
    val isCompleted = debt.status == "COMPLETED" || debt.remainingAmount <= 0.01
    val paidAmount = Math.max(0.0, debt.totalAmount - debt.remainingAmount)
    val paidPercent = if (debt.totalAmount > 0) Math.min(100, Math.max(0, ((paidAmount / debt.totalAmount) * 100).toInt())) else 100

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Date progress calculation
    val dateProgress: Float
    val datePercentText: String
    if (debt.dueDate != null && debt.dueDate > debt.creationDate) {
        val calculated = ((now - debt.creationDate).toFloat() / (debt.dueDate - debt.creationDate).toFloat()).coerceIn(0f, 1f)
        dateProgress = calculated
        datePercentText = "${(calculated * 100).toInt()}%"
    } else {
        dateProgress = 0.30f
        datePercentText = "30%"
    }

    // Overdue or Remaining Days check
    val isOverdue = debt.dueDate != null && debt.dueDate < now && !isCompleted
    val overdueDays = if (isOverdue) Math.max(1, ((now - debt.dueDate!!) / (1000 * 60 * 60 * 24)).toInt()) else 0

    val daysRemaining = if (debt.dueDate != null && debt.dueDate >= now) {
        val diff = debt.dueDate - now
        Math.max(1, (diff / (1000 * 60 * 60 * 24)).toInt())
    } else null

    // Muted alpha for content when completed (but history button stays full opacity)
    val contentAlpha = if (isCompleted) 0.45f else 1.0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF1FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            // TOP ROW: AVATAR + NAME + TYPE | METRICS (dimmed when completed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // LEFT: AVATAR + NAME + TYPE
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .border(2.dp, Color(0xFF2196F3), CircleShape)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF1E88E5),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = debt.personName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF1F2937),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val repaymentText = when (debt.repaymentType) {
                            "ONE_TIME" -> "Trả 1 lần"
                            "INSTALLMENT" -> "Trả nhiều kỳ"
                            "FLEXIBLE" -> "Linh hoạt"
                            "PERIODIC_FLEXIBLE" -> "Định kỳ"
                            "ACCUMULATING" -> "Nợ cộng dồn"
                            else -> "Linh hoạt"
                        }
                        Text(
                            text = "Hình thức : $repaymentText",
                            fontSize = 12.sp,
                            color = Color(0xFF4B5563)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // RIGHT: METRICS ONLY (no donut here - it's on progress bar row)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatHelper.formatVND(debt.totalAmount),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFF1E88E5)
                    )
                    Text(
                        text = "đã trả : ${FormatHelper.formatVND(paidAmount)}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "còn : ${FormatHelper.formatVND(debt.remainingAmount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFFE53935)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // MIDDLE ROW: START DATE (Green icon) & DUE DATE (Red icon) (dimmed when completed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // START DATE BOX (Green icon mờ)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Start Date",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text("bắt đầu", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                        Text(
                            dateFormatter.format(debt.creationDate),
                            fontSize = 11.sp,
                            color = Color(0xFF4B5563)
                        )
                    }
                }

                // DUE DATE BOX (Red icon mờ)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Due Date",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text("hạn", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                        Text(
                            text = if (debt.dueDate != null) dateFormatter.format(debt.dueDate) else "",
                            fontSize = 11.sp,
                            color = Color(0xFF4B5563)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // LINEAR PROGRESS ROW: 75% TIME LINE + DONUT CIRCLE AT THE END (arrow position!)
            // (dimmed when completed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 75% width: Progress bar + time percent text
                Row(
                    modifier = Modifier.weight(0.75f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { dateProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF7C4DFF),
                        trackColor = Color(0xFFEDE7F6)
                    )
                    Text(
                        text = datePercentText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C4DFF)
                    )
                }

                // 25% width: Donut Circle showing paid percentage (arrow points HERE!)
                Box(
                    modifier = Modifier
                        .weight(0.25f)
                        .wrapContentSize(Alignment.CenterEnd),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier.size(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { paidPercent / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF00C853),
                            strokeWidth = 4.dp,
                            trackColor = Color(0xFFEEEEEE)
                        )
                        Text(
                            text = "$paidPercent%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00C853)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // BOTTOM ROW: STATUS BADGE (LEFT) + HISTORY & ACTION BUTTONS (RIGHT)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT SIDE: STATUS BADGE (always full opacity)
                Box {
                    if (isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "ĐÃ HOÀN THÀNH",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    } else if (isOverdue) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Quá hạn $overdueDays ngày",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE53935)
                                )
                            }
                        }
                    } else if (daysRemaining != null) {
                        val badgeColor = if (daysRemaining <= 1) Color(0xFF2196F3) else Color(0xFF2E7D32)
                        val badgeBg = if (daysRemaining <= 1) Color(0xFFE3F2FD) else Color(0xFFE8F5E9)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Còn $daysRemaining ngày",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            }
                        }
                    }
                }

                // RIGHT SIDE: HISTORY BUTTON (ALWAYS FULL OPACITY!) + ACTION BUTTONS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // History Button: ALWAYS full opacity, prominent even when card is dimmed
                    Surface(
                        onClick = { onHistoryClick(debt) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCompleted) Color(0xFF78909C) else Color(0xFFB0BEC5),
                        tonalElevation = if (isCompleted) 4.dp else 2.dp,
                        shadowElevation = if (isCompleted) 3.dp else 1.dp,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Lịch sử",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Action Buttons (HIDDEN if completed, dimmed with content)
                    if (!isCompleted) {
                        if (debt.repaymentType == "ACCUMULATING") {
                            OutlinedButton(
                                onClick = { onIncreaseClick(debt) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(38.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                Text("Ghi Thêm", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { onPayClick(debt) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            modifier = Modifier.height(38.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp)
                        ) {
                            Text(if (debt.type == "DEBT") "Trả" else "Thu", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtHistoryBottomSheet(
    debt: Debt,
    allTransactions: List<com.app.data.Transaction>,
    onDismiss: () -> Unit
) {
    val debtTransactions = remember(allTransactions, debt) {
        allTransactions.filter { tx ->
            tx.debtId == debt.id || (tx.note.contains(debt.personName, ignoreCase = true) &&
                    (tx.categoryName.contains("vay", ignoreCase = true) || tx.categoryName.contains("nợ", ignoreCase = true)))
        }.sortedByDescending { it.timestamp }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Lịch sử giao dịch",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${if (debt.type == "DEBT") "Khoản nợ" else "Khoản cho vay"}: ${debt.personName}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Đóng")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (debtTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có lịch sử trả/thu nợ nào.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(debtTransactions, key = { it.id }) { tx ->
                        val isIncome = tx.type == "INCOME"
                        val statusColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
                        
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(statusColor.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isIncome) Icons.Default.Add else Icons.Default.Remove,
                                        contentDescription = tx.type,
                                        tint = statusColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.categoryName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${tx.walletName}${if (tx.note.isNotBlank()) " • " + tx.note else ""}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (isIncome) "+" else "-"}${FormatHelper.formatVND(tx.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = statusColor
                                    )
                                    Text(
                                        text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(java.util.Date(tx.timestamp)),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddDebtDialog(
    viewModel: FinanceViewModel,
    wallets: List<Wallet>,
    defaultType: String,
    onDismiss: () -> Unit,
    onAdd: (personName: String, amount: Double, type: String, note: String, dueDate: Long?, walletId: Int, repaymentType: String, periodicAmount: Double?, periodType: String?) -> Unit
) {
    val context = LocalContext.current
    var personName by remember { mutableStateOf("") }
    var rawAmount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(defaultType) }
    var note by remember { mutableStateOf("") }
    var dueDateTimestamp by remember { mutableStateOf<Long?>(null) }
    var selectedWalletId by remember { mutableStateOf<Int?>(wallets.firstOrNull()?.id) }
    
    var repaymentType by remember { mutableStateOf("FLEXIBLE") }
    var rawPeriodicAmount by remember { mutableStateOf("") }
    var periodType by remember { mutableStateOf("MONTHLY") }
    
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Các loại hình trả nợ", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "• Trả một lần: Thanh toán một lần là kết thúc.", fontSize = 14.sp)
                    Text(text = "• Trả nhiều kỳ: Chia thành nhiều kỳ trả cố định.", fontSize = 14.sp)
                    Text(text = "• Linh hoạt: Không có lịch cố định, trả linh hoạt tùy ý.", fontSize = 14.sp)
                    Text(text = "• Định kỳ linh hoạt: Trả theo kỳ nhưng số tiền mỗi kỳ tùy chọn.", fontSize = 14.sp)
                    Text(text = "• Nợ cộng dồn: Nợ tăng dần, có thể ghi thêm nợ.", fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Đã hiểu")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Thêm khoản nợ", fontWeight = FontWeight.Bold)
                IconButton(onClick = { showInfoDialog = true }) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Thông tin", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Switch
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "DEBT",
                        onClick = { type = "DEBT" },
                        label = { Text("Đi Vay") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "LOAN",
                        onClick = { type = "LOAN" },
                        label = { Text("Cho Vay") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text(if (type == "DEBT") "Người cho vay" else "Người vay") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                CustomMoneyInputField(
                    value = rawAmount,
                    onValueChange = { rawAmount = it },
                    label = "Số tiền (đ)",
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Wallet Selector
                if (wallets.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedWalletName = wallets.find { it.id == selectedWalletId }?.name ?: "Chọn ví"
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedWalletName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (type == "DEBT") "Tiền chuyển vào ví" else "Lấy tiền từ ví") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { expanded = true }
                        )
                        
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            wallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w.name) },
                                    onClick = { selectedWalletId = w.id; expanded = false }
                                )
                            }
                        }
                    }
                }
                
                // Due Date
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dueDateTimestamp?.let { dateFormatter.format(it) } ?: "Không có",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hạn trả") },
                        trailingIcon = {
                            if (dueDateTimestamp != null) {
                                IconButton(onClick = { dueDateTimestamp = null }) {
                                    Icon(Icons.Default.Close, "Xóa")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(end = 48.dp)
                            .background(Color.Transparent)
                            .clickable {
                                val cal = Calendar.getInstance()
                                dueDateTimestamp?.let { cal.timeInMillis = it }
                                DatePickerDialog(context, { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                    dueDateTimestamp = newCal.timeInMillis
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            }
                    )
                }
                
                // Repayment Type Selector
                var expandedRepayment by remember { mutableStateOf(false) }
                val repaymentMap = mapOf(
                    "ONE_TIME" to "Trả 1 lần",
                    "INSTALLMENT" to "Trả nhiều kỳ",
                    "FLEXIBLE" to "Linh hoạt",
                    "PERIODIC_FLEXIBLE" to "Định kỳ linh hoạt",
                    "ACCUMULATING" to "Nợ cộng dồn"
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = repaymentMap[repaymentType] ?: "Linh hoạt",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Loại hình trả nợ") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedRepayment = true }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Transparent)
                            .clickable { expandedRepayment = true }
                    )
                    
                    DropdownMenu(expanded = expandedRepayment, onDismissRequest = { expandedRepayment = false }) {
                        repaymentMap.forEach { (key, value) ->
                            DropdownMenuItem(
                                text = { Text(value) },
                                onClick = { repaymentType = key; expandedRepayment = false }
                            )
                        }
                    }
                }

                if (repaymentType == "PERIODIC_FLEXIBLE" || repaymentType == "INSTALLMENT") {
                    CustomMoneyInputField(
                        value = rawPeriodicAmount,
                        onValueChange = { rawPeriodicAmount = it },
                        label = "Số tiền mỗi kỳ (đ)",
                        modifier = Modifier.fillMaxWidth()
                    )

                    var expandedPeriod by remember { mutableStateOf(false) }
                    val periodMap = mapOf(
                        "WEEKLY" to "Hàng tuần",
                        "MONTHLY" to "Hàng tháng",
                        "YEARLY" to "Hàng năm"
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = periodMap[periodType] ?: "Hàng tháng",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kỳ hạn") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedPeriod = true }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { expandedPeriod = true }
                        )
                        DropdownMenu(expanded = expandedPeriod, onDismissRequest = { expandedPeriod = false }) {
                            periodMap.forEach { (key, value) ->
                                DropdownMenuItem(
                                    text = { Text(value) },
                                    onClick = { periodType = key; expandedPeriod = false }
                                )
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = FormatHelper.evaluateExpression(rawAmount)
                    val periodicAmt = if (rawPeriodicAmount.isBlank()) null else FormatHelper.evaluateExpression(rawPeriodicAmount)
                    if (personName.isNotBlank() && amt > 0 && selectedWalletId != null) {
                        onAdd(personName, amt, type, note, dueDateTimestamp, selectedWalletId!!, repaymentType, periodicAmt, periodType)
                    } else {
                        viewModel.showWarningNotification("Vui lòng điền đủ tên, số tiền và chọn ví")
                    }
                }
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun PayDebtDialog(
    debt: Debt,
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onPay: (amount: Double, walletId: Int, note: String) -> Unit
) {
    var rawAmount by remember { mutableStateOf("") }
    var selectedWalletId by remember { mutableStateOf<Int?>(wallets.firstOrNull()?.id) }
    var note by remember { mutableStateOf("") }

    val now = System.currentTimeMillis()
    val isOverdue = debt.dueDate != null && debt.dueDate < now && debt.status != "COMPLETED"
    val overdueDays = if (isOverdue) Math.max(1, ((now - debt.dueDate!!) / (1000 * 60 * 60 * 24)).toInt()) else 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (debt.type == "DEBT") "Trả nợ cho ${debt.personName}" else "Thu nợ từ ${debt.personName}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isOverdue) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Khoản nợ này đã quá hạn $overdueDays ngày!",
                                color = Color(0xFFE53935),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Text(
                    text = "Dư nợ hiện tại: ${FormatHelper.formatVND(debt.remainingAmount)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                
                CustomMoneyInputField(
                    value = rawAmount,
                    onValueChange = { rawAmount = it },
                    label = if (debt.type == "DEBT") "Số tiền trả (đ)" else "Số tiền thu (đ)",
                    placeholder = debt.remainingAmount.toLong().toString(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { rawAmount = debt.remainingAmount.toLong().toString() },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Trả hết dư nợ (${FormatHelper.formatVND(debt.remainingAmount)})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Wallet Selector
                if (wallets.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedWalletName = wallets.find { it.id == selectedWalletId }?.name ?: "Chọn ví"
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedWalletName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (debt.type == "DEBT") "Trừ tiền từ ví" else "Cộng tiền vào ví") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { expanded = true }
                        )
                        
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            wallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w.name) },
                                    onClick = { selectedWalletId = w.id; expanded = false }
                                )
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val evaluated = FormatHelper.evaluateExpression(rawAmount)
                    val amt = if (rawAmount.isBlank()) debt.remainingAmount else evaluated
                    if (amt > 0 && selectedWalletId != null) {
                        onPay(amt, selectedWalletId!!, note)
                    }
                }
            ) {
                Text("Xác nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun IncreaseDebtDialog(
    debt: Debt,
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onIncrease: (amount: Double, walletId: Int, note: String) -> Unit
) {
    var rawAmount by remember { mutableStateOf("") }
    var selectedWalletId by remember { mutableStateOf<Int?>(wallets.firstOrNull()?.id) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (debt.type == "DEBT") "Ghi thêm nợ từ ${debt.personName}" else "Cho ${debt.personName} vay thêm", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Dư nợ hiện tại: ${FormatHelper.formatVND(debt.remainingAmount)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                
                CustomMoneyInputField(
                    value = rawAmount,
                    onValueChange = { rawAmount = it },
                    label = "Số tiền phát sinh (đ)",
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (wallets.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedWalletName = wallets.find { it.id == selectedWalletId }?.name ?: "Chọn ví"
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedWalletName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (debt.type == "DEBT") "Tiền được chuyển vào ví" else "Trích tiền từ ví") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { expanded = true }
                        )
                        
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            wallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w.name) },
                                    onClick = { selectedWalletId = w.id; expanded = false }
                                )
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = FormatHelper.evaluateExpression(rawAmount)
                    if (amt > 0 && selectedWalletId != null) {
                        onIncrease(amt, selectedWalletId!!, note)
                    }
                }
            ) {
                Text("Ghi Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
