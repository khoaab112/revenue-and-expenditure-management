package com.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.data.Debt
import com.app.data.Wallet
import com.app.ui.FormatHelper
import com.app.ui.IconMapper
import com.app.ui.components.AppModalBottomSheet
import com.app.ui.components.CustomMoneyInputField
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.os.Build.VERSION.SDK_INT
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtBookScreen(
    transactionViewModel: com.app.ui.viewmodels.TransactionViewModel, walletViewModel: com.app.ui.viewmodels.WalletViewModel, settingsViewModel: com.app.ui.viewmodels.SettingsViewModel, syncViewModel: com.app.ui.viewmodels.SyncViewModel, aiAdvisorViewModel: com.app.ui.viewmodels.AiAdvisorViewModel,
    onNavigateBack: () -> Unit,
    initialTab: Int = 0
) {
    val context = LocalContext.current
    val gifImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val debts by transactionViewModel.allDebts.collectAsState()
    val wallets by walletViewModel.allWallets.collectAsState()
    val dailyTransactions by transactionViewModel.dailyTransactions.collectAsState()

    var selectedTab by remember { mutableStateOf(initialTab) }
    val tabs = listOf("Đi Vay", "Cho Vay")

    // Seen keys tracking for Rule 5 animation protection
    val seenKeys = remember { mutableStateOf(mutableSetOf<String>()) }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var debtToPay by remember { mutableStateOf<Debt?>(null) }
    var debtToIncrease by remember { mutableStateOf<Debt?>(null) }
    var debtForHistory by remember { mutableStateOf<Debt?>(null) }

    val filteredDebts = remember(debts, selectedTab) {
        debts.filter { debt ->
            if (selectedTab == 0) debt.type == "DEBT" else debt.type == "LOAN"
        }
    }

    // Header Metrics Calculation
    val totalRemainingAmount = remember(filteredDebts) {
        filteredDebts.filter { it.status != "COMPLETED" && it.remainingAmount > 0.01 }.sumOf { it.remainingAmount }
    }
    val totalOriginalAmount = remember(filteredDebts) {
        filteredDebts.sumOf { it.totalAmount }
    }
    val totalPaidAmount = remember(totalOriginalAmount, totalRemainingAmount) {
        (totalOriginalAmount - totalRemainingAmount).coerceAtLeast(0.0)
    }
    val overallPaidProgress = remember(totalOriginalAmount, totalPaidAmount) {
        if (totalOriginalAmount > 0.01) (totalPaidAmount / totalOriginalAmount).toFloat().coerceIn(0f, 1f) else 0f
    }
    val activeCount = remember(filteredDebts) {
        filteredDebts.count { it.status != "COMPLETED" && it.remainingAmount > 0.01 }
    }
    val nowTime = System.currentTimeMillis()
    val overdueCount = remember(filteredDebts, nowTime) {
        filteredDebts.count { it.dueDate != null && it.dueDate < nowTime && it.status != "COMPLETED" && it.remainingAmount > 0.01 }
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, 
                    end = 16.dp, 
                    top = 16.dp, 
                    bottom = 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Summary Card
                item(key = "summary_header_$selectedTab") {
                    DebtSummaryCard(
                        selectedTab = selectedTab,
                        totalRemaining = totalRemainingAmount,
                        totalPaid = totalPaidAmount,
                        overallProgress = overallPaidProgress,
                        activeCount = activeCount,
                        overdueCount = overdueCount,
                        seenKeys = seenKeys.value,
                        modifier = Modifier.staggeredEntrance(0, "debt_summary_$selectedTab", seenKeys.value)
                    )
                }

                if (sortedDebts.isEmpty()) {
                    item(key = "empty_state_$selectedTab") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp)
                                .staggeredEntrance(1, "debt_empty_$selectedTab", seenKeys.value),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(R.drawable.address_book)
                                        .build(),
                                    imageLoader = gifImageLoader,
                                    contentDescription = "Empty State",
                                    modifier = Modifier.size(110.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    if (selectedTab == 0) "Chưa có khoản đi vay nào" else "Chưa có khoản cho vay nào",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(sortedDebts, key = { _, debt -> debt.id }) { index, debt ->
                        DebtItemCard(
                            debt = debt,
                            onPayClick = { debtToPay = it },
                            onIncreaseClick = { debtToIncrease = it },
                            onHistoryClick = { debtForHistory = it },
                            onUpdateDueDate = { targetDebt, newDueDate ->
                                transactionViewModel.updateDebtDueDate(targetDebt, newDueDate)
                            },
                            onUpdateCreationDate = { targetDebt, newCreationDate ->
                                transactionViewModel.updateDebtCreationDate(targetDebt, newCreationDate)
                            },
                            seenKeys = seenKeys.value,
                            modifier = Modifier.staggeredEntrance(
                                index = index + 1,
                                key = "debt_item_${debt.id}_${debt.status}_${debt.remainingAmount}",
                                seenKeys = seenKeys.value
                            )
                        )
                    }
                }
            }
        }

        // Floating Action Button with smooth scale entrance
        var fabVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            fabVisible = true
        }
        val fabScale by animateFloatAsState(
            targetValue = if (fabVisible) 1f else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "fabScale"
        )

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .graphicsLayer {
                    scaleX = fabScale
                    scaleY = fabScale
                    alpha = fabScale
                },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Debt")
        }
    }


    if (showAddDialog) {
        AddDebtDialog(
            transactionViewModel = transactionViewModel, walletViewModel = walletViewModel, settingsViewModel = settingsViewModel, syncViewModel = syncViewModel, aiAdvisorViewModel = aiAdvisorViewModel,
            wallets = wallets,
            defaultType = if (selectedTab == 0) "DEBT" else "LOAN",
            onDismiss = { showAddDialog = false },
            onAdd = { personName, amount, type, note, dueDate, walletId, repaymentType, periodicAmount, periodType ->
                transactionViewModel.addDebt(personName, amount, type, note, dueDate, walletId, repaymentType, periodicAmount, periodType)
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
                transactionViewModel.payDebt(debt, amount, walletId, note)
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
                transactionViewModel.increaseDebt(debt, amount, walletId, note)
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
fun StripedProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF5CAE34),
    trackColor: Color = Color(0xFFF3F4F6),
    alreadyAnimated: Boolean = false
) {
    val isZero = progress <= 0.001f
    var animTarget by remember(progress, alreadyAnimated) {
        mutableFloatStateOf(if (alreadyAnimated || isZero) progress.coerceIn(0f, 1f) else 0f)
    }
    LaunchedEffect(progress, alreadyAnimated) {
        if (!isZero && !alreadyAnimated) {
            animTarget = progress.coerceIn(0f, 1f)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (isZero || alreadyAnimated) progress.coerceIn(0f, 1f) else animTarget,
        animationSpec = if (isZero || alreadyAnimated) snap() else tween(
            durationMillis = 1000,
            delayMillis = 100,
            easing = FastOutSlowInEasing
        ),
        label = "debtProgressAnim"
    )

    androidx.compose.foundation.Canvas(modifier = modifier.clip(RoundedCornerShape(50))) {
        // Draw track
        drawRect(color = trackColor, size = size)
        
        // Draw progress only if > 0
        if (animatedProgress > 0.001f) {
            val progressWidth = size.width * animatedProgress
            drawRect(
                color = color,
                size = size.copy(width = progressWidth)
            )
            
            // Draw stripes inside progress area
            clipRect(right = progressWidth) {
                val stripeWidth = 6.dp.toPx()
                val spacing = 6.dp.toPx()
                val totalStripes = (size.width / (stripeWidth + spacing)).toInt() * 2
                
                for (i in -totalStripes..totalStripes) {
                    val startX = i * (stripeWidth + spacing)
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(startX, size.height)
                        lineTo(startX + stripeWidth, size.height)
                        lineTo(startX + stripeWidth + size.height, 0f)
                        lineTo(startX + size.height, 0f)
                        close()
                    }
                    drawPath(path, color = Color.White.copy(alpha = 0.25f))
                }
            }
        }
    }
}

@Composable
fun DebtItemCard(
    debt: Debt,
    onPayClick: (Debt) -> Unit,
    onIncreaseClick: (Debt) -> Unit,
    onHistoryClick: (Debt) -> Unit,
    onUpdateDueDate: ((Debt, Long) -> Unit)? = null,
    onUpdateCreationDate: ((Debt, Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
    seenKeys: MutableSet<String>? = null
) {
    val progressKey = "debt_item_progress_${debt.id}"
    val alreadyAnimated = remember(progressKey) { seenKeys?.contains(progressKey) == true }
    LaunchedEffect(progressKey) {
        seenKeys?.add(progressKey)
    }

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

    val contentAlpha = if (isCompleted) 0.5f else 1.0f

    // Animations for overdue pulse and progress donut
    val infiniteTransition = rememberInfiniteTransition(label = "overduePulse")
    val overduePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "overduePulseAlpha"
    )

    val isDonutZero = paidPercent <= 0
    var donutTarget by remember(paidPercent, alreadyAnimated) {
        mutableFloatStateOf(if (alreadyAnimated || isDonutZero) (paidPercent / 100f).coerceIn(0f, 1f) else 0f)
    }
    LaunchedEffect(paidPercent, alreadyAnimated) {
        if (!isDonutZero && !alreadyAnimated) {
            donutTarget = (paidPercent / 100f).coerceIn(0f, 1f)
        }
    }

    val animatedDonutProgress by animateFloatAsState(
        targetValue = if (isDonutZero || alreadyAnimated) (paidPercent / 100f).coerceIn(0f, 1f) else donutTarget,
        animationSpec = if (isDonutZero || alreadyAnimated) snap() else tween(durationMillis = 1000, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "animatedDonutProgress"
    )

    val displayedPercent = if (isDonutZero) 0 else if (alreadyAnimated) paidPercent else (animatedDonutProgress * 100).toInt()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(contentAlpha),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // TOP ROW: Avatar + Name + Type and Amounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // LEFT: Avatar + Info
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF5C79FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = debt.personName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF2B2B43),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row {
                            Text(
                                text = "Hình thức: ",
                                fontSize = 13.sp,
                                color = Color.Gray
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
                                text = repaymentText,
                                fontSize = 13.sp,
                                color = Color(0xFF5C79FF),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // RIGHT: Amounts aligned
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatHelper.formatVND(debt.totalAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF5C79FF),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(modifier = Modifier.width(130.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Đã trả:", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            FormatHelper.formatVND(paidAmount),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Color(0xFF00C853)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.width(130.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Còn lại:", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            FormatHelper.formatVND(debt.remainingAmount),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Color(0xFFE53935)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // MIDDLE ROW: Start Date & Due Date boxes
            val context = LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // START DATE (Clickable to edit start date if active)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = if (!isCompleted) Color(0xFF4CAF50).copy(alpha = 0.35f) else Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isCompleted) {
                            val cal = Calendar.getInstance().apply { timeInMillis = debt.creationDate }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }
                                    onUpdateCreationDate?.invoke(debt, newCal.timeInMillis)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = "Start Date",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bắt đầu", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2B2B43))
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            dateFormatter.format(debt.creationDate),
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                // DUE DATE (Clickable to add/extend due date if active)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = if (!isCompleted) Color(0xFF5C54E5).copy(alpha = 0.35f) else Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isCompleted) {
                            val cal = Calendar.getInstance()
                            debt.dueDate?.let { cal.timeInMillis = it }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }
                                    onUpdateDueDate?.invoke(debt, newCal.timeInMillis)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(if (!isCompleted) Color(0xFFEDEBFD) else Color(0xFFFFEBEE), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = "Due Date",
                            tint = if (!isCompleted) Color(0xFF5C54E5) else Color(0xFFE53935),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kết thúc", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2B2B43))
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = if (debt.dueDate != null) dateFormatter.format(debt.dueDate) else "Chạm để đặt",
                            fontSize = 11.sp,
                            color = if (debt.dueDate != null) Color(0xFF6B7280) else Color(0xFF5C54E5),
                            fontWeight = if (debt.dueDate != null) FontWeight.Normal else FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PROGRESS ROW: Label + Linear progress (left) | Donut progress (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Column: Label + Progress Bar
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tiến độ thanh toán",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF2B2B43)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StripedProgressIndicator(
                            progress = dateProgress,
                            alreadyAnimated = alreadyAnimated,
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp),
                            color = Color(0xFF5CAE34),
                            trackColor = Color(0xFFF3F4F6)
                        )
                        Text(
                            text = datePercentText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5CAE34)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Right Column: Donut Chart
                Box(
                    modifier = Modifier.size(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { animatedDonutProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF00C853),
                        strokeWidth = 6.dp,
                        trackColor = Color(0xFFF3F4F6),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text(
                        text = "$displayedPercent%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00C853)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // BOTTOM ROW: Status Badge (Left) + Actions (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT: STATUS BADGE
                Box {
                    if (isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "Đã hoàn thành",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    } else if (isOverdue) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF0F0),
                            modifier = Modifier.graphicsLayer { alpha = overduePulseAlpha }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Quá hạn $overdueDays ngày",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE53935)
                                )
                            }
                        }
                    } else if (daysRemaining != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Còn $daysRemaining ngày",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                }

                // RIGHT: ACTIONS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // History Button (Outlined)
                    OutlinedButton(
                        onClick = { onHistoryClick(debt) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4B5563))
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Lịch sử",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lịch sử", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Pay Button (Solid)
                    if (!isCompleted) {
                        Button(
                            onClick = { onPayClick(debt) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp).defaultMinSize(minWidth = 70.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C79FF))
                        ) {
                            Text(if (debt.type == "DEBT") "Trả" else "Thu", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddDebtDialog(
    transactionViewModel: com.app.ui.viewmodels.TransactionViewModel, walletViewModel: com.app.ui.viewmodels.WalletViewModel, settingsViewModel: com.app.ui.viewmodels.SettingsViewModel, syncViewModel: com.app.ui.viewmodels.SyncViewModel, aiAdvisorViewModel: com.app.ui.viewmodels.AiAdvisorViewModel,
    wallets: List<Wallet>,
    defaultType: String,
    onDismiss: () -> Unit,
    onAdd: (personName: String, amount: Double, type: String, note: String, dueDate: Long?, walletId: Int, repaymentType: String, periodicAmount: Double?, periodType: String?) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var personName by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }

    var rawAmount by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }

    var type by remember { mutableStateOf(defaultType) }
    var note by remember { mutableStateOf("") }
    var dueDateTimestamp by remember { mutableStateOf<Long?>(null) }

    var selectedWalletId by remember { mutableStateOf<Int?>(wallets.firstOrNull()?.id) }
    var walletError by remember { mutableStateOf<String?>(null) }
    
    var repaymentType by remember { mutableStateOf("FLEXIBLE") }
    var rawPeriodicAmount by remember { mutableStateOf("") }
    var periodType by remember { mutableStateOf("MONTHLY") }
    
    var showWalletSelectModal by remember { mutableStateOf(false) }
    var showRepaymentSelectModal by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val availableWallets = remember(wallets) { wallets.filter { it.type != "SAVINGS" } }

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

    // Wallet Selection Centered Modal
    if (showWalletSelectModal) {
        AlertDialog(
            onDismissRequest = { showWalletSelectModal = false },
            title = {
                Text(
                    text = if (type == "DEBT") "Chọn ví nhận tiền" else "Chọn ví xuất tiền",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (availableWallets.isEmpty()) {
                        Text("Không có ví hợp lệ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        availableWallets.forEach { wallet ->
                            val walletColor = remember(wallet.colorHex) {
                                try { Color(android.graphics.Color.parseColor(wallet.colorHex)) } catch (e: Exception) { Color(0xFF2196F3) }
                            }
                            val isSelected = wallet.id == selectedWalletId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedWalletId = wallet.id
                                        walletError = null
                                        showWalletSelectModal = false
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(walletColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = IconMapper.getIconByName(wallet.iconName),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(wallet.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text(
                                                FormatHelper.formatVND(wallet.balance),
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedWalletId = wallet.id
                                            walletError = null
                                            showWalletSelectModal = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWalletSelectModal = false }) {
                    Text("Đóng", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Repayment Type Centered Modal
    if (showRepaymentSelectModal) {
        val repaymentOptions = listOf(
            Triple("FLEXIBLE", "Linh hoạt", "Không có lịch cố định, trả tùy ý khi có tiền"),
            Triple("ONE_TIME", "Trả 1 lần", "Thanh toán dứt điểm toàn bộ khoản nợ một lần"),
            Triple("INSTALLMENT", "Trả nhiều kỳ", "Chia khoản nợ thành nhiều kỳ trả định kỳ cố định"),
            Triple("PERIODIC_FLEXIBLE", "Định kỳ linh hoạt", "Trả theo kỳ hạn nhưng số tiền mỗi kỳ tùy chọn"),
            Triple("ACCUMULATING", "Nợ cộng dồn", "Khoản nợ tăng dần, có thể ghi phát sinh thêm")
        )
        AlertDialog(
            onDismissRequest = { showRepaymentSelectModal = false },
            title = {
                Text("Chọn hình thức trả nợ", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repaymentOptions.forEach { (key, title, desc) ->
                        val isSelected = repaymentType == key
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    repaymentType = key
                                    showRepaymentSelectModal = false
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        repaymentType = key
                                        showRepaymentSelectModal = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRepaymentSelectModal = false }) {
                    Text("Đóng", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        title = "Thêm khoản nợ",
        sheetState = sheetState,
        headerExtraActions = {
            IconButton(onClick = { showInfoDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Thông tin",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        var hasError = false
                        if (personName.isBlank()) {
                            nameError = "Vui lòng nhập tên người vay / cho vay!"
                            hasError = true
                        }
                        val amt = FormatHelper.evaluateExpression(rawAmount)
                        if (rawAmount.isBlank() || amt <= 0) {
                            amountError = "Vui lòng nhập số tiền nợ hợp lệ!"
                            hasError = true
                        }
                        if (selectedWalletId == null) {
                            walletError = "Vui lòng chọn ví liên kết!"
                            hasError = true
                        }

                        if (!hasError) {
                            val periodicAmt = if (rawPeriodicAmount.isBlank()) null else FormatHelper.evaluateExpression(rawPeriodicAmount)
                            onAdd(personName, amt, type, note, dueDateTimestamp, selectedWalletId!!, repaymentType, periodicAmt, periodType)
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5C54E5),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Tạo khoản nợ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Selector: Đi Vay / Cho Vay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { type = "DEBT" },
                    color = if (type == "DEBT") Color(0xFFFF3B30) else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallMade,
                            contentDescription = null,
                            tint = if (type == "DEBT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Đi Vay",
                            fontWeight = FontWeight.Bold,
                            color = if (type == "DEBT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { type = "LOAN" },
                    color = if (type == "LOAN") Color(0xFF34C759) else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallReceived,
                            contentDescription = null,
                            tint = if (type == "LOAN") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cho Vay",
                            fontWeight = FontWeight.Bold,
                            color = if (type == "LOAN") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Person Name Input Field
            OutlinedTextField(
                value = personName,
                onValueChange = {
                    personName = it
                    if (it.isNotBlank()) nameError = null
                },
                label = { Text(if (type == "DEBT") "Người cho vay (*)" else "Người vay (*)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (nameError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { err -> { Text(err, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Amount Input Field
            Column(modifier = Modifier.fillMaxWidth()) {
                CustomMoneyInputField(
                    value = rawAmount,
                    onValueChange = {
                        rawAmount = it
                        if (it.isNotBlank() && FormatHelper.evaluateExpression(it) > 0) amountError = null
                    },
                    label = "Số tiền (đ) (*)",
                    modifier = Modifier.fillMaxWidth()
                )
                if (amountError != null) {
                    Text(
                        text = amountError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            // Wallet Selector (Trực quan với Modal Center)
            val selectedWallet = availableWallets.find { it.id == selectedWalletId }
            val selectedWalletName = selectedWallet?.name ?: "Chọn ví (*)"

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedWalletName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (type == "DEBT") "Tiền nhận vào ví (*)" else "Tiền xuất từ ví (*)") },
                    leadingIcon = {
                        if (selectedWallet != null) {
                            val walletColor = remember(selectedWallet.colorHex) {
                                try { Color(android.graphics.Color.parseColor(selectedWallet.colorHex)) } catch (e: Exception) { Color(0xFF2196F3) }
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(walletColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = IconMapper.getIconByName(selectedWallet.iconName),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    isError = walletError != null,
                    supportingText = walletError?.let { err -> { Text(err, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWalletSelectModal = true },
                    shape = RoundedCornerShape(14.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Transparent)
                        .clickable { showWalletSelectModal = true }
                )
            }

            // Due Date Field (100% standardized OutlinedTextField matching system style)
            Box(modifier = Modifier.fillMaxWidth()) {
                val dateText = dueDateTimestamp?.let { dateFormatter.format(it) } ?: "Không có hạn trả"
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Hạn trả nợ") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (dueDateTimestamp != null) {
                            IconButton(onClick = { dueDateTimestamp = null }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Xóa",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
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

            // Repayment Type Selector (Trực quan với Modal Center)
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
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRepaymentSelectModal = true },
                    shape = RoundedCornerShape(14.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Transparent)
                        .clickable { showRepaymentSelectModal = true }
                )
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
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedPeriod = true },
                        shape = RoundedCornerShape(14.dp)
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
                                text = { Text(value, fontWeight = FontWeight.SemiBold) },
                                onClick = { periodType = key; expandedPeriod = false }
                            )
                        }
                    }
                }
            }

            // Note Input Field (Textarea format for multi-line notes)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ghi chú") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = false,
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayDebtDialog(
    debt: Debt,
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onPay: (amount: Double, walletId: Int, note: String) -> Unit
) {
    var rawAmount by remember { mutableStateOf("") }
    val availableWallets = remember(wallets) { wallets.filter { it.type != "SAVINGS" } }
    var selectedWalletId by remember { mutableStateOf<Int?>(availableWallets.firstOrNull()?.id) }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var walletError by remember { mutableStateOf<String?>(null) }
    var showWalletModal by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val isOverdue = debt.dueDate != null && debt.dueDate < now && debt.status != "COMPLETED"
    val overdueDays = if (isOverdue) Math.max(1, ((now - debt.dueDate!!) / (1000 * 60 * 60 * 24)).toInt()) else 0

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        title = if (debt.type == "DEBT") "Trả nợ cho ${debt.personName}" else "Thu nợ từ ${debt.personName}",
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
                        val evaluated = FormatHelper.evaluateExpression(rawAmount)
                        val amt = if (rawAmount.isBlank()) debt.remainingAmount else evaluated
                        var hasError = false
                        if (amt <= 0) {
                            amountError = "Vui lòng nhập số tiền hợp lệ!"
                            hasError = true
                        }
                        if (selectedWalletId == null) {
                            walletError = "Vui lòng chọn ví!"
                            hasError = true
                        }

                        if (!hasError) {
                            onPay(amt, selectedWalletId!!, note)
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5C54E5),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Xác nhận",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isOverdue) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
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

            // Banner Dư nợ hiện tại
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dư nợ hiện tại",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatHelper.formatVND(debt.remainingAmount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF5C54E5)
                    )
                }
            }

            // Input Số tiền trả
            Column(modifier = Modifier.fillMaxWidth()) {
                CustomMoneyInputField(
                    value = rawAmount,
                    onValueChange = {
                        rawAmount = it
                        if (it.isNotBlank() && FormatHelper.evaluateExpression(it) > 0) amountError = null
                    },
                    label = if (debt.type == "DEBT") "Số tiền trả (VNĐ) (*)" else "Số tiền thu (VNĐ) (*)",
                    placeholder = debt.remainingAmount.toLong().toString(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (amountError != null) {
                    Text(
                        text = amountError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val currentAmount = FormatHelper.evaluateExpression(rawAmount)
                val isFullPaid = currentAmount > 0 && Math.abs(currentAmount - debt.remainingAmount) < 0.01

                val activeColor = Color(0xFF5C54E5)
                val inactiveBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                val inactiveBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                val inactiveText = MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (isFullPaid) {
                                rawAmount = ""
                            } else {
                                rawAmount = debt.remainingAmount.toLong().toString()
                                amountError = null
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isFullPaid) activeColor.copy(alpha = 0.12f) else inactiveBg,
                    border = BorderStroke(1.5.dp, if (isFullPaid) activeColor else inactiveBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isFullPaid) activeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .border(1.dp, if (isFullPaid) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFullPaid) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (debt.type == "DEBT") "Trả hết dư nợ: ${FormatHelper.formatVND(debt.remainingAmount)}" else "Thu hết dư nợ: ${FormatHelper.formatVND(debt.remainingAmount)}",
                            fontSize = 14.sp,
                            fontWeight = if (isFullPaid) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isFullPaid) activeColor else inactiveText
                        )
                    }
                }
            }

            // Wallet Selector Dropdown
            val selectedWallet = availableWallets.find { it.id == selectedWalletId }
            val selectedWalletName = selectedWallet?.name ?: "Chọn ví (*)"
            var expandedWallet by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedWalletName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (debt.type == "DEBT") "Trừ tiền từ ví (*)" else "Cộng tiền vào ví (*)") },
                    leadingIcon = {
                        if (selectedWallet != null) {
                            val walletColor = remember(selectedWallet.colorHex) {
                                try { Color(android.graphics.Color.parseColor(selectedWallet.colorHex)) } catch (e: Exception) { Color(0xFF2196F3) }
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(walletColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = IconMapper.getIconByName(selectedWallet.iconName),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    isError = walletError != null,
                    supportingText = walletError?.let { err -> { Text(err, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedWallet = true },
                    shape = RoundedCornerShape(14.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Transparent)
                        .clickable { expandedWallet = true }
                )

                DropdownMenu(expanded = expandedWallet, onDismissRequest = { expandedWallet = false }) {
                    availableWallets.forEach { w ->
                        DropdownMenuItem(
                            text = { Text(w.name, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                selectedWalletId = w.id
                                walletError = null
                                expandedWallet = false
                            }
                        )
                    }
                }
            }

            // Note Textarea
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ghi chú") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = false,
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncreaseDebtDialog(
    debt: Debt,
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onIncrease: (amount: Double, walletId: Int, note: String) -> Unit
) {
    var rawAmount by remember { mutableStateOf("") }
    val availableWallets = remember(wallets) { wallets.filter { it.type != "SAVINGS" } }
    var selectedWalletId by remember { mutableStateOf<Int?>(availableWallets.firstOrNull()?.id) }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var walletError by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        title = if (debt.type == "DEBT") "Vay thêm từ ${debt.personName}" else "Cho ${debt.personName} vay thêm",
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
                        val amt = FormatHelper.evaluateExpression(rawAmount)
                        var hasError = false
                        if (amt <= 0) {
                            amountError = "Vui lòng nhập số tiền phát sinh hợp lệ!"
                            hasError = true
                        }
                        if (selectedWalletId == null) {
                            walletError = "Vui lòng chọn ví!"
                            hasError = true
                        }

                        if (!hasError) {
                            onIncrease(amt, selectedWalletId!!, note)
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5C54E5),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Ghi Thêm",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Banner Dư nợ hiện tại
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dư nợ hiện tại",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatHelper.formatVND(debt.remainingAmount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF5C54E5)
                    )
                }
            }

            // Input Số tiền phát sinh
            Column(modifier = Modifier.fillMaxWidth()) {
                CustomMoneyInputField(
                    value = rawAmount,
                    onValueChange = {
                        rawAmount = it
                        if (it.isNotBlank() && FormatHelper.evaluateExpression(it) > 0) amountError = null
                    },
                    label = "Số tiền phát sinh (VNĐ) (*)",
                    modifier = Modifier.fillMaxWidth()
                )
                if (amountError != null) {
                    Text(
                        text = amountError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            // Wallet Selector Dropdown
            val selectedWallet = availableWallets.find { it.id == selectedWalletId }
            val selectedWalletName = selectedWallet?.name ?: "Chọn ví (*)"
            var expandedWallet by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedWalletName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (debt.type == "DEBT") "Tiền được chuyển vào ví (*)" else "Trích tiền từ ví (*)") },
                    leadingIcon = {
                        if (selectedWallet != null) {
                            val walletColor = remember(selectedWallet.colorHex) {
                                try { Color(android.graphics.Color.parseColor(selectedWallet.colorHex)) } catch (e: Exception) { Color(0xFF2196F3) }
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(walletColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = IconMapper.getIconByName(selectedWallet.iconName),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    isError = walletError != null,
                    supportingText = walletError?.let { err -> { Text(err, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedWallet = true },
                    shape = RoundedCornerShape(14.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Transparent)
                        .clickable { expandedWallet = true }
                )

                DropdownMenu(expanded = expandedWallet, onDismissRequest = { expandedWallet = false }) {
                    availableWallets.forEach { w ->
                        DropdownMenuItem(
                            text = { Text(w.name, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                selectedWalletId = w.id
                                walletError = null
                                expandedWallet = false
                            }
                        )
                    }
                }
            }

            // Note Textarea
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ghi chú") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = false,
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
fun DebtSummaryCard(
    selectedTab: Int,
    totalRemaining: Double,
    totalPaid: Double,
    overallProgress: Float,
    activeCount: Int,
    overdueCount: Int,
    modifier: Modifier = Modifier,
    seenKeys: MutableSet<String>? = null
) {
    val progressKey = "summary_card_progress_$selectedTab"
    val alreadyAnimated = remember(progressKey) { seenKeys?.contains(progressKey) == true }
    LaunchedEffect(progressKey) {
        seenKeys?.add(progressKey)
    }

    val isDebtTab = selectedTab == 0
    val titleText = if (isDebtTab) "Tổng dư nợ cần trả" else "Tổng nợ cần thu"
    val cardGradient = if (isDebtTab) {
        Brush.linearGradient(
            colors = listOf(Color(0xFF1E293B), Color(0xFF334155))
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
        )
    }
    val accentColor = if (isDebtTab) Color(0xFFF87171) else Color(0xFF4ADE80)

    val isZero = totalRemaining <= 0.01

    var animMoneyTarget by remember(selectedTab, totalRemaining, alreadyAnimated) {
        mutableFloatStateOf(if (alreadyAnimated || isZero) totalRemaining.toFloat() else 0f)
    }
    var animProgressTarget by remember(selectedTab, overallProgress, alreadyAnimated) {
        mutableFloatStateOf(if (alreadyAnimated || isZero) overallProgress.coerceIn(0f, 1f) else 0f)
    }

    LaunchedEffect(selectedTab, totalRemaining, overallProgress, alreadyAnimated) {
        if (!isZero && !alreadyAnimated) {
            animMoneyTarget = totalRemaining.toFloat()
            animProgressTarget = overallProgress.coerceIn(0f, 1f)
        }
    }

    val animatedMoney by animateFloatAsState(
        targetValue = if (isZero || alreadyAnimated) totalRemaining.toFloat() else animMoneyTarget,
        animationSpec = if (isZero || alreadyAnimated) snap() else tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "animSummaryMoney"
    )

    val animatedSummaryProgress by animateFloatAsState(
        targetValue = if (isZero || alreadyAnimated) overallProgress.coerceIn(0f, 1f) else animProgressTarget,
        animationSpec = if (isZero || alreadyAnimated) snap() else tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "animatedSummaryProgress"
    )

    val displayedMoney = if (isZero) 0.0 else if (alreadyAnimated) totalRemaining else animatedMoney.toDouble()
    val displayedSummaryPercent = if (isZero) 0 else if (alreadyAnimated) (overallProgress * 100).toInt() else (animatedSummaryProgress * 100).toInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(accentColor.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDebtTab) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = titleText,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (overdueCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFFEF4444), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "$overdueCount quá hạn",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFCA5A5),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = FormatHelper.formatVND(displayedMoney),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Đã thanh toán: ${FormatHelper.formatVND(totalPaid)}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$displayedSummaryPercent%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                StripedProgressIndicator(
                    progress = if (isZero) 0f else animatedSummaryProgress,
                    alreadyAnimated = alreadyAnimated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = accentColor,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Đang quản lý $activeCount khoản",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.staggeredEntrance(
    index: Int,
    key: String,
    seenKeys: MutableSet<String>
): Modifier {
    val alreadySeen = remember(key) { seenKeys.contains(key) }
    var isVisibleOnScreen by remember { mutableStateOf(alreadySeen) }
    var skipAnimation by remember { mutableStateOf(alreadySeen) }

    val progress by animateFloatAsState(
        targetValue = if (isVisibleOnScreen) 1f else 0f,
        animationSpec = if (skipAnimation) snap()
        else tween(
            durationMillis = 380,
            delayMillis = (index * 45).coerceAtMost(180),
            easing = FastOutSlowInEasing
        ),
        label = "debt_staggered_$index"
    )

    val density = LocalDensity.current
    val offsetYPx = remember(density) { with(density) { 35.dp.toPx() } }
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    return this
        .onGloballyPositioned { coordinates ->
            if (!isVisibleOnScreen) {
                val y = coordinates.positionInRoot().y
                if (y < screenHeightPx - 20f) {
                    if (y <= 0) skipAnimation = true
                    seenKeys.add(key)
                    isVisibleOnScreen = true
                }
            }
        }
        .graphicsLayer {
            alpha = progress
            translationY = if (skipAnimation) 0f else (1f - progress) * offsetYPx
        }
}

