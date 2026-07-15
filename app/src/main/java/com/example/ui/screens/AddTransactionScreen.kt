package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
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

data class EditableScannedItem(
    val name: String,
    val amount: Double,
    val amountStr: String,
    val category: String
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

    var rawExpression by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME
    var selectedWalletId by remember { mutableStateOf<Int?>(null) }
    var selectedCategoryName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    
    val isAiScannerEnabled by viewModel.isAiScannerEnabled.collectAsState()

    // AI Receipt Scanner state
    var isScanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var showManualAiDialog by remember { mutableStateOf(false) }
    var isUsingAiState by remember { mutableStateOf(false) }
    var scanDurationSeconds by remember { mutableStateOf<Double?>(null) }

    var scannedItemsState by remember { mutableStateOf<List<EditableScannedItem>>(emptyList()) }
    var scannedDateTimeStr by remember { mutableStateOf("") }
    var scannedShopName by remember { mutableStateOf("") }
    var scannedTaxAmount by remember { mutableStateOf<Double?>(null) }
    var scannedDiscountAmount by remember { mutableStateOf<Double?>(null) }

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            scope.launch {
                isUsingAiState = true
                isScanning = true
                scanError = null
                scannedItemsState = emptyList()
                scannedDateTimeStr = ""
                scannedShopName = ""
                scannedTaxAmount = null
                scannedDiscountAmount = null
                scanDurationSeconds = null
                val startTime = System.currentTimeMillis()
                try {
                    val bitmap = if (android.os.Build.VERSION.SDK_INT < 28) {
                        @Suppress("DEPRECATION")
                        android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    } else {
                        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                        android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.isMutableRequired = true
                        }
                    }
                    if (bitmap != null) {
                        val result = com.example.service.GeminiReceiptScanner.scanReceipt(bitmap)
                        scanDurationSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                        if (result.success) {
                            scannedItemsState = result.items.map {
                                EditableScannedItem(
                                    name = it.name,
                                    amount = it.amount,
                                    amountStr = String.format(java.util.Locale.US, "%.0f", it.amount),
                                    category = it.category
                                )
                            }
                            scannedShopName = result.shopName ?: "Hóa đơn mua sắm"
                            selectedType = result.transactionType ?: "EXPENSE"
                            scannedTaxAmount = result.taxAmount
                            scannedDiscountAmount = result.discountAmount

                            val sdf = java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build())
                            scannedDateTimeStr = if (!result.dateTimeStr.isNullOrBlank()) {
                                result.dateTimeStr
                            } else {
                                sdf.format(java.util.Date())
                            }

                            if (result.totalAmount != null) {
                                rawExpression = result.totalAmount.toLong().toString()
                            }
                            viewModel.showSuccessNotification("Quét hóa đơn bằng AI thành công!")
                        } else {
                            scanError = result.errorMessage
                        }
                    } else {
                        scanError = "Không thể đọc dữ liệu ảnh hóa đơn."
                    }
                } catch (e: Exception) {
                    scanError = "Lỗi xử lý ảnh: ${e.localizedMessage}"
                } finally {
                    isScanning = false
                }
            }
        }
    }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        if (bitmap != null) {
            scope.launch {
                isUsingAiState = true
                isScanning = true
                scanError = null
                scannedItemsState = emptyList()
                scannedDateTimeStr = ""
                scannedShopName = ""
                scannedTaxAmount = null
                scannedDiscountAmount = null
                scanDurationSeconds = null
                val startTime = System.currentTimeMillis()
                try {
                    val result = com.example.service.GeminiReceiptScanner.scanReceipt(bitmap)
                    scanDurationSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                    if (result.success) {
                        scannedItemsState = result.items.map {
                            EditableScannedItem(
                                name = it.name,
                                amount = it.amount,
                                amountStr = String.format(java.util.Locale.US, "%.0f", it.amount),
                                category = it.category
                            )
                        }
                        scannedShopName = result.shopName ?: "Hóa đơn từ Camera"
                        selectedType = result.transactionType ?: "EXPENSE"
                        scannedTaxAmount = result.taxAmount
                        scannedDiscountAmount = result.discountAmount

                        val sdf = java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build())
                        scannedDateTimeStr = if (!result.dateTimeStr.isNullOrBlank()) {
                            result.dateTimeStr
                        } else {
                            sdf.format(java.util.Date())
                        }

                        if (result.totalAmount != null) {
                            rawExpression = result.totalAmount.toLong().toString()
                        }
                        viewModel.showSuccessNotification("Quét hóa đơn từ Camera bằng AI thành công!")
                    } else {
                        scanError = result.errorMessage
                    }
                } catch (e: Exception) {
                    scanError = "Lỗi xử lý ảnh: ${e.localizedMessage}"
                } finally {
                    isScanning = false
                }
            }
        }
    }

    val wallets by viewModel.dailyWallets.collectAsState()
    val categoriesList by viewModel.categoriesList.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    // Transfer Setup
    var isTransfer by remember { mutableStateOf(false) }
    var transferWalletId by remember { mutableStateOf<Int?>(null) }

    val events by viewModel.allEvents.collectAsState()
    var isEventTransaction by remember { mutableStateOf(false) }
    var selectedEventId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(events) {
        val now = System.currentTimeMillis()
        val activeEvents = events.filter {
            now >= it.startDate && (it.endDate == null || now <= it.endDate + 86400000L - 1)
        }
        if (activeEvents.isNotEmpty() && selectedEventId == null && !isEventTransaction) {
            val nearestStart = activeEvents.maxByOrNull { it.startDate }
            selectedEventId = nearestStart?.id
            isEventTransaction = true
        }
    }

    // Date Picker Setup
    val calendar = remember { Calendar.getInstance() }
    var selectedTimestamp by remember { mutableStateOf(calendar.timeInMillis) }
    var dateLabel by remember { mutableStateOf("Hôm nay") }

    // Smart Select State Management
    var hasManuallySelected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scrollState.scrollTo(0)
    }

    LaunchedEffect(selectedType) {
        hasManuallySelected = false
    }

    // Auto-select CASH wallet if available, otherwise first wallet
    LaunchedEffect(wallets) {
        if (selectedWalletId == null && wallets.isNotEmpty()) {
            val cashWallet = wallets.find { it.type == "CASH" }
            selectedWalletId = cashWallet?.id ?: wallets.first().id
        }
    }

    // Auto-select transfer target/source wallet if isTransfer is enabled
    LaunchedEffect(isTransfer, selectedWalletId) {
        if (isTransfer) {
            if (transferWalletId == null || transferWalletId == selectedWalletId) {
                transferWalletId = wallets.firstOrNull { it.id != selectedWalletId }?.id
            }
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

    val dateTimeFormatter = remember { SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build()) }

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

    if (isUsingAiState) {
        AiScannerScreen(
            viewModel = viewModel,
            isScanning = isScanning,
            scanError = scanError,
            initialItems = scannedItemsState,
            dateTimeStr = scannedDateTimeStr,
            shopName = scannedShopName,
            selectedTypeInput = selectedType,
            initialTaxAmount = scannedTaxAmount,
            initialDiscountAmount = scannedDiscountAmount,
            scanDuration = scanDurationSeconds,
            wallets = wallets,
            onBack = { isUsingAiState = false },
            onSuccess = {
                // Reset AI scanning states and return to main adding form
                scannedItemsState = emptyList()
                scannedShopName = ""
                scannedDateTimeStr = ""
                scannedTaxAmount = null
                scannedDiscountAmount = null
                isUsingAiState = false
                scanError = null
                scanDurationSeconds = null
                
                // Reset main transaction form fields
                rawExpression = ""
                note = ""
                selectedCategoryName = ""
                
                // Scroll main screen to top
                scope.launch { scrollState.animateScrollTo(0) }
            },
            onReScanCamera = {
                try {
                    cameraLauncher.launch(null)
                } catch (e: Exception) {
                    scanError = "Không thể khởi động camera: ${e.localizedMessage}"
                }
            },
            onReScanGallery = {
                try {
                    galleryLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                } catch (e: Exception) {
                    scanError = "Không thể mở bộ sưu tập: ${e.localizedMessage}"
                }
            }
        )
    } else {
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

        // --- SMART AI RECEIPT SCANNER COMPONENT ---
        if (isAiScannerEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_receipt_scanner_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Scanner", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text("Quét hóa đơn", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        "Lỗi mạng? Dùng prompt",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                        modifier = Modifier.clickable { showManualAiDialog = true }.padding(4.dp)
                    )
                }

                if (isScanning) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AI đang xử lý hóa đơn...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Action for Camera
                        FilledTonalButton(
                            onClick = {
                                try {
                                    cameraLauncher.launch(null)
                                } catch (e: Exception) {
                                    scanError = "Không thể khởi động camera: ${e.localizedMessage}"
                                }
                            },
                            modifier = Modifier.weight(1f).height(36.dp).testTag("ai_scan_camera_btn"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chụp ảnh", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        // Action for Gallery Picker
                        FilledTonalButton(
                            onClick = {
                                try {
                                    galleryLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                } catch (e: Exception) {
                                    scanError = "Không thể mở bộ sưu tập: ${e.localizedMessage}"
                                }
                            },
                            modifier = Modifier.weight(1f).height(36.dp).testTag("ai_scan_gallery_btn"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chọn ảnh", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                scanError?.let { err ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = err,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        }

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
                Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Chi", modifier = Modifier.size(18.dp))
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
                Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Thu", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Khoản Thu")
            }
        }

        // 7. Số tiền
        com.example.ui.components.CustomMoneyInputField(
            value = rawExpression,
            onValueChange = { rawExpression = it },
            label = "Số tiền phát sinh",
            autoFocus = false,
            onDismissKeyboard = {
                scope.launch {
                    scrollState.animateScrollTo(0)
                }
            },
            testTag = "tx_amount_text_field"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Layout 2 options side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Feature 1: Transfer
                Card(
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTransfer) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                                         else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (isTransfer) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { 
                                isTransfer = !isTransfer 
                                if (isTransfer) isEventTransaction = false
                            }
                            .padding(start = 12.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Transfer",
                                tint = if (isTransfer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nội bộ",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Switch(
                            checked = isTransfer,
                            onCheckedChange = { 
                                isTransfer = it 
                                if (it) isEventTransaction = false
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.scale(0.7f).testTag("transfer_quick_switch")
                        )
                    }
                }
                
                // Feature 2: Event
                Card(
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEventTransaction) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                                         else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (isEventTransaction) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { 
                                isEventTransaction = !isEventTransaction 
                                if (isEventTransaction) isTransfer = false
                            }
                            .padding(start = 12.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "Event",
                                tint = if (isEventTransaction) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sự kiện",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Switch(
                            checked = isEventTransaction,
                            onCheckedChange = { 
                                isEventTransaction = it 
                                if (it) isTransfer = false
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                }
            }
            
            // Thêm tính năng chọn Event
            val activeEventsForSelection = events.filter {
                val now = System.currentTimeMillis()
                now >= it.startDate && (it.endDate == null || now <= it.endDate + 86400000L - 1)
            }.sortedBy { it.startDate }

            val isOptionExpanded = isEventTransaction || isTransfer
            if (isOptionExpanded) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                            if (isEventTransaction) {
                                var showQuickCreate by remember { mutableStateOf(false) }
        
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    items(activeEventsForSelection) { event ->
                                            val isSelected = event.id == selectedEventId
                                            val eventColor = try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(event.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) eventColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
                                                    .border(if (isSelected) 2.dp else 1.dp, if (isSelected) eventColor else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                    .clickable { selectedEventId = event.id }
                                            ) {
                                                Text(
                                                    event.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) eventColor else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                                )
                                                
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier.matchParentSize()
                                                    ) {
                                                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                                            val sizePx = 24.dp.toPx()
                                                            val path = androidx.compose.ui.graphics.Path().apply {
                                                                moveTo(size.width, 0f)
                                                                lineTo(size.width, sizePx)
                                                                lineTo(size.width - sizePx, 0f)
                                                                close()
                                                            }
                                                            drawPath(path, color = eventColor)
                                                        }
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier
                                                                .size(12.dp)
                                                                .align(Alignment.TopEnd)
                                                                .offset(x = (-2).dp, y = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                    .clickable { showQuickCreate = true }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Tạo mới", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
        
                                    if (showQuickCreate) {
                                        var newEventName by remember { mutableStateOf("") }
                                        var newEventDesc by remember { mutableStateOf("") }
                                        AlertDialog(
                                            onDismissRequest = { showQuickCreate = false },
                                            title = { Text("Tạo sự kiện mới", fontWeight = FontWeight.Bold) },
                                            text = {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedTextField(
                                                        value = newEventName,
                                                        onValueChange = { newEventName = it },
                                                        label = { Text("Tên sự kiện (*)") },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        if (newEventName.isNotBlank()) {
                                                            viewModel.addEvent(
                                                                name = newEventName,
                                                                description = newEventDesc,
                                                                startDate = System.currentTimeMillis(),
                                                                endDate = null,
                                                                limitAmount = null
                                                            )
                                                            viewModel.showSuccessNotification("Đã tạo $newEventName")
                                                            showQuickCreate = false
                                                        } else {
                                                            viewModel.showWarningNotification("Vui lòng nhập tên")
                                                        }
                                                    }
                                                ) { Text("Lưu") }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showQuickCreate = false }) { Text("Hủy") }
                                            }
                                        )
                                    }
                            } else if (isTransfer) {
                                val transferLabel = if (selectedType == "EXPENSE") "Ví nhận tiền (Đích)" else "Ví rút tiền (Nguồn)"
                                Text(
                                    text = transferLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                
                                if (wallets.isEmpty()) {
                                    Text(
                                        text = "Không tìm thấy ví khả dụng.",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 13.sp
                                    )
                                } else {
                                    // Filter out the primary selected wallet to avoid transfer to self!
                                    val availableTransferWallets = wallets.filter { it.id != selectedWalletId }
                                    if (availableTransferWallets.isEmpty()) {
                                        Text(
                                            text = "Vui lòng tạo thêm ví khác để chuyển tiền.",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 13.sp
                                        )
                                    } else {
                                        val chunkedTransferWallets = availableTransferWallets.chunked(2)
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            chunkedTransferWallets.forEach { rowWallets ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    rowWallets.forEach { wt ->
                                                        val isSelected = transferWalletId == wt.id
                                                        val accentColor = FormatHelper.parseColor(wt.colorHex)
                                                        val cardColor = if (isSelected) accentColor.copy(alpha = 0.15f)
                                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                        val borderColor = if (isSelected) accentColor
                                                                          else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                                        
                                                        Card(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(48.dp)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .clickable { transferWalletId = wt.id }
                                                                .testTag("tx_transfer_wallet_chip_${wt.id}"),
                                                            colors = CardDefaults.cardColors(containerColor = cardColor),
                                                            border = BorderStroke(if (isSelected) 1.8.dp else 1.dp, borderColor)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(24.dp)
                                                                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = IconMapper.getIconByName(wt.iconName),
                                                                        contentDescription = wt.name,
                                                                        tint = accentColor,
                                                                        modifier = Modifier.size(14.dp)
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
                            }
                        }
                    }

                    val surfaceColor = MaterialTheme.colorScheme.surface
                    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    val isLeft = isTransfer
                    
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(9.dp)) {
                        val triangleW = 16.dp.toPx()
                        val triangleH = 8.dp.toPx()
                        val cardY = 8.dp.toPx()
                        
                        val centerOffset = if (isLeft) (size.width / 2f - 4.dp.toPx()) / 2f 
                                            else size.width - (size.width / 2f - 4.dp.toPx()) / 2f
                                            
                        val fillPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(centerOffset, 0f)
                            lineTo(centerOffset + triangleW / 2f, cardY + 2.5f) 
                            lineTo(centerOffset - triangleW / 2f, cardY + 2.5f)
                            close()
                        }
                        drawPath(fillPath, color = surfaceColor)
                        
                        drawLine(
                            color = borderColor,
                            start = androidx.compose.ui.geometry.Offset(centerOffset, 0f),
                            end = androidx.compose.ui.geometry.Offset(centerOffset + triangleW / 2f, cardY),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawLine(
                            color = borderColor,
                            start = androidx.compose.ui.geometry.Offset(centerOffset, 0f),
                            end = androidx.compose.ui.geometry.Offset(centerOffset - triangleW / 2f, cardY),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
            }

            // 2. Tài khoản thanh toán (bố cục 2x2 đẹp mắt)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (isTransfer) {
                    if (selectedType == "EXPENSE") "Ví chuyển đi (Nguồn)" else "Ví nhận về (Đích)"
                } else "Tài khoản thanh toán",
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
        } // Closed wrapping Column

        if (isTransfer) {
            // Thông báo chuyển tiền
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Transfer info",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Giao dịch Chuyển khoản liên ví không cần chọn hạng mục. Hệ thống sẽ tự động hạch toán đối ứng Thu & Chi để dòng tiền luôn chính xác.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        } else {
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

        } // Kết thúc khối if (!isTransfer)

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
                        IconButton(
                            onClick = { showDateTimePicker() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Chọn ngày giờ",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(end = 48.dp) // Tránh che icon lịch
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDateTimePicker() }
                )
            }
        }

        // --- activeEventsForSelection is defined above ---
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

        Spacer(modifier = Modifier.height(16.dp))

        // 8. Lưu
        Button(
            onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                
                val amount = FormatHelper.evaluateExpression(rawExpression)
                val walletId = selectedWalletId
                if (amount > 0 && walletId != null) {
                    if (isTransfer) {
                        val targetId = transferWalletId
                        if (targetId != null && targetId != walletId) {
                            val sourceWallet = wallets.firstOrNull { it.id == walletId }
                            val targetWallet = wallets.firstOrNull { it.id == targetId }
                            val sourceName = sourceWallet?.name ?: "Ví nguồn"
                            val targetName = targetWallet?.name ?: "Ví đích"
                            
                            val finalSourceId = if (selectedType == "EXPENSE") walletId else targetId
                            val finalDestId = if (selectedType == "EXPENSE") targetId else walletId
                            
                            viewModel.addTransaction(
                                walletId = finalSourceId,
                                type = "TRANSFER",
                                amount = amount,
                                categoryName = "Chuyển khoản",
                                note = note.ifBlank { "Chuyển khoản nội bộ" },
                                timestamp = selectedTimestamp,
                                isRecurring = false,
                                recurrencePeriod = "NONE",
                                destinationWalletId = finalDestId
                            )
                            viewModel.showSuccessNotification("Thực hiện chuyển khoản liên ví thành công!")
                            
                            // Reset state fields & stay on screen
                            rawExpression = ""
                            note = ""
                            selectedCategoryName = ""
                            isTransfer = false
                            transferWalletId = null
                            scope.launch { scrollState.animateScrollTo(0) }
                        } else {
                            viewModel.showWarningNotification("Vui lòng chọn ví nhận khác nhau!")
                        }
                    } else {
                        // Normal manual transaction
                        if (selectedCategoryName.isEmpty()) {
                            viewModel.showWarningNotification("Vui lòng chọn hạng mục giao dịch!")
                            return@Button
                        }
                        viewModel.addTransaction(
                            walletId = walletId,
                            type = selectedType,
                            amount = amount,
                            categoryName = selectedCategoryName,
                            note = note,
                            timestamp = selectedTimestamp,
                            isRecurring = false,
                            recurrencePeriod = "NONE",
                            eventId = if (isEventTransaction) selectedEventId else null
                        )
                        viewModel.showSuccessNotification("Thêm giao dịch mới thành công!")
                        
                        // Reset state fields instead of navigating away
                        rawExpression = ""
                        note = ""
                        selectedCategoryName = ""
                        scope.launch { scrollState.animateScrollTo(0) }
                    }
                }
            },
            enabled = if (isTransfer) {
                FormatHelper.evaluateExpression(rawExpression) > 0.0 && selectedWalletId != null && transferWalletId != null && transferWalletId != selectedWalletId
            } else {
                FormatHelper.evaluateExpression(rawExpression) > 0.0 && selectedWalletId != null && selectedCategoryName.isNotEmpty()
            },
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

        Spacer(modifier = Modifier.height(100.dp))
        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.ime))
    }

    if (showManualAiDialog) {
        ManualAiDialog(
            viewModel = viewModel,
            onDismiss = { showManualAiDialog = false },
            onJsonSubmit = { jsonString ->
                showManualAiDialog = false
                isScanning = true
                scanError = null
                val startTime = System.currentTimeMillis()
                
                try {
                    val result = com.example.service.GeminiReceiptScanner.parseManualJson(jsonString)
                    scanDurationSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                    
                    if (result.success) {
                        scannedItemsState = result.items.map {
                            EditableScannedItem(
                                name = it.name,
                                amount = it.amount,
                                amountStr = it.amount.toLong().toString(),
                                category = it.category
                            )
                        }
                        scannedDateTimeStr = result.dateTimeStr ?: ""
                        scannedShopName = result.shopName ?: ""
                        scannedTaxAmount = result.taxAmount
                        scannedDiscountAmount = result.discountAmount
                        
                        selectedType = result.transactionType ?: "EXPENSE"
                        isUsingAiState = true 
                    } else {
                        scanError = result.errorMessage ?: "Có lỗi xảy ra khi đọc dữ liệu."
                    }
                } catch (e: Exception) {
                    scanError = "Dữ liệu JSON không hợp lệ."
                }
                isScanning = false
            }
        )
    }
}
}

@Composable
fun AiScannerScreen(
    viewModel: com.example.ui.FinanceViewModel,
    isScanning: Boolean,
    scanError: String?,
    initialItems: List<EditableScannedItem>,
    dateTimeStr: String,
    shopName: String,
    selectedTypeInput: String,
    initialTaxAmount: Double? = null,
    initialDiscountAmount: Double? = null,
    scanDuration: Double? = null,
    wallets: List<com.example.data.Wallet>,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onReScanCamera: () -> Unit,
    onReScanGallery: () -> Unit
) {
    val context = LocalContext.current
    var itemsList by remember(initialItems) { mutableStateOf(initialItems) }
    var selectedDateTimeStrState by remember(dateTimeStr) { mutableStateOf(dateTimeStr) }
    var shopNameState by remember(shopName) { mutableStateOf(shopName) }
    var selectedType by remember(selectedTypeInput) { mutableStateOf(selectedTypeInput) }
    var selectedWalletId by remember { mutableStateOf<Int?>(null) }
    var isMerged by remember { mutableStateOf(false) }
    var taxState by remember(initialTaxAmount) {
        mutableStateOf(initialTaxAmount?.let { if (it > 0) String.format(java.util.Locale.US, "%.0f", it) else "" } ?: "")
    }
    var discountState by remember(initialDiscountAmount) {
        mutableStateOf(initialDiscountAmount?.let { if (it > 0) String.format(java.util.Locale.US, "%.0f", it) else "" } ?: "")
    }
    val parsedTax = taxState.toDoubleOrNull() ?: 0.0
    val parsedDiscount = discountState.toDoubleOrNull() ?: 0.0
    var mergedNoteState by remember(shopName, itemsList) {
        val itemsCount = itemsList.size
        mutableStateOf(
            if (shopName.isNotBlank()) "$shopName / Hóa đơn của $itemsCount món"
            else "Hóa đơn của $itemsCount món"
        )
    }

    // Auto-select first wallet if available
    LaunchedEffect(wallets) {
        if (selectedWalletId == null && wallets.isNotEmpty()) {
            selectedWalletId = wallets.first().id
        }
    }

    // Modal state for category selection
    var selectCategoryForIndex by remember { mutableStateOf<Int?>(null) }
    val categoriesList by viewModel.categoriesList.collectAsState()

    // Calculate total reactively
    val totalSum = remember(itemsList) {
        itemsList.sumOf { it.amountStr.toDoubleOrNull() ?: 0.0 }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation Title / Header Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("ai_scanner_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Thêm giao dịch",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Status Card showing processing state or finished indicator (Highly Compact Version for Optimization)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isScanning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Đang phân tích hóa đơn...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (scanDuration != null) {
                                    "Đã xong (${String.format(java.util.Locale.US, "%.1f", scanDuration)}s)"
                                } else {
                                    "Đã xong"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onReScanCamera,
                                modifier = Modifier.height(30.dp).testTag("ai_rescan_camera_btn"),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chụp lại", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = onReScanGallery,
                                modifier = Modifier.height(30.dp).testTag("ai_rescan_gallery_btn"),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chọn ảnh", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { selectedType = "EXPENSE" },
                            modifier = Modifier.weight(1f).height(34.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedType == "EXPENSE") Color(0xFFEF5350) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (selectedType == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Khoản chi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { selectedType = "INCOME" },
                            modifier = Modifier.weight(1f).height(34.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedType == "INCOME") Color(0xFF66BB6A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (selectedType == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Khoản thu", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (!scanError.isNullOrBlank()) {
                    Text(
                        text = scanError,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // List Content (Records or Skeletons)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isScanning) {
                // Skeleton loading state
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(4) {
                        SkeletonItem()
                    }
                }
            } else {
                // Item Records List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(itemsList) { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Category chip mapping
                                val financeCat = remember(item.category) {
                                    com.example.data.Categories.getByCategoryName(item.category)
                                }
                                
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                Color(android.graphics.Color.parseColor(financeCat.colorHex)).copy(alpha = 0.15f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                selectCategoryForIndex = index
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = IconMapper.getIconByName(financeCat.iconName),
                                            contentDescription = financeCat.name,
                                            tint = Color(android.graphics.Color.parseColor(financeCat.colorHex)),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = "${index + 1}.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Product Name input field
                                OutlinedTextField(
                                    value = item.name,
                                    onValueChange = { newVal ->
                                        itemsList = itemsList.toMutableList().apply {
                                            this[index] = this[index].copy(name = newVal)
                                        }
                                    },
                                    modifier = Modifier.weight(1.5f),
                                    textStyle = TextStyle(fontSize = 13.sp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                )

                                // Amount input field
                                OutlinedTextField(
                                    value = FormatHelper.formatInputNumber(item.amountStr),
                                    onValueChange = { newVal ->
                                        val cleanDigits = newVal.filter { it.isDigit() }
                                        val parseVal = cleanDigits.toDoubleOrNull() ?: 0.0
                                        itemsList = itemsList.toMutableList().apply {
                                            this[index] = this[index].copy(
                                                amountStr = cleanDigits,
                                                amount = parseVal
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                )

                                // Delete Record Item Button
                                IconButton(
                                    onClick = {
                                        itemsList = itemsList.toMutableList().apply {
                                            removeAt(index)
                                        }
                                    },
                                    modifier = Modifier.size(36.dp).testTag("delete_scanned_item_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Xóa mặt hàng",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom elements inside Scroll View to avoid clipping
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        // Invoice metadata (dateTime & Total)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Thời gian hóa đơn:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Clickable text to edit invoice timestamp
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val cal = Calendar.getInstance()
                                        val parser = SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build())
                                        try {
                                            val parsedDate = parser.parse(selectedDateTimeStrState)
                                            if (parsedDate != null) {
                                                cal.time = parsedDate
                                            }
                                        } catch (e: Exception) {}

                                        val timePicker = TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                cal.set(Calendar.MINUTE, minute)
                                                selectedDateTimeStrState = parser.format(cal.time)
                                            },
                                            cal.get(Calendar.HOUR_OF_DAY),
                                            cal.get(Calendar.MINUTE),
                                            true
                                        )

                                        val datePicker = DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                cal.set(Calendar.YEAR, year)
                                                cal.set(Calendar.MONTH, month)
                                                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                timePicker.show()
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        )
                                        datePicker.show()
                                    }
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Edit Time",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = selectedDateTimeStrState,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Total items sum label
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tổng tiền hàng:",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatHelper.formatVND(totalSum),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Add extra/VAT state (Requirement 4)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Thuế VAT / Chi phí khác:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedTextField(
                                value = FormatHelper.formatInputNumber(taxState),
                                onValueChange = { newVal ->
                                    val clean = newVal.filter { it.isDigit() }
                                    taxState = clean
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Add Voucher/Discount state (Requirement 5)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = null,
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Giảm tiền phát sinh / Voucher:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedTextField(
                                value = FormatHelper.formatInputNumber(discountState),
                                onValueChange = { newVal ->
                                    val clean = newVal.filter { it.isDigit() }
                                    discountState = clean
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dynamic Actual Amount Display card (Requirement 6)
                        val actualAmount = remember(totalSum, parsedTax, parsedDiscount) {
                            maxOf(0.0, totalSum + parsedTax - parsedDiscount)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tiền thực tế thanh toán:",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = FormatHelper.formatVND(actualAmount),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (selectedType == "EXPENSE") Color(0xFFF44336) else Color(0xFF4CAF50)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Wallet Accounts Source Select
                        Text(
                            text = "Tài khoản thanh toán:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            wallets.forEach { wt ->
                                val isSelected = selectedWalletId == wt.id
                                val accentColor = FormatHelper.parseColor(wt.colorHex)
                                val cardColor = if (isSelected) accentColor.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                val borderColor = if (isSelected) accentColor else Color.Transparent

                                Card(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedWalletId = wt.id },
                                    colors = CardDefaults.cardColors(containerColor = cardColor),
                                    border = BorderStroke(if (isSelected) 1.5.dp else 0.dp, borderColor)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = IconMapper.getIconByName(wt.iconName),
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = wt.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Group Invoice Toggle Selector (Requirement 3)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Gộp hóa đơn",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Gộp tất cả sản phẩm thành một danh mục 'Hóa đơn'",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = isMerged,
                                        onCheckedChange = { isMerged = it }
                                    )
                                }

                                if (isMerged) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    Text(
                                        text = "Ghi chú hóa đơn (Tên cửa hàng, sản phẩm...):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = mergedNoteState,
                                        onValueChange = { mergedNoteState = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = TextStyle(fontSize = 13.sp),
                                        placeholder = { Text("Nhập tên công ty / cửa hàng / ghi chú hóa đơn...") },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Trigger Save Button
                        Button(
                            onClick = {
                                val parser = SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build())
                                val finalTimestamp = try {
                                    parser.parse(selectedDateTimeStrState)?.time ?: System.currentTimeMillis()
                                } catch (e: Exception) {
                                    System.currentTimeMillis()
                                }

                                val activeWalletId = selectedWalletId
                                if (activeWalletId != null) {
                                    if (isMerged || itemsList.isEmpty()) {
                                        // Merge into single invoice transaction using calculated actualAmount!
                                        viewModel.addTransaction(
                                            walletId = activeWalletId,
                                            type = selectedType,
                                            amount = actualAmount,
                                            categoryName = "Hóa đơn",
                                            note = if (mergedNoteState.isNotBlank()) mergedNoteState else (if (shopNameState.isNotBlank()) shopNameState else "Hóa đơn quét bằng AI"),
                                            timestamp = finalTimestamp,
                                            isRecurring = false,
                                            recurrencePeriod = "NONE"
                                        )
                                    } else {
                                        // Save as multiple granular records as requested by user, distributing VAT & discounts proportionally
                                        itemsList.forEach { editable ->
                                            if (editable.amount > 0) {
                                                val proportion = if (totalSum > 0.0) editable.amount / totalSum else 0.0
                                                val itemPart = proportion * (parsedTax - parsedDiscount)
                                                val finalAmount = maxOf(0.0, editable.amount + itemPart)

                                                viewModel.addTransaction(
                                                    walletId = activeWalletId,
                                                    type = selectedType,
                                                    amount = finalAmount,
                                                    categoryName = editable.category,
                                                    note = editable.name,
                                                    timestamp = finalTimestamp,
                                                    isRecurring = false,
                                                    recurrencePeriod = "NONE"
                                                )
                                            }
                                        }
                                    }

                                    viewModel.showSuccessNotification("Lưu thông tin hóa đơn thành công!")
                                    onSuccess()
                                } else {
                                    viewModel.showWarningNotification("Vui lòng chọn tài khoản thanh toán!")
                                }
                            },
                            enabled = !isScanning && (isMerged || itemsList.isNotEmpty() || actualAmount > 0) && selectedWalletId != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("ai_scanner_save_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedType == "EXPENSE") Color(0xFFF44336) else Color(0xFF4CAF50),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "CHẤP NHẬN & LƯU HÓA ĐƠN",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    // Render Category selection dropdown modal dialog
    selectCategoryForIndex?.let { itemIdx ->
        CategorySelectionDialog(
            categories = categoriesList,
            currentType = selectedType,
            onCategorySelected = { cat ->
                itemsList = itemsList.toMutableList().apply {
                    this[itemIdx] = this[itemIdx].copy(category = cat.name)
                }
            },
            onDismiss = { selectCategoryForIndex = null }
        )
    }
}

@Composable
fun ManualAiDialog(
    viewModel: com.example.ui.FinanceViewModel,
    onDismiss: () -> Unit,
    onJsonSubmit: (String) -> Unit
) {
    var jsonInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.IntegrationInstructions, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Dùng prompt", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("Khi hệ thống tự động bận, bạn có thể tự nhờ AI phân tích theo 3 bước sau:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Spacer(Modifier.height(16.dp))
                
                Text("Bước 1: Sao chép câu lệnh", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                OutlinedButton(
                    onClick = {
                        val annotatedString = androidx.compose.ui.text.AnnotatedString(com.example.service.GeminiReceiptScanner.GEMINI_PROMPT_TEXT)
                        clipboardManager.setText(annotatedString)
                        viewModel.showSuccessNotification("Đã chép câu lệnh")
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sao chép câu lệnh (Prompt)")
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text("Bước 2: Mở Gemini / ChatGPT", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Gửi ảnh hóa đơn của bạn kèm câu lệnh vừa chép cho hệ thống AI.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://gemini.google.com"))
                            try {
                                context.startActivity(android.content.Intent.createChooser(intent, "Mở trình duyệt"))
                            } catch (e: Exception) {
                                viewModel.showErrorNotification("Không thể mở web")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text("Mở Gemini", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://chatgpt.com"))
                            try {
                                context.startActivity(android.content.Intent.createChooser(intent, "Mở trình duyệt"))
                            } catch (e: Exception) {
                                viewModel.showErrorNotification("Không thể mở web")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text("Mở ChatGPT", fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bước 3: Dán kết quả vào đây", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Chép khối JSON mà AI trả về.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrEmpty()) {
                                jsonInput = clipText
                            }
                        }
                    ) {
                        Text("Dán")
                    }
                }
                
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp).padding(top = 4.dp),
                    placeholder = { Text("{\n  \"totalAmount\": 199000,\n  ...\n}") },
                    textStyle = TextStyle(fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJsonSubmit(jsonInput) },
                enabled = jsonInput.isNotBlank() && jsonInput.contains("{") && jsonInput.contains("}")
            ) {
                Text("Xác nhận dữ liệu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun SkeletonItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Gray.copy(alpha = alpha), shape = CircleShape)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .background(Color.Gray.copy(alpha = alpha), shape = RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(12.dp)
                        .background(Color.Gray.copy(alpha = alpha), shape = RoundedCornerShape(4.dp))
                )
            }
        }
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(18.dp)
                .background(Color.Gray.copy(alpha = alpha), shape = RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun CategorySelectionDialog(
    categories: List<com.example.data.FinanceCategory>,
    currentType: String,
    onCategorySelected: (com.example.data.FinanceCategory) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn hạng mục cho vật vật", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            val filtered = categories.filter { it.type == currentType || it.type == "BOTH" }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.heightIn(max = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { cat ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCategorySelected(cat)
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(android.graphics.Color.parseColor(cat.colorHex)).copy(alpha = 0.2f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = IconMapper.getIconByName(cat.iconName),
                                    contentDescription = cat.name,
                                    tint = Color(android.graphics.Color.parseColor(cat.colorHex)),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(cat.name, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
