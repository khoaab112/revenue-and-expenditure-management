package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Categories
import com.example.data.Transaction
import com.example.data.Wallet
import com.example.data.FinanceCategory
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import com.example.ui.FinanceViewModel
import com.example.ui.FormatHelper
import com.example.ui.IconMapper
import androidx.compose.runtime.saveable.rememberSaveable
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    viewModel: FinanceViewModel,
    onNavigateToTimeline: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val dailyTransactions by viewModel.dailyTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val categoriesList by viewModel.categoriesList.collectAsState()
    val walletsList by viewModel.allWallets.collectAsState()
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    if (transactionToDelete != null) {
        val typeTag = if (transactionToDelete!!.type == "EXPENSE") "khoản chi" else "khoản thu/chuyển"
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Xác nhận xóa giao dịch?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa $typeTag '${FormatHelper.formatVND(transactionToDelete!!.amount)}' (${transactionToDelete!!.note})? Hành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToDelete?.let { tx ->
                            viewModel.deleteTransaction(tx)
                            viewModel.showSuccessNotification("Xóa giao dịch thành công")
                        }
                        transactionToDelete = null
                    }
                ) {
                    Text("XÓA", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("HỦY")
                }
            }
        )
    }

    // --- DISPLAY MODE STATES ---
    var displayMode by rememberSaveable { mutableStateOf("CALENDAR") } // LIST, CALENDAR
    var selectedCalendarYear by rememberSaveable { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedCalendarMonth by rememberSaveable { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var selectedCalendarDayOfMonth by rememberSaveable { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }

    var selectedCalendarDay = remember(selectedCalendarYear, selectedCalendarMonth, selectedCalendarDayOfMonth) {
        CalendarDay(
            year = selectedCalendarYear,
            month = selectedCalendarMonth,
            dayOfMonth = selectedCalendarDayOfMonth,
            isCurrentMonth = true // Not strictly used for tracking selection
        )
    }

    var showDayDetailDialog by remember { mutableStateOf<CalendarDay?>(null) }
    var showQuickActionMenuByDay by remember { mutableStateOf<CalendarDay?>(null) }
    var showQuickAddDialogByDay by remember { mutableStateOf<CalendarDay?>(null) }

    var showFilterSheet by remember { mutableStateOf(false) }

    // --- TIME FILTER STATES ---
    var activeTimeFilterMode by rememberSaveable { mutableStateOf("MONTH") } // ALL, WEEK, DAY, MONTH, YEAR, RANGE
    var selectedCustomDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedCustomMonth by rememberSaveable { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) } // 0 to 11
    var selectedCustomMonthYear by rememberSaveable { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedCustomYear by rememberSaveable { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedRangeStartMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedRangeEndMillis by rememberSaveable { mutableStateOf<Long?>(null) }

    val selectedCustomDate: Calendar? = selectedCustomDateMillis?.let { Calendar.getInstance().apply { timeInMillis = it } }
    val selectedRangeStart: Calendar? = selectedRangeStartMillis?.let { Calendar.getInstance().apply { timeInMillis = it } }
    val selectedRangeEnd: Calendar? = selectedRangeEndMillis?.let { Calendar.getInstance().apply { timeInMillis = it } }

    LaunchedEffect(displayMode) {
        if (displayMode == "CALENDAR") {
            if (activeTimeFilterMode != "MONTH") {
                activeTimeFilterMode = "MONTH"
            }
        }
    }

    val showDatePicker = {
        val currentCal = selectedCustomDate ?: Calendar.getInstance()
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                selectedCustomDateMillis = newCal.timeInMillis
                activeTimeFilterMode = "DAY"
            },
            currentCal.get(Calendar.YEAR),
            currentCal.get(Calendar.MONTH),
            currentCal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    var showMonthDialog by remember { mutableStateOf(false) }
    var showYearDialog by remember { mutableStateOf(false) }
    var showRangeDialog by remember { mutableStateOf(false) }

    // Synchronize Local Time Filter Selection with ViewModel filter ranges
    LaunchedEffect(
        activeTimeFilterMode,
        selectedCustomDate,
        selectedCustomMonth,
        selectedCustomMonthYear,
        selectedCustomYear,
        selectedRangeStart,
        selectedRangeEnd
    ) {
        when (activeTimeFilterMode) {
            "ALL" -> {
                viewModel.setDateFilterRange(null, null)
            }
            "WEEK" -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                viewModel.setDateFilterRange(start, end)
            }
            "DAY" -> {
                val cal = selectedCustomDate ?: Calendar.getInstance()
                val startCal = Calendar.getInstance().apply {
                    timeInMillis = cal.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = startCal.timeInMillis

                val endCal = Calendar.getInstance().apply {
                    timeInMillis = cal.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val end = endCal.timeInMillis

                viewModel.setDateFilterRange(start, end)
            }
            "MONTH" -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedCustomMonthYear)
                    set(Calendar.MONTH, selectedCustomMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis

                val endCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedCustomMonthYear)
                    set(Calendar.MONTH, selectedCustomMonth)
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val end = endCal.timeInMillis

                viewModel.setDateFilterRange(start, end)
            }
            "YEAR" -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedCustomYear)
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis

                val endCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedCustomYear)
                    set(Calendar.DAY_OF_YEAR, getActualMaximum(Calendar.DAY_OF_YEAR))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val end = endCal.timeInMillis

                viewModel.setDateFilterRange(start, end)
            }
            "RANGE" -> {
                val sCal = selectedRangeStart
                val eCal = selectedRangeEnd
                if (sCal != null && eCal != null) {
                    val startCal = Calendar.getInstance().apply {
                        timeInMillis = sCal.timeInMillis
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val endCal = Calendar.getInstance().apply {
                        timeInMillis = eCal.timeInMillis
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    viewModel.setDateFilterRange(startCal.timeInMillis, endCal.timeInMillis)
                } else {
                    viewModel.setDateFilterRange(null, null)
                }
            }
        }
    }

    // --- MONTH SELECTION DIALOG ---
    if (showMonthDialog) {
        AlertDialog(
            onDismissRequest = { showMonthDialog = false },
            title = { Text("Chọn Tháng", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedCustomMonthYear-- }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Year")
                        }
                        Text("Năm $selectedCustomMonthYear", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { selectedCustomMonthYear++ }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Year")
                        }
                    }

                    val months = listOf(
                        "Thg 1", "Thg 2", "Thg 3", "Thg 4",
                        "Thg 5", "Thg 6", "Thg 7", "Thg 8",
                        "Thg 9", "Thg 10", "Thg 11", "Thg 12"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 0 until 4) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (j in 0 until 3) {
                                    val monthIdx = i * 3 + j
                                    val isSelected = selectedCustomMonth == monthIdx
                                    Button(
                                        onClick = {
                                            selectedCustomMonth = monthIdx
                                            activeTimeFilterMode = "MONTH"
                                            showMonthDialog = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(months[monthIdx], fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // --- YEAR SELECTION DIALOG ---
    if (showYearDialog) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 4..currentYear + 2).toList()

        AlertDialog(
            onDismissRequest = { showYearDialog = false },
            title = { Text("Chọn Năm", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    years.chunked(3).forEach { rowYears ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowYears.forEach { yr ->
                                val isSelected = selectedCustomYear == yr
                                Button(
                                    onClick = {
                                        selectedCustomYear = yr
                                        activeTimeFilterMode = "YEAR"
                                        showYearDialog = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(yr.toString(), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showYearDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // --- RANGE SELECTION DIALOG ---
    if (showRangeDialog) {
        val pickerContext = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { showRangeDialog = false },
            title = { Text("Khoảng thời gian", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val currentCal = selectedRangeStart ?: Calendar.getInstance()
                                android.app.DatePickerDialog(
                                    pickerContext,
                                    { _, year, month, dayOfMonth ->
                                        selectedRangeStartMillis = Calendar.getInstance().apply {
                                            set(year, month, dayOfMonth)
                                        }.timeInMillis
                                    },
                                    currentCal.get(Calendar.YEAR),
                                    currentCal.get(Calendar.MONTH),
                                    currentCal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(
                                text = selectedRangeStart?.let {
                                    SimpleDateFormat("dd/MM/yyyy", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build()).format(it.time)
                                } ?: "Từ ngày...",
                                fontSize = 13.sp
                            )
                        }

                        Text("—", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Button(
                            onClick = {
                                val currentCal = selectedRangeEnd ?: Calendar.getInstance()
                                android.app.DatePickerDialog(
                                    pickerContext,
                                    { _, year, month, dayOfMonth ->
                                        selectedRangeEndMillis = Calendar.getInstance().apply {
                                            set(year, month, dayOfMonth)
                                        }.timeInMillis
                                    },
                                    currentCal.get(Calendar.YEAR),
                                    currentCal.get(Calendar.MONTH),
                                    currentCal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(
                                text = selectedRangeEnd?.let {
                                    SimpleDateFormat("dd/MM/yyyy", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build()).format(it.time)
                                } ?: "Đến ngày...",
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (selectedRangeStart != null && selectedRangeEnd != null) {
                        if (selectedRangeStart!!.after(selectedRangeEnd)) {
                            Text(
                                text = "Ngày bắt đầu phải nhỏ hơn ngày kết thúc!",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedRangeStart != null && selectedRangeEnd != null && !selectedRangeStart!!.after(selectedRangeEnd)) {
                            activeTimeFilterMode = "RANGE"
                            showRangeDialog = false
                        }
                    },
                    enabled = selectedRangeStart != null && selectedRangeEnd != null && !selectedRangeStart!!.after(selectedRangeEnd)
                ) {
                    Text("Áp dụng")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRangeDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Grouping transactions by date for a premium timeline look
    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { FormatHelper.formatDate(it.timestamp) }
    }

    // Precompute daily summaries to optimize lazy list scrolling performance
    val dailySummaries = remember(groupedTransactions) {
        groupedTransactions.mapValues { (_, txList) ->
            val totalIncome = txList.filter { it.type == "INCOME" }.sumOf { it.amount }
            val totalExpense = txList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            Pair(totalIncome, totalExpense)
        }
    }

    // Grouping transactions by calendar date key for instant calendar day lookup
    val transactionsByDayKey = remember(dailyTransactions) {
        dailyTransactions.groupBy { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
        }
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var isFiltersExpanded by remember { mutableStateOf(false) }

    val filterSummary = remember(
        searchQuery,
        selectedTypeFilter,
        selectedCategoryFilter,
        activeTimeFilterMode,
        selectedCustomDate,
        selectedCustomMonth,
        selectedCustomMonthYear,
        selectedCustomYear,
        selectedRangeStart,
        selectedRangeEnd
    ) {
        val parts = mutableListOf<String>()
        if (searchQuery.isNotEmpty()) {
            parts.add("Tìm: \"$searchQuery\"")
        }
        if (selectedTypeFilter != "ALL") {
            parts.add(if (selectedTypeFilter == "EXPENSE") "Chi" else "Thu")
        }
        if (selectedCategoryFilter != "ALL") {
            parts.add("Hạng mục: $selectedCategoryFilter")
        }
        val timeStr = when (activeTimeFilterMode) {
            "ALL" -> "Mọi lúc"
            "DAY" -> selectedCustomDate?.let { 
                SimpleDateFormat("dd/MM/yyyy", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build()).format(it.time) 
            } ?: "Một ngày"
            "WEEK" -> "1 Tuần qua"
            "RANGE" -> if (selectedRangeStart != null && selectedRangeEnd != null) {
                val s = SimpleDateFormat("dd/MM", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build()).format(selectedRangeStart!!.time)
                val e = SimpleDateFormat("dd/MM", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build()).format(selectedRangeEnd!!.time)
                "$s - $e"
            } else "Khoảng ngày"
            "MONTH" -> "Thg ${selectedCustomMonth + 1}/$selectedCustomMonthYear"
            "YEAR" -> "Năm $selectedCustomYear"
            else -> "Mọi lúc"
        }
        if (timeStr != "Mọi lúc") {
            parts.add(timeStr)
        }
        
        if (parts.isEmpty()) "Mọi giao dịch (Không có bộ lọc)" else parts.joinToString(" • ")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lịch Sử Giao Dịch",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            
            val exportContext = androidx.compose.ui.platform.LocalContext.current
            FilledTonalButton(
                onClick = {
                    com.example.ui.ExcelExportHelper.exportTransactionsToCsv(
                        context = exportContext,
                        transactions = filteredTransactions,
                        onWarning = { viewModel.showWarningNotification(it) },
                        onError = { viewModel.showErrorNotification(it) }
                    )
                },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("export_csv_button"),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share, 
                    contentDescription = "Xuất Excel",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Xuất Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Mode Switcher (Danh sách / Lịch)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("LIST" to "Danh sách", "CALENDAR" to "Lịch").forEach { (mode, label) ->
                val isSelected = displayMode == mode
                val bgColors = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val textColors = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColors)
                        .clickable { displayMode = mode }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (mode == "LIST") Icons.AutoMirrored.Filled.List else Icons.Default.CalendarToday,
                        contentDescription = label,
                        tint = textColors,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColors
                    )
                }
            }
        }

        if (displayMode == "LIST") {
            // COLLAPSIBLE FILTER + SEARCH GROUP CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Clickable Header to Expand/Collapse
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isFiltersExpanded = !isFiltersExpanded }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Bộ lọc và tìm kiếm",
                            tint = if (isFiltersExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Bộ lọc & Tìm kiếm",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (filterSummary.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = filterSummary,
                                    fontSize = 10.sp,
                                    color = if (filterSummary.startsWith("Mọi giao dịch")) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                           else MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = { isFiltersExpanded = !isFiltersExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isFiltersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isFiltersExpanded) "Thu gọn" else "Mở rộng",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                AnimatedVisibility(
                    visible = isFiltersExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        
                        // 1. Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Tìm kiếm ghi chú, danh mục, ví...", fontSize = 13.sp) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search Log", modifier = Modifier.size(20.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("history_search_input"),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                        )
                        
                        // 2. Transaction Type Group
                        Text(
                            text = "Loại giao dịch",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "ALL" to "Tất cả",
                                "EXPENSE" to "Khoản Chi",
                                "INCOME" to "Khoản Thu"
                            ).forEach { (typeVal, name) ->
                                val isSelected = selectedTypeFilter == typeVal
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setTypeFilter(typeVal) },
                                    label = { Text(name, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("filter_type_$typeVal"),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                        
                        // 3. Category selector Group
                        Text(
                            text = "Hạng mục giao dịch",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isAllCategory = selectedCategoryFilter == "ALL"
                            FilterChip(
                                selected = isAllCategory,
                                onClick = { viewModel.setCategoryFilter("ALL") },
                                label = { Text("Mọi danh mục", fontSize = 11.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("filter_category_ALL"),
                                shape = RoundedCornerShape(8.dp)
                            )
                
                            Box(
                                modifier = Modifier.weight(1.2f)
                            ) {
                                val selectedCatObj = categoriesList.find { it.name == selectedCategoryFilter }
                                FilterChip(
                                    selected = !isAllCategory,
                                    onClick = { categoryDropdownExpanded = true },
                                    label = {
                                        Text(
                                            text = if (isAllCategory) "Chọn danh mục..." else selectedCategoryFilter,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Category key",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    leadingIcon = selectedCatObj?.let { cat ->
                                        {
                                            Icon(
                                                imageVector = IconMapper.getIconByName(cat.iconName),
                                                contentDescription = cat.name,
                                                tint = if (!isAllCategory) Color.Unspecified else FormatHelper.parseColor(cat.colorHex),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("filter_category_select_selector"),
                                    shape = RoundedCornerShape(8.dp)
                                )
                
                                DropdownMenu(
                                    expanded = categoryDropdownExpanded,
                                    onDismissRequest = { categoryDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.55f)
                                ) {
                                    categoriesList.forEach { cat ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = IconMapper.getIconByName(cat.iconName),
                                                        contentDescription = cat.name,
                                                        tint = FormatHelper.parseColor(cat.colorHex),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(cat.name, fontSize = 13.sp)
                                                }
                                            },
                                            onClick = {
                                                viewModel.setCategoryFilter(cat.name)
                                                categoryDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 4. Time Filter Group
                        Text(
                            text = "Thời gian",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // All time
                            val isAll = activeTimeFilterMode == "ALL"
                            FilterChip(
                                selected = isAll,
                                onClick = { activeTimeFilterMode = "ALL" },
                                label = { Text("Mọi lúc", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "All time",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.testTag("time_filter_ALL"),
                                shape = RoundedCornerShape(8.dp)
                            )
            
                            // Day
                            val isDay = activeTimeFilterMode == "DAY"
                            val dayLabel = if (selectedCustomDate != null) {
                                SimpleDateFormat("dd/MM/yyyy", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build()).format(selectedCustomDate!!.time)
                            } else {
                                "Theo ngày"
                            }
                            FilterChip(
                                selected = isDay,
                                onClick = { showDatePicker() },
                                label = { Text(dayLabel, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Specific day",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.testTag("time_filter_DAY"),
                                shape = RoundedCornerShape(8.dp)
                            )
            
                            // Week
                            val isWeek = activeTimeFilterMode == "WEEK"
                            FilterChip(
                                selected = isWeek,
                                onClick = { activeTimeFilterMode = "WEEK" },
                                label = { Text("1 Tuần", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Last week",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.testTag("time_filter_WEEK"),
                                shape = RoundedCornerShape(8.dp)
                            )
            
                            // Range
                            val isRange = activeTimeFilterMode == "RANGE"
                            val rangeLabel = if (selectedRangeStart != null && selectedRangeEnd != null) {
                                val startStr = SimpleDateFormat("dd/MM", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build()).format(selectedRangeStart!!.time)
                                val endStr = SimpleDateFormat("dd/MM", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build()).format(selectedRangeEnd!!.time)
                                "$startStr - $endStr"
                            } else {
                                "Khoảng ngày"
                            }
                            FilterChip(
                                selected = isRange,
                                onClick = { showRangeDialog = true },
                                label = { Text(rangeLabel, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Date range",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.testTag("time_filter_RANGE"),
                                shape = RoundedCornerShape(8.dp)
                            )
            
                            // Month
                            val isMonth = activeTimeFilterMode == "MONTH"
                            val monthLabel = "Thg ${selectedCustomMonth + 1}/$selectedCustomMonthYear"
                            FilterChip(
                                selected = isMonth,
                                onClick = { showMonthDialog = true },
                                label = { Text(if (isMonth) monthLabel else "Theo tháng", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Specific month",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.testTag("time_filter_MONTH"),
                                shape = RoundedCornerShape(8.dp)
                            )
            
                            // Year
                            val isYear = activeTimeFilterMode == "YEAR"
                            val yearLabel = "Năm $selectedCustomYear"
                            FilterChip(
                                selected = isYear,
                                onClick = { showYearDialog = true },
                                label = { Text(if (isYear) yearLabel else "Theo năm", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Specific year",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.testTag("time_filter_YEAR"),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        
                        // Reset Button option if any filter is active
                        if (searchQuery.isNotEmpty() || selectedTypeFilter != "ALL" || selectedCategoryFilter != "ALL" || activeTimeFilterMode != "ALL") {
                            TextButton(
                                onClick = {
                                    viewModel.setSearchQuery("")
                                    viewModel.setTypeFilter("ALL")
                                    viewModel.setCategoryFilter("ALL")
                                    activeTimeFilterMode = "ALL"
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Xóa lọc", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bỏ toàn bộ lọc", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        }
        
        if (displayMode == "LIST") {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Transaction Timeline List with daily summaries
            if (groupedTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "No trans found",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Không tìm thấy giao dịch phù hợp!",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Thử thay đổi bộ lọc tìm kiếm",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("history_transactions_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedTransactions.forEach { (dateStr, txList) ->
                        stickyHeader {
                            val summary = dailySummaries[dateStr] ?: Pair(0.0, 0.0)
                            val totalIncome = summary.first
                            val totalExpense = summary.second

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dateStr,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { onNavigateToTimeline(dateStr) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timeline,
                                            contentDescription = "Timeline",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (totalExpense > 0.0) {
                                        Text(
                                            text = "-${FormatHelper.formatVND(totalExpense)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFF44336)
                                        )
                                    }
                                    if (totalIncome > 0.0) {
                                        Text(
                                            text = "+${FormatHelper.formatVND(totalIncome)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        items(txList, key = { it.id }) { tx ->
                            RemovableTransactionItem(
                                tx = tx,
                                onDelete = { transactionToDelete = tx },
                                onEdit = { editingTransaction = tx }
                            )
                        }
                    }
                }
            }
        } else {
            // --- CALENDAR MODE CONTENT ---
            // 1. Sleek Forest Green Navigation Header
            CalendarHeaderRow(
                selectedMonth = selectedCustomMonth,
                selectedYear = selectedCustomMonthYear,
                onMonthChange = { selectedCustomMonth = it },
                onYearChange = { selectedCustomMonthYear = it }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 2. 7-Column Grid View and Day Transactions Scrollable
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                CalendarGrid(
                    selectedMonth = selectedCustomMonth,
                    selectedYear = selectedCustomMonthYear,
                    transactionsByDayKey = transactionsByDayKey,
                    selectedCalendarDay = selectedCalendarDay,
                    onDayClick = { day ->
                        selectedCalendarYear = day.year
                        selectedCalendarMonth = day.month
                        selectedCalendarDayOfMonth = day.dayOfMonth
                    },
                    onDayLongPress = { day ->
                        selectedCalendarYear = day.year
                        selectedCalendarMonth = day.month
                        selectedCalendarDayOfMonth = day.dayOfMonth
                        showQuickActionMenuByDay = day
                    }
                )

                val dayVal = selectedCalendarDay
                val dayKey = "${dayVal.year}-${dayVal.month}-${dayVal.dayOfMonth}"
                val dayTxs = transactionsByDayKey[dayKey] ?: emptyList()
                DayTransactionsInline(
                    day = dayVal,
                    transactions = dayTxs,
                    onEditTransaction = { tx ->
                        editingTransaction = tx
                    },
                    onDeleteTransaction = { tx ->
                        transactionToDelete = tx
                    },
                    onNavigateToTimeline = onNavigateToTimeline
                )
            }
        }
    }

    if (editingTransaction != null) {
        EditTransactionDialog(
            tx = editingTransaction!!,
            categoriesList = categoriesList,
            walletsList = walletsList,
            onDismiss = { editingTransaction = null },
            onSave = { updatedTx ->
                viewModel.updateTransaction(updatedTx)
                editingTransaction = null
            }
        )
    }

    if (showQuickActionMenuByDay != null) {
        val dayVal = showQuickActionMenuByDay!!
        val dayKey = "${dayVal.year}-${dayVal.month}-${dayVal.dayOfMonth}"
        val dayTxs = transactionsByDayKey[dayKey] ?: emptyList()
        DayQuickActionsDialog(
            day = dayVal,
            hasTransactions = dayTxs.isNotEmpty(),
            onDismiss = { showQuickActionMenuByDay = null },
            onQuickAdd = {
                showQuickAddDialogByDay = dayVal
                showQuickActionMenuByDay = null
            },
            onManageDay = {
                showQuickActionMenuByDay = null
            }
        )
    }

    if (showQuickAddDialogByDay != null) {
        val dayVal = showQuickAddDialogByDay!!
        val cal = Calendar.getInstance().apply {
            set(dayVal.year, dayVal.month, dayVal.dayOfMonth)
        }
        QuickAddTransactionDialog(
            initialTimestamp = cal.timeInMillis,
            categoriesList = categoriesList,
            walletsList = walletsList,
            onDismiss = { showQuickAddDialogByDay = null },
            onSave = { walletId, type, amount, categoryName, categoryIcon, categoryColor, note, timestamp ->
                viewModel.addTransaction(
                    walletId = walletId,
                    type = type,
                    amount = amount,
                    categoryName = categoryName,
                    note = note,
                    timestamp = timestamp
                )
                showQuickAddDialogByDay = null
            }
        )
    }
}

@Composable
fun RemovableTransactionItem(
    tx: Transaction,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .clickable { onEdit() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icon Circle
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(FormatHelper.parseColor(tx.categoryColor)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = IconMapper.getIconByName(tx.categoryIcon),
                contentDescription = tx.categoryName,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // Details Column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.categoryName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
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
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (tx.isRecurring) {
                    Text(text = "•", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Định kỳ",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Money + Delete Panel
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (tx.type == "EXPENSE") "-${FormatHelper.formatVND(tx.amount)}"
                           else if (tx.type == "TRANSFER") "±${FormatHelper.formatVND(tx.amount)}"
                           else "+${FormatHelper.formatVND(tx.amount)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (tx.type == "EXPENSE") Color(0xFFF44336) else if (tx.type == "TRANSFER") Color(0xFF2196F3) else Color(0xFF4CAF50),
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = FormatHelper.formatTime(tx.timestamp),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(24.dp)
                    .testTag("delete_tx_btn_${tx.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EditTransactionDialog(
    tx: Transaction,
    categoriesList: List<FinanceCategory>,
    walletsList: List<Wallet>,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    val dismissWithAnimation = {
        scope.launch {
            isVisible = false
            delay(220)
            onDismiss()
        }
    }

    var amountText by remember { mutableStateOf(tx.amount.toLong().toString()) }
    var selectedType by remember { mutableStateOf(tx.type) } // EXPENSE, INCOME
    var selectedCategoryName by remember { mutableStateOf(tx.categoryName) }
    var selectedWalletId by remember { mutableStateOf<Int?>(tx.walletId) }
    var noteText by remember { mutableStateOf(tx.note) }
    var isRecurring by remember { mutableStateOf(tx.isRecurring) }
    var recurrencePeriod by remember { mutableStateOf(tx.recurrencePeriod.ifEmpty { "DAILY" }) }
    var selectedTimestamp by remember { mutableStateOf(tx.timestamp) }
    
    // Dropdowns visibility
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var walletDropdownExpanded by remember { mutableStateOf(false) }
    
    // Filter categories depending on selectedType
    val filteredCategories = remember(categoriesList, selectedType) {
        categoriesList.filter { it.type == selectedType || it.type == "BOTH" }
    }
    
    // Ensure the category chosen is kept valid
    LaunchedEffect(selectedType) {
        val hasCategory = filteredCategories.any { it.name == selectedCategoryName }
        if (!hasCategory && filteredCategories.isNotEmpty()) {
            selectedCategoryName = filteredCategories.first().name
        }
    }

    val dateTimeFormatter = remember { SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build()) }
    var dateLabel by remember(selectedTimestamp) {
        mutableStateOf(dateTimeFormatter.format(selectedTimestamp))
    }

    val showDateTimePicker = {
        val currentCal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        val timePicker = android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val finalCal = Calendar.getInstance().apply {
                    timeInMillis = selectedTimestamp
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                selectedTimestamp = finalCal.timeInMillis
            },
            currentCal.get(Calendar.HOUR_OF_DAY),
            currentCal.get(Calendar.MINUTE),
            true
        )

        val datePicker = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val partialCal = Calendar.getInstance().apply {
                    timeInMillis = selectedTimestamp
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedTimestamp = partialCal.timeInMillis
                timePicker.show()
            },
            currentCal.get(Calendar.YEAR),
            currentCal.get(Calendar.MONTH),
            currentCal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    Dialog(
        onDismissRequest = { dismissWithAnimation() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable { dismissWithAnimation() },
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f)
                        .clickable(enabled = false) {}, // Prevent clicks outside from propagating
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 54.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sửa Giao Dịch",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { dismissWithAnimation() }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Edit")
                            }
                        }

                        // Scrollable Content Column (takes up screen but keeps actions pinned)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. Transaction Type (Income / Expense Selector)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "EXPENSE" to "Khoản Chi",
                                    "INCOME" to "Khoản Thu"
                                ).forEach { (typeVal, label) ->
                                    val isSelected = selectedType == typeVal
                                    val targetBgColor = if (isSelected) {
                                        if (typeVal == "EXPENSE") Color(0xFFF44336) else Color(0xFF4CAF50)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                    val targetContentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                                    Button(
                                        onClick = { selectedType = typeVal },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = targetBgColor,
                                            contentColor = targetContentColor
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("edit_tx_type_$typeVal")
                                    ) {
                                        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // 2. Amount Input field
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Số tiền *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                OutlinedTextField(
                                    value = amountText,
                                    onValueChange = { amountText = it },
                                    placeholder = { Text("0") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("edit_tx_amount_input")
                                )
                                // Real-time currency preview helper
                                val expressionValue = FormatHelper.evaluateExpression(amountText)
                                if (expressionValue > 0.0) {
                                    Text(
                                        text = "= " + FormatHelper.formatVND(expressionValue),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selectedType == "EXPENSE") Color(0xFFF44336) else Color(0xFF4CAF50),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }

                            // 3. Note Field
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Ghi chú",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                OutlinedTextField(
                                    value = noteText,
                                    onValueChange = { noteText = it },
                                    placeholder = { Text("Mô tả chi tiết giao dịch...") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("edit_tx_note_input")
                                )
                            }

                            // 4. Category Selector Dropdown Button
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Danh mục *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    val currentCategoryObject = categoriesList.find { it.name == selectedCategoryName }
                                    
                                    OutlinedTextField(
                                        value = selectedCategoryName,
                                        onValueChange = {},
                                        readOnly = true,
                                        leadingIcon = currentCategoryObject?.let { cat ->
                                            {
                                                Icon(
                                                    imageVector = IconMapper.getIconByName(cat.iconName),
                                                    contentDescription = cat.name,
                                                    tint = FormatHelper.parseColor(cat.colorHex),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("edit_tx_category_selector")
                                    )
                                    // Bắt sự kiện click mượt mà phủ lên toàn bộ ô input
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color.Transparent)
                                            .clickable { categoryDropdownExpanded = !categoryDropdownExpanded }
                                    )

                                    DropdownMenu(
                                        expanded = categoryDropdownExpanded,
                                        onDismissRequest = { categoryDropdownExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        filteredCategories.forEach { cat ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = IconMapper.getIconByName(cat.iconName),
                                                            contentDescription = cat.name,
                                                            tint = FormatHelper.parseColor(cat.colorHex),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Text(cat.name, fontSize = 14.sp)
                                                    }
                                                },
                                                onClick = {
                                                    selectedCategoryName = cat.name
                                                    categoryDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 5. Wallet Dropdown Selector
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Chọn tài khoản / Ví *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    val currentWalletObject = walletsList.find { it.id == selectedWalletId }
                                    
                                    OutlinedTextField(
                                        value = currentWalletObject?.name ?: "Chưa chọn tài khoản",
                                        onValueChange = {},
                                        readOnly = true,
                                        leadingIcon = currentWalletObject?.let { w ->
                                            {
                                                Icon(
                                                    imageVector = IconMapper.getIconByName(w.iconName),
                                                    contentDescription = w.name,
                                                    tint = FormatHelper.parseColor(w.colorHex),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("edit_tx_wallet_selector")
                                    )
                                    // Bắt sự kiện click mượt mà phủ lên toàn bộ ô input
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color.Transparent)
                                            .clickable { walletDropdownExpanded = !walletDropdownExpanded }
                                    )

                                    DropdownMenu(
                                        expanded = walletDropdownExpanded,
                                        onDismissRequest = { walletDropdownExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        walletsList.forEach { w ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = IconMapper.getIconByName(w.iconName),
                                                            contentDescription = w.name,
                                                            tint = FormatHelper.parseColor(w.colorHex),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Text(w.name, fontSize = 14.sp)
                                                    }
                                                },
                                                onClick = {
                                                    selectedWalletId = w.id
                                                    walletDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 6. Time and Date Selector
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Thời gian giao dịch",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Button(
                                    onClick = showDateTimePicker,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("edit_tx_datetime_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Thời gian: $dateLabel", fontSize = 14.sp)
                                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Pick DateTime", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bottom Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = { dismissWithAnimation() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("HỦY BỎ")
                            }

                            val finalAmount = FormatHelper.evaluateExpression(amountText)
                            Button(
                                onClick = {
                                    val targetCat = categoriesList.find { it.name == selectedCategoryName } ?: categoriesList.first()
                                    val targetWallet = walletsList.find { it.id == selectedWalletId } ?: walletsList.first()
                                    
                                    val updatedTx = tx.copy(
                                        walletId = targetWallet.id,
                                        walletName = targetWallet.name,
                                        type = selectedType,
                                        amount = finalAmount,
                                        categoryName = targetCat.name,
                                        categoryIcon = targetCat.iconName,
                                        categoryColor = targetCat.colorHex,
                                        note = noteText.ifEmpty { targetCat.name },
                                        timestamp = selectedTimestamp,
                                        isRecurring = isRecurring,
                                        recurrencePeriod = if (isRecurring) recurrencePeriod else "NONE"
                                    )
                                    onSave(updatedTx)
                                },
                                enabled = finalAmount > 0.0 && selectedWalletId != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .testTag("edit_tx_save_btn")
                            ) {
                                Text("Thay đổi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CalendarDay(
    val year: Int,
    val month: Int, // 0-based
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean
)

private fun getCalendarGridDays(year: Int, monthIdx: Int): List<CalendarDay> {
    val days = mutableListOf<CalendarDay>()
    
    // Calendar for first day of current month
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthIdx)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=SUNDAY, 2=MONDAY, ...
    val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
    
    // Backfill previous month days
    val prevMonthCal = cal.clone() as Calendar
    prevMonthCal.add(Calendar.MONTH, -1)
    val maxDaysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val prevMonthYear = prevMonthCal.get(Calendar.YEAR)
    val prevMonthIdx = prevMonthCal.get(Calendar.MONTH)
    
    for (i in offset - 1 downTo 0) {
        val dayNo = maxDaysInPrevMonth - i
        days.add(CalendarDay(prevMonthYear, prevMonthIdx, dayNo, isCurrentMonth = false))
    }
    
    // Current month days
    val maxDaysInCurrentMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (dayNo in 1..maxDaysInCurrentMonth) {
        days.add(CalendarDay(year, monthIdx, dayNo, isCurrentMonth = true))
    }
    
    // Next month days to fill up to a multiple of 7
    val totalCells = if (days.size <= 35) 35 else 42
    val nextMonthCal = cal.clone() as Calendar
    nextMonthCal.add(Calendar.MONTH, 1)
    val nextMonthYear = nextMonthCal.get(Calendar.YEAR)
    val nextMonthIdx = nextMonthCal.get(Calendar.MONTH)
    
    var nextMonthDayNo = 1
    while (days.size < totalCells) {
        days.add(CalendarDay(nextMonthYear, nextMonthIdx, nextMonthDayNo, isCurrentMonth = false))
        nextMonthDayNo++
    }
    
    return days
}

@Composable
fun CalendarHeaderRow(
    selectedMonth: Int,
    selectedYear: Int,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2E7D32)) // Beautiful Fintech Green
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Prev / Month Title / Next
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    if (selectedMonth == 0) {
                        onMonthChange(11)
                        onYearChange(selectedYear - 1)
                    } else {
                        onMonthChange(selectedMonth - 1)
                    }
                },
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.25f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Tháng trước",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            val monthStr = "THÁNG %02d - %d".format(selectedMonth + 1, selectedYear)
            Text(
                text = monthStr,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            IconButton(
                onClick = {
                    if (selectedMonth == 11) {
                        onMonthChange(0)
                        onYearChange(selectedYear + 1)
                    } else {
                        onMonthChange(selectedMonth + 1)
                    }
                },
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.25f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Tháng sau",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Quick Month/Year Dropdown Selectors
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            var monthDropdownShow by remember { mutableStateOf(false) }
            var yearDropdownShow by remember { mutableStateOf(false) }

            // Month Selector
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .clickable { monthDropdownShow = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${selectedMonth + 1}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                }
                DropdownMenu(
                    expanded = monthDropdownShow,
                    onDismissRequest = { monthDropdownShow = false }
                ) {
                    (0..11).forEach { mIdx ->
                        DropdownMenuItem(
                            text = { Text("${mIdx + 1}", fontSize = 12.sp) },
                            onClick = {
                                onMonthChange(mIdx)
                                monthDropdownShow = false
                            }
                        )
                    }
                }
            }

            // Year Selector
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .clickable { yearDropdownShow = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = selectedYear.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                }
                DropdownMenu(
                    expanded = yearDropdownShow,
                    onDismissRequest = { yearDropdownShow = false }
                ) {
                    val currYear = Calendar.getInstance().get(Calendar.YEAR)
                    (currYear - 4..currYear + 2).forEach { yr ->
                        DropdownMenuItem(
                            text = { Text(yr.toString(), fontSize = 12.sp) },
                            onClick = {
                                onYearChange(yr)
                                yearDropdownShow = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(
    selectedMonth: Int,
    selectedYear: Int,
    transactionsByDayKey: Map<String, List<Transaction>>,
    selectedCalendarDay: CalendarDay?,
    onDayClick: (CalendarDay) -> Unit,
    onDayLongPress: (CalendarDay) -> Unit
) {
    val todayCal = remember { Calendar.getInstance() }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Week Header (Thứ hai, ... Chủ nhật as shown in the reference image)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            val weekdays = listOf("T.Hai", "T.Ba", "T.Tư", "T.Năm", "T.Sáu", "T.Bảy", "C.Nhật")
            weekdays.forEach { name ->
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF51AE5A)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(bottom = 4.dp))

        // Grid Cells
        val gridDays = remember(selectedMonth, selectedYear) {
            getCalendarGridDays(selectedYear, selectedMonth)
        }
        val rows = remember(gridDays) {
            gridDays.chunked(7)
        }

        rows.forEach { rowDays ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowDays.forEach { day ->
                    val dayKey = "${day.year}-${day.month}-${day.dayOfMonth}"
                    val dayTxs = transactionsByDayKey[dayKey] ?: emptyList()

                    val isToday = day.year == todayYear && day.month == todayMonth && day.dayOfMonth == todayDay
                    
                    val isFuture = when {
                        day.year > todayYear -> true
                        day.year < todayYear -> false
                        else -> {
                            if (day.month > todayMonth) true
                            else if (day.month < todayMonth) false
                            else day.dayOfMonth > todayDay
                        }
                    }

                    val isSelected = selectedCalendarDay?.run {
                        this.year == day.year && this.month == day.month && this.dayOfMonth == day.dayOfMonth
                    } == true

                    CalendarCell(
                        day = day,
                        isToday = isToday,
                        isSelected = isSelected,
                        isFuture = isFuture,
                        transactions = dayTxs,
                        onClick = { onDayClick(day) },
                        onLongPress = { onDayLongPress(day) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarCell(
    day: CalendarDay,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    transactions: List<Transaction>,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalIncome = remember(transactions) {
        transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    
    val hasTransactions = transactions.isNotEmpty()
    
    val cellBgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        hasTransactions -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    
    val cellBorderModifier = when {
        isSelected -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
        isToday -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
        hasTransactions -> Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        else -> Modifier
    }

    val contentAlpha = if (day.isCurrentMonth) {
        if (isFuture) 0.5f else 1.0f
    } else {
        0.35f
    }

    Box(
        modifier = modifier
            .aspectRatio(0.65f)
            .padding(1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(cellBgColor)
            .then(cellBorderModifier)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Day Number Label
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 2.dp, top = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = day.dayOfMonth.toString(),
                    fontSize = 11.sp,
                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    }
                )
            }
            
            // Amounts Container
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (totalExpense > 0.0) {
                    Text(
                        text = "-${formatCompactVND(totalExpense)}",
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336).copy(alpha = contentAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (totalIncome > 0.0) {
                    Text(
                        text = "+${formatCompactVND(totalIncome)}",
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50).copy(alpha = contentAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Keep layout spacing consistent
                if (totalIncome == 0.0 && totalExpense == 0.0) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun DayTransactionsInline(
    day: CalendarDay,
    transactions: List<Transaction>,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onNavigateToTimeline: (String) -> Unit = {}
) {
    val dateLabel = "%02d/%02d/%d".format(day.dayOfMonth, day.month + 1, day.year)
    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header of selected day
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Giao dịch ngày $dateLabel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (transactions.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.IconButton(
                            onClick = { onNavigateToTimeline(dateLabel) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Timeline,
                                contentDescription = "Timeline",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (totalExpense > 0.0) {
                        Text(
                            text = "-${FormatHelper.formatVND(totalExpense)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF44336)
                        )
                    }
                    if (totalIncome > 0.0) {
                        Text(
                            text = "+${FormatHelper.formatVND(totalIncome)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Không có giao dịch nào trong ngày này.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                transactions.forEach { tx ->
                    RemovableTransactionItem(
                        tx = tx,
                        onDelete = { onDeleteTransaction(tx) },
                        onEdit = { onEditTransaction(tx) }
                    )
                }
            }
        }
    }
}

@Composable
fun DayQuickActionsDialog(
    day: CalendarDay,
    hasTransactions: Boolean,
    onDismiss: () -> Unit,
    onQuickAdd: () -> Unit,
    onManageDay: () -> Unit
) {
    val dateLabel = "%02d/%02d/%d".format(day.dayOfMonth, day.month + 1, day.year)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Thao tác ngày $dateLabel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Action: Quick Add
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onQuickAdd() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Thêm Giao Dịch Nhanh", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Tạo nhanh một giao dịch cho ngày này", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Action: Manage Day (only if hasTransactions)
                if (hasTransactions) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onManageDay() }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Xem chi tiết các giao dịch", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Xóa, sửa giao dịch của ngày $dateLabel", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ĐÓNG")
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAddTransactionDialog(
    initialTimestamp: Long,
    categoriesList: List<FinanceCategory>,
    walletsList: List<Wallet>,
    onDismiss: () -> Unit,
    onSave: (walletId: Int, type: String, amount: Double, categoryName: String, categoryIcon: String, categoryColor: String, note: String, timestamp: Long) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME
    var selectedCategoryName by remember { mutableStateOf(categoriesList.firstOrNull { it.type == "EXPENSE" || it.type == "BOTH" }?.name ?: "") }
    var selectedWalletId by remember { mutableStateOf<Int?>(walletsList.firstOrNull()?.id) }
    var noteText by remember { mutableStateOf("") }
    
    // Dropdowns visibility
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var walletDropdownExpanded by remember { mutableStateOf(false) }
    
    // Filter categories depending on selectedType
    val filteredCategories = remember(categoriesList, selectedType) {
        categoriesList.filter { it.type == selectedType || it.type == "BOTH" }
    }
    
    // Ensure the category chosen is kept valid
    LaunchedEffect(selectedType) {
        val hasCategory = filteredCategories.any { it.name == selectedCategoryName }
        if (!hasCategory && filteredCategories.isNotEmpty()) {
            selectedCategoryName = filteredCategories.first().name
        }
    }

    val dateTimeFormatter = remember { SimpleDateFormat("dd/MM/yyyy", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build()) }
    val dateLabel = dateTimeFormatter.format(initialTimestamp)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Thêm Giao Dịch Nhanh",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ngày: $dateLabel",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Add")
                    }
                }

                // 1. Transaction Type (Income / Expense Selector)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "EXPENSE" to "Khoản Chi",
                        "INCOME" to "Khoản Thu"
                    ).forEach { (typeVal, label) ->
                        val isSelected = selectedType == typeVal
                        val targetBgColor = if (isSelected) {
                            if (typeVal == "EXPENSE") Color(0xFFF44336) else Color(0xFF4CAF50)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                        val targetContentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                        Button(
                            onClick = { selectedType = typeVal },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = targetBgColor,
                                contentColor = targetContentColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // 2. Amount Input field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Số tiền *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    val expressionValue = FormatHelper.evaluateExpression(amountText)
                    if (expressionValue > 0.0) {
                        Text(
                            text = "= " + FormatHelper.formatVND(expressionValue),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedType == "EXPENSE") Color(0xFFF44336) else Color(0xFF4CAF50),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                // 3. Note Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Ghi chú",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text("Mô tả chi tiết giao dịch...") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 4. Category Selector Dropdown Button
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Danh mục *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentCategoryObject = categoriesList.find { it.name == selectedCategoryName }
                        
                        OutlinedTextField(
                            value = selectedCategoryName,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = currentCategoryObject?.let { cat ->
                                {
                                    Icon(
                                        imageVector = IconMapper.getIconByName(cat.iconName),
                                        contentDescription = cat.name,
                                        tint = FormatHelper.parseColor(cat.colorHex),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            trailingIcon = {
                                IconButton(onClick = { categoryDropdownExpanded = !categoryDropdownExpanded }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { categoryDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            filteredCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = IconMapper.getIconByName(cat.iconName),
                                                contentDescription = cat.name,
                                                tint = FormatHelper.parseColor(cat.colorHex),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(cat.name, fontSize = 14.sp)
                                        }
                                    },
                                    onClick = {
                                        selectedCategoryName = cat.name
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 5. Wallet Dropdown Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Chọn tài khoản / Ví *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentWalletObject = walletsList.find { it.id == selectedWalletId }
                        
                        OutlinedTextField(
                            value = currentWalletObject?.name ?: "Chưa chọn tài khoản",
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = currentWalletObject?.let { w ->
                                {
                                    Icon(
                                        imageVector = IconMapper.getIconByName(w.iconName),
                                        contentDescription = w.name,
                                        tint = FormatHelper.parseColor(w.colorHex),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            trailingIcon = {
                                IconButton(onClick = { walletDropdownExpanded = !walletDropdownExpanded }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { walletDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = walletDropdownExpanded,
                            onDismissRequest = { walletDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            walletsList.forEach { w ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = IconMapper.getIconByName(w.iconName),
                                                contentDescription = w.name,
                                                tint = FormatHelper.parseColor(w.colorHex),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(w.name, fontSize = 14.sp)
                                        }
                                    },
                                    onClick = {
                                        selectedWalletId = w.id
                                        walletDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("HỦY BỎ")
                    }

                    val finalAmount = FormatHelper.evaluateExpression(amountText)
                    Button(
                        onClick = {
                            val targetCat = categoriesList.find { it.name == selectedCategoryName } ?: categoriesList.first()
                            onSave(
                                selectedWalletId ?: walletsList.first().id,
                                selectedType,
                                finalAmount,
                                targetCat.name,
                                targetCat.iconName,
                                targetCat.colorHex,
                                noteText.ifEmpty { targetCat.name },
                                initialTimestamp
                            )
                        },
                        enabled = finalAmount > 0.0 && selectedWalletId != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                    ) {
                        Text("THÊM", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatCompactVND(amount: Double): String {
    if (amount <= 0.0) return ""
    return when {
        amount >= 1_000_000_000 -> {
            val bnVal = amount / 1_000_000_000.0
            val formatted = if (bnVal % 1.0 == 0.0) "%.0f".format(Locale.US, bnVal) else "%.1f".format(Locale.US, bnVal)
            "${formatted}B"
        }
        amount >= 1_000_000 -> {
            val mVal = amount / 1_000_000.0
            val formatted = if (mVal % 1.0 == 0.0) "%.0f".format(Locale.US, mVal) else "%.1f".format(Locale.US, mVal)
            "${formatted}tr"
        }
        amount >= 1_000 -> {
            val kVal = amount / 1_000.0
            val formatted = if (kVal % 1.0 == 0.0) "%.0f".format(Locale.US, kVal) else "%.1f".format(Locale.US, kVal)
            "${formatted}k"
        }
        else -> "%.0f".format(Locale.US, amount)
    }
}
