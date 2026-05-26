package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Categories
import com.example.data.FinanceCategory
import com.example.ui.FinanceViewModel
import com.example.ui.FormatHelper
import com.example.ui.IconMapper
import java.util.Calendar
import kotlinx.coroutines.launch

data class SmartCategorySuggestion(
    val category: FinanceCategory,
    val score: Double,
    val reason: String
)

@Composable
fun AddTransactionScreen(
    viewModel: FinanceViewModel,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val wallets by viewModel.dailyWallets.collectAsState()
    val categoriesList by viewModel.categoriesList.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    var rawExpression by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME
    var selectedWalletId by remember { mutableStateOf<Int?>(null) }
    var selectedCategoryName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    // Recurring Setup
    var isRecurring by remember { mutableStateOf(false) }
    var recurrencePeriod by remember { mutableStateOf("DAILY") } // DAILY, WEEKLY, MONTHLY

    // Date Picker Setup
    val calendar = remember { Calendar.getInstance() }
    var selectedTimestamp by remember { mutableStateOf(calendar.timeInMillis) }
    var dateLabel by remember { mutableStateOf("Hôm nay") }

    // Smart Select State Management
    var hasManuallySelected by remember { mutableStateOf(false) }

    LaunchedEffect(selectedType) {
        hasManuallySelected = false
    }

    // Auto-select first wallet if available
    LaunchedEffect(wallets) {
        if (selectedWalletId == null && wallets.isNotEmpty()) {
            selectedWalletId = wallets.first().id
        }
    }

    // Filter categories depending on type
    val filteredCategories = remember(categoriesList, selectedType) {
        categoriesList.filter { it.type == selectedType || it.type == "BOTH" }
    }

    val currentAmount = remember(rawExpression) {
        FormatHelper.evaluateExpression(rawExpression)
    }

    // Compute intelligent category suggestions in real-time
    val smartSuggestions = remember(
        currentAmount,
        note,
        selectedType,
        selectedWalletId,
        selectedTimestamp,
        allTransactions,
        filteredCategories
    ) {
        if (currentAmount <= 0.0) return@remember emptyList<SmartCategorySuggestion>()
        if (allTransactions.isEmpty() || filteredCategories.isEmpty()) return@remember emptyList<SmartCategorySuggestion>()

        val typeTxs = allTransactions.filter { it.type == selectedType }
        if (typeTxs.isEmpty()) return@remember emptyList<SmartCategorySuggestion>()

        val latestTxCategory = typeTxs.maxByOrNull { it.timestamp }?.categoryName
        val frequencyMap = typeTxs.groupBy { it.categoryName }.mapValues { it.value.size }

        // Group similar amounts of the same type
        val exactAmountMatches = if (currentAmount > 0.0) {
            typeTxs.filter { it.amount == currentAmount }
        } else {
            emptyList()
        }

        filteredCategories.mapNotNull { cat ->
            val cName = cat.name
            var categoryScore = 0.0
            val reasonsList = mutableListOf<String>()

            // 1. Base Frequency Usage bias (Max 15)
            val occurrences = frequencyMap.getOrDefault(cName, 0)
            if (occurrences > 0) {
                categoryScore += Math.min(15.0, occurrences * 1.5)
            }

            // 2. Most recent transaction bias (10 pts)
            if (cName == latestTxCategory) {
                categoryScore += 10.0
            }

            // 3. Amount consistency bonus (from previous similar amounts)
            if (exactAmountMatches.isNotEmpty()) {
                val amountMatchesForCat = exactAmountMatches.filter { it.categoryName == cName }.size
                val amountRatio = amountMatchesForCat.toDouble() / exactAmountMatches.size
                if (amountMatchesForCat >= 1) {
                    if (amountRatio >= 0.7 && exactAmountMatches.size >= 2) {
                        categoryScore += 45.0
                        reasonsList.add("Thường chi mức này")
                    } else {
                        categoryScore += 15.0
                        reasonsList.add("Có chi mức này")
                    }
                }
            }

            // 4. Historical comparison logic
            val txsToCheck = typeTxs.take(150)
            var historicalMaxForCat = 0.0
            var bestReasonForCat = ""

            for (tx in txsToCheck) {
                if (tx.categoryName != cName) continue

                var txRawScore = 0.0
                val currentTxReasons = mutableListOf<String>()

                // Match note
                if (note.isNotBlank() && tx.note.isNotBlank()) {
                    val curNoteNorm = note.trim().lowercase()
                    val txNoteNorm = tx.note.trim().lowercase()
                    
                    if (curNoteNorm == txNoteNorm) {
                        txRawScore += 70.0
                        currentTxReasons.add("Ghi chú y hệt")
                    } else if (curNoteNorm.contains(txNoteNorm) || txNoteNorm.contains(curNoteNorm)) {
                        txRawScore += 45.0
                        currentTxReasons.add("Ghi chú tương đồng")
                    }
                }

                // Match exact or close amounts
                if (currentAmount > 0.0) {
                    if (currentAmount == tx.amount) {
                         txRawScore += 20.0
                         currentTxReasons.add("Số tiền giống")
                    } else {
                        val diff = Math.abs(currentAmount - tx.amount)
                        val maxAmt = Math.max(currentAmount, tx.amount)
                        if (maxAmt > 0 && diff / maxAmt <= 0.1) {
                            txRawScore += 15.0
                            currentTxReasons.add("Số tiền tương tự")
                        } else if (maxAmt > 0 && diff / maxAmt <= 0.25) {
                            txRawScore += 10.0
                            currentTxReasons.add("Số tiền hơi giống")
                        }
                    }
                }

                // Match hours
                val calCurrent = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                val calTx = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                val currHour = calCurrent.get(Calendar.HOUR_OF_DAY)
                val txHour = calTx.get(Calendar.HOUR_OF_DAY)
                val hourDiff = Math.abs(currHour - txHour)
                if (hourDiff == 0 || hourDiff == 23) {
                    txRawScore += 10.0
                    currentTxReasons.add("Cùng khung giờ")
                } else if (hourDiff <= 2 || hourDiff >= 22) {
                    txRawScore += 7.0
                    currentTxReasons.add("Gần khung giờ")
                }

                // Match wallet
                if (selectedWalletId != null && tx.walletId == selectedWalletId) {
                    txRawScore += 5.0
                    currentTxReasons.add("Cùng tài khoản")
                }

                // Recency preference
                val daysAgo = (selectedTimestamp - tx.timestamp) / (24L * 60L * 60L * 1000L).toDouble()
                val recencyWeight = 1.0 / (1.0 + Math.max(0.0, daysAgo) / 30.0)

                val weightedScore = txRawScore * recencyWeight
                if (weightedScore > historicalMaxForCat) {
                    historicalMaxForCat = weightedScore
                    bestReasonForCat = currentTxReasons.joinToString(", ")
                }
            }

            categoryScore += historicalMaxForCat
            
            if (categoryScore > 10.0) {
                val displayReason = if (bestReasonForCat.isNotEmpty()) {
                    bestReasonForCat
                } else if (reasonsList.isNotEmpty()) {
                    reasonsList.joinToString(", ")
                } else {
                    "Danh mục quen thuộc"
                }
                SmartCategorySuggestion(
                    category = cat,
                    score = Math.min(100.0, categoryScore),
                    reason = displayReason
                )
            } else {
                null
            }
        }.sortedByDescending { it.score }.take(3)
    }

    // High confidence trigger for auto-selecting category
    LaunchedEffect(smartSuggestions) {
        if (!hasManuallySelected && smartSuggestions.isNotEmpty()) {
            val topSuggest = smartSuggestions.first()
            if (topSuggest.score >= 65.0) {
                if (selectedCategoryName != topSuggest.category.name) {
                    selectedCategoryName = topSuggest.category.name
                }
            }
        }
    }

    val categoryUsageCounts = remember(allTransactions) {
        val counts = mutableMapOf<String, Int>()
        allTransactions.forEach { tx ->
            counts[tx.categoryName] = counts.getOrDefault(tx.categoryName, 0) + 1
        }
        counts
    }

    val parentCategories = remember(filteredCategories, categoryUsageCounts) {
        val parents = filteredCategories.filter { it.parentName == null }
        parents.sortedByDescending { parent ->
            val subCategoryNames = filteredCategories.filter { it.parentName == parent.name }.map { it.name }
            categoryUsageCounts.getOrDefault(parent.name, 0) + subCategoryNames.sumOf { categoryUsageCounts.getOrDefault(it, 0) }
        }
    }

    LaunchedEffect(parentCategories) {
        if (parentCategories.isNotEmpty() && selectedCategoryName.isBlank()) {
            selectedCategoryName = parentCategories.first().name
        } else if (parentCategories.isNotEmpty() && filteredCategories.none { it.name == selectedCategoryName }) {
            selectedCategoryName = parentCategories.first().name
        }
    }

    val dateTimeFormatter = remember { SimpleDateFormat("HH:mm dd/MM/yyyy", Locale("vi", "VN")) }

    val showDateTimePicker = {
        val currentCal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        val timePicker = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val finalCal = Calendar.getInstance().apply {
                    timeInMillis = selectedTimestamp
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                selectedTimestamp = finalCal.timeInMillis
                dateLabel = dateTimeFormatter.format(finalCal.timeInMillis)
            },
            currentCal.get(Calendar.HOUR_OF_DAY),
            currentCal.get(Calendar.MINUTE),
            true // 24-hour style
        )

        val datePicker = DatePickerDialog(
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form Title
        Text(
            text = "Thêm Giao Dịch Mới",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // 1. Loại tiền (Switch EXPENSE vs INCOME)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { selectedType = "EXPENSE" },
                modifier = Modifier.weight(1f).testTag("select_expense_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == "EXPENSE") Color(0xFFF44336)
                                     else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selectedType == "EXPENSE") Color.White
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Chi")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Khoản Chi")
            }

            Button(
                onClick = { selectedType = "INCOME" },
                modifier = Modifier.weight(1f).testTag("select_income_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == "INCOME") Color(0xFF4CAF50)
                                     else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selectedType == "INCOME") Color.White
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Thu")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Khoản Thu")
            }
        }

        // 7. Số tiền
        com.example.ui.components.CustomMoneyInputField(
            value = rawExpression,
            onValueChange = { rawExpression = it },
            label = "Số tiền phát sinh",
            autoFocus = true,
            onDismissKeyboard = {
                scope.launch {
                    scrollState.animateScrollTo(0)
                }
            },
            testTag = "tx_amount_text_field"
        )

        // 2. Tài khoản thanh toán (bố cục 2x2 đẹp mắt)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Tài khoản thanh toán",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (wallets.isEmpty()) {
                Text(
                    text = "Không tìm thấy ví khả dụng. Chọn mục Tài khoản để tạo mới.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            } else {
                val chunkedWallets = wallets.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunkedWallets.forEach { rowWallets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowWallets.forEach { wt ->
                                val isSelected = selectedWalletId == wt.id
                                val accentColor = FormatHelper.parseColor(wt.colorHex)
                                val cardColor = if (isSelected) accentColor.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                val borderColor = if (isSelected) accentColor
                                                  else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedWalletId = wt.id }
                                        .testTag("tx_wallet_chip_${wt.id}"),
                                    colors = CardDefaults.cardColors(containerColor = cardColor),
                                    border = BorderStroke(if (isSelected) 1.8.dp else 1.dp, borderColor)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(accentColor.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = IconMapper.getIconByName(wt.iconName),
                                                contentDescription = wt.name,
                                                tint = accentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = wt.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = FormatHelper.formatVND(wt.balance),
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = accentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowWallets.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 3. Hạng mục dạng lưới (các mục bé hơn, 4 cột)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Hạng mục giao dịch",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Dynamic horizontal view containing top 3 smart suggestions
            if (allTransactions.isNotEmpty() && smartSuggestions.isNotEmpty()) {
                AnimatedVisibility(
                    visible = smartSuggestions.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Gợi ý thông minh",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Gợi ý nhanh (tối đa 3):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            smartSuggestions.forEach { suggestion ->
                                val cat = suggestion.category
                                val isSelected = selectedCategoryName == cat.name
                                val isAutoSelected = !hasManuallySelected && suggestion.score >= 65.0 && isSelected
                                val accentColor = try { FormatHelper.parseColor(cat.colorHex) } catch (e: Exception) { Color.Gray }

                                val chipBg = if (isSelected) {
                                    accentColor.copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }

                                val chipBorder = if (isAutoSelected) {
                                    BorderStroke(1.2.dp, if (selectedType == "EXPENSE") Color(0xFFF44336) else Color(0xFF4CAF50))
                                } else if (isSelected) {
                                    BorderStroke(1.2.dp, accentColor)
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedCategoryName = cat.name
                                            hasManuallySelected = true
                                        }
                                        .testTag("smart_suggest_${cat.name}"),
                                    color = chipBg,
                                    shape = RoundedCornerShape(12.dp),
                                    border = chipBorder
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = IconMapper.getIconByName(cat.iconName),
                                            contentDescription = cat.name,
                                            tint = accentColor,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = cat.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isAutoSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (selectedType == "EXPENSE") Color(0xFFF44336).copy(alpha = 0.12f)
                                                        else Color(0xFF4CAF50).copy(alpha = 0.12f),
                                                        RoundedCornerShape(3.dp)
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Tự động chọn",
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (selectedType == "EXPENSE") Color(0xFFF44336) else Color(0xFF4CAF50)
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "${suggestion.score.toInt()}%",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val chunkedCategories = parentCategories.chunked(4)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val currentSelectedCategory = filteredCategories.firstOrNull { it.name == selectedCategoryName }
                val activeParentName = currentSelectedCategory?.parentName ?: currentSelectedCategory?.name

                chunkedCategories.forEach { rowCats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCats.forEach { cat ->
                            val isSelected = activeParentName == cat.name
                            val categoryColor = try { FormatHelper.parseColor(cat.colorHex) } catch(e: Exception) { Color.Gray }
                            
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCategoryName = cat.name // select this parent category. SubCats will show below.
                                            hasManuallySelected = true
                                        }
                                        .padding(vertical = 4.dp)
                                        .testTag("category_select_${cat.name}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) categoryColor
                                                else categoryColor.copy(alpha = 0.12f)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else categoryColor.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = IconMapper.getIconByName(cat.iconName),
                                            contentDescription = cat.name,
                                            tint = if (isSelected) Color.White else categoryColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = cat.name,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (rowCats.size < 4) {
                            for (i in 0 until (4 - rowCats.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Render subcategories for the active parent if it is in this row
                    val activeCatInRow = rowCats.firstOrNull { it.name == activeParentName }
                    if (activeCatInRow != null) {
                        val subCats = filteredCategories.filter { it.parentName == activeCatInRow.name }
                        if (subCats.isNotEmpty()) {
                            val activeColor = try { FormatHelper.parseColor(activeCatInRow.colorHex) } catch(e: Exception) { Color.Gray }
                            val parentIndex = rowCats.indexOf(activeCatInRow)
                            
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(16.dp)
                                ) {
                                    val canvasWidth = size.width
                                    val canvasHeight = size.height
                                    val strokePx = 2.dp.toPx()
                                    
                                    val gapWidth = 8.dp.toPx()
                                    val totalGapsWidth = gapWidth * 3
                                    val itemWidth = (canvasWidth - totalGapsWidth) / 4f
                                    val arrowCenterX = parentIndex * (itemWidth + gapWidth) + itemWidth / 2f
                                    
                                    val arrowWidth = 16.dp.toPx()
                                    val arrowHeight = 8.dp.toPx()
                                    
                                    val baseY = canvasHeight - strokePx / 2f
                                    
                                    val path = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(0f, baseY)
                                        lineTo(arrowCenterX - arrowWidth / 2f, baseY)
                                        lineTo(arrowCenterX, baseY - arrowHeight)
                                        lineTo(arrowCenterX + arrowWidth / 2f, baseY)
                                        lineTo(canvasWidth, baseY)
                                    }
                                    
                                    drawPath(
                                        path = path,
                                        color = activeColor,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = strokePx,
                                            join = androidx.compose.ui.graphics.StrokeJoin.Round,
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    )
                                }
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            activeColor.copy(alpha = 0.08f), 
                                            RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                                        )
                                        .padding(vertical = 12.dp, horizontal = 8.dp)
                                ) {
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        item {
                                            val isSubSelected = selectedCategoryName == activeCatInRow.name
                                            FilterChip(
                                                selected = isSubSelected,
                                                onClick = {
                                                    selectedCategoryName = activeCatInRow.name
                                                    hasManuallySelected = true
                                                },
                                                label = { Text("Chung", fontSize = 12.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = activeColor,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                        items(subCats.size) { idx ->
                                            val sub = subCats[idx]
                                            val isSubSelected = selectedCategoryName == sub.name
                                            FilterChip(
                                                selected = isSubSelected,
                                                onClick = {
                                                    selectedCategoryName = sub.name
                                                    hasManuallySelected = true
                                                },
                                                label = { Text(sub.name, fontSize = 12.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = activeColor,
                                                    selectedLabelColor = Color.White
                                                ),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = IconMapper.getIconByName(sub.iconName),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = if (isSubSelected) Color.White else activeColor
                                                    )
                                                }
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

        // 4 & 6. Ghi chú hóa đơn/mô tả
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Ghi chú / Mô tả giao dịch") },
            leadingIcon = { Icon(imageVector = Icons.Default.EditNote, contentDescription = "Note") },
            modifier = Modifier.fillMaxWidth().testTag("tx_note_input"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        // 5. Thời gian (Có chức năng lấy thời gian nhanh, format HH:mm dd/MM/yyyy)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Thời gian phát sinh",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dateTimeFormatter.format(selectedTimestamp),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ngày & Giờ (HH:mm dd/MM/yyyy)") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Schedule, contentDescription = "Time") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDateTimePicker() }
                )
            }

            // Chức năng lấy thời gian nhanh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Bây giờ" Quick choice
                FilterChip(
                    selected = false,
                    onClick = {
                        selectedTimestamp = System.currentTimeMillis()
                        dateLabel = "Bây giờ"
                    },
                    label = { Text("Bây giờ ⚡") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                )

                // Today at 08:00
                FilterChip(
                    selected = false,
                    onClick = {
                        selectedTimestamp = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 8)
                            set(Calendar.MINUTE, 0)
                        }.timeInMillis
                        dateLabel = "Hôm nay"
                    },
                    label = { Text("08:00 Sáng") }
                )

                // Yesterday
                FilterChip(
                    selected = false,
                    onClick = {
                        selectedTimestamp = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -1)
                        }.timeInMillis
                        dateLabel = "Hôm qua"
                    },
                    label = { Text("Hôm qua") }
                )
            }
        }

        // --- Recurring Transaction Setup Section ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Repeat, contentDescription = "Recurring", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Thiết lập giao dịch định kỳ", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "Tự động sao chép phát sinh trong tương lai", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it },
                    modifier = Modifier.testTag("tx_recurring_switch")
                )
            }

            if (isRecurring) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "DAILY" to "Hàng ngày",
                        "WEEKLY" to "Hàng tuần",
                        "MONTHLY" to "Hàng tháng"
                    ).forEach { (freq, name) ->
                        FilterChip(
                            selected = recurrencePeriod == freq,
                            onClick = { recurrencePeriod = freq },
                            label = { Text(name) },
                            modifier = Modifier.testTag("tx_recurrence_$freq")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 8. Lưu
        Button(
            onClick = {
                val amount = FormatHelper.evaluateExpression(rawExpression)
                val walletId = selectedWalletId
                if (amount > 0 && walletId != null) {
                    viewModel.addTransaction(
                        walletId = walletId,
                        type = selectedType,
                        amount = amount,
                        categoryName = selectedCategoryName,
                        note = note,
                        timestamp = selectedTimestamp,
                        isRecurring = isRecurring,
                        recurrencePeriod = if (isRecurring) recurrencePeriod else "NONE"
                    )
                    android.widget.Toast.makeText(context, "Thêm giao dịch mới thành công!", android.widget.Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
            },
            enabled = FormatHelper.evaluateExpression(rawExpression) > 0.0 && selectedWalletId != null,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_transaction_btn"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedType == "EXPENSE") Color(0xFFF44336) else Color(0xFF4CAF50),
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = "LƯU GIAO DỊCH",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
