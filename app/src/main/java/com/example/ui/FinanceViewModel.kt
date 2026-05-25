package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class FilterCriteria(
    val query: String,
    val type: String,
    val category: String,
    val start: Long?,
    val end: Long?
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    // Google Sync settings Flow
    private val _googleSheetUrl = MutableStateFlow("")
    val googleSheetUrl: StateFlow<String> = _googleSheetUrl.asStateFlow()

    private val _googleDocUrl = MutableStateFlow("")
    val googleDocUrl: StateFlow<String> = _googleDocUrl.asStateFlow()

    private val _googleAppsScriptUrl = MutableStateFlow("")
    val googleAppsScriptUrl: StateFlow<String> = _googleAppsScriptUrl.asStateFlow()

    private val _googleSheetLastSync = MutableStateFlow("")
    val googleSheetLastSync: StateFlow<String> = _googleSheetLastSync.asStateFlow()

    private val _googleDocLastSync = MutableStateFlow("")
    val googleDocLastSync: StateFlow<String> = _googleDocLastSync.asStateFlow()

    private val _syncStatus = MutableStateFlow("") // "IDLE", "SYNCING", "SUCCESS", "ERROR"
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _syncProgressLogs = MutableStateFlow<List<String>>(emptyList())
    val syncProgressLogs: StateFlow<List<String>> = _syncProgressLogs.asStateFlow()

    // Base flows
    val allWallets: StateFlow<List<Wallet>>
    val allTransactions: StateFlow<List<Transaction>>
    val allBudgets: StateFlow<List<Budget>>
    val allSavingsGoals: StateFlow<List<SavingsGoal>>

    val dailyWallets: StateFlow<List<Wallet>>
    val savingsWallets: StateFlow<List<Wallet>>
    val dailyTransactions: StateFlow<List<Transaction>>
    val savingsTransactions: StateFlow<List<Transaction>>

    private val _categoriesList = MutableStateFlow<List<FinanceCategory>>(Categories.list)
    val categoriesList: StateFlow<List<FinanceCategory>> = _categoriesList.asStateFlow()

    // PIN Protection Flow
    private val _isPinEnabled = MutableStateFlow(false)
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _savedPinHash = MutableStateFlow("")
    val savedPinHash: StateFlow<String> = _savedPinHash.asStateFlow()

    // Transaction History Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow("ALL") // ALL, INCOME, EXPENSE
    val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("ALL") // ALL, and individual categories
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _filterStartDate = MutableStateFlow<Long?>(null)
    val filterStartDate: StateFlow<Long?> = _filterStartDate.asStateFlow()

    private val _filterEndDate = MutableStateFlow<Long?>(null)
    val filterEndDate: StateFlow<Long?> = _filterEndDate.asStateFlow()

    // Filtered transactions
    val filteredTransactions: StateFlow<List<Transaction>>

    // Current Active Month (For budgets and statistics) - default to current month
    private val _activeMonth = MutableStateFlow(
        Calendar.getInstance().let { cal ->
            String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }
    )
    val activeMonth: StateFlow<String> = _activeMonth.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        val dao = database.financeDao()
        repository = FinanceRepository(dao)

        // Base states
        allWallets = repository.allWallets
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allTransactions = repository.allTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        dailyWallets = repository.allWallets
            .map { list -> list.filter { it.type != "SAVINGS" } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        savingsWallets = repository.allWallets
            .map { list -> list.filter { it.type == "SAVINGS" } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        dailyTransactions = combine(repository.allTransactions, repository.allWallets) { txs, wts ->
            val savingsIds = wts.filter { it.type == "SAVINGS" }.map { it.id }.toSet()
            txs.filter { it.walletId !in savingsIds }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        savingsTransactions = combine(repository.allTransactions, repository.allWallets) { txs, wts ->
            val savingsIds = wts.filter { it.type == "SAVINGS" }.map { it.id }.toSet()
            txs.filter { it.walletId in savingsIds }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allBudgets = repository.getAllBudgets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allSavingsGoals = repository.allSavingsGoals
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Setup filter flow
        val filterCriteriaFlow = combine(
            _searchQuery,
            _selectedTypeFilter,
            _selectedCategoryFilter,
            _filterStartDate,
            _filterEndDate
        ) { q, t, c, s, e ->
            FilterCriteria(q, t, c, s, e)
        }

        filteredTransactions = combine(
            dailyTransactions,
            filterCriteriaFlow
        ) { txs, criteria ->
            txs.filter { tx ->
                val matchesQuery = tx.note.contains(criteria.query, ignoreCase = true) ||
                        tx.categoryName.contains(criteria.query, ignoreCase = true) ||
                        tx.walletName.contains(criteria.query, ignoreCase = true)
                val matchesType = criteria.type == "ALL" || tx.type == criteria.type
                val matchesCategory = criteria.category == "ALL" || tx.categoryName == criteria.category
                val matchesStart = criteria.start == null || tx.timestamp >= criteria.start
                val matchesEnd = criteria.end == null || tx.timestamp <= criteria.end

                matchesQuery && matchesType && matchesCategory && matchesStart && matchesEnd
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Check and Seed default data
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
            loadCategories()
            loadSecuritySettings()
            processRecurringTransactions()
        }
    }

    // --- SECURITY PIN LOGIC ---
    private suspend fun loadSecuritySettings() {
        val pinEnabledSetting = repository.getSetting("pin_enabled")
        val pinHashSetting = repository.getSetting("pin_hash")

        val enabled = pinEnabledSetting?.value == "true"
        _isPinEnabled.value = enabled
        _savedPinHash.value = pinHashSetting?.value ?: ""
        
        // If PIN is not enabled, the app is unlocked by default
        _isAppUnlocked.value = !enabled

        // Load sync configurations
        _googleSheetUrl.value = repository.getSetting("google_sheet_url")?.value ?: ""
        _googleDocUrl.value = repository.getSetting("google_doc_url")?.value ?: ""
        _googleAppsScriptUrl.value = repository.getSetting("google_apps_script_url")?.value ?: ""
        _googleSheetLastSync.value = repository.getSetting("google_sheet_last_sync")?.value ?: "Chưa đồng bộ"
        _googleDocLastSync.value = repository.getSetting("google_doc_last_sync")?.value ?: "Chưa đồng bộ"
    }

    // --- CATEGORIES LOGIC ---
    private fun serializeCategories(categories: List<FinanceCategory>): String {
        return categories.joinToString(";;") { "${it.name}|${it.iconName}|${it.colorHex}|${it.type}|${it.parentName ?: ""}" }
    }

    private fun deserializeCategories(data: String): List<FinanceCategory> {
        if (data.isEmpty()) return emptyList()
        return data.split(";;").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 4) {
                val parentName = if (parts.size >= 5 && parts[4].isNotEmpty()) parts[4] else null
                FinanceCategory(parts[0], parts[1], parts[2], parts[3], parentName)
            } else null
        }
    }

    private suspend fun loadCategories() {
        val catSetting = repository.getSetting("custom_categories")
        if (catSetting != null) {
            _categoriesList.value = deserializeCategories(catSetting.value)
        } else {
            // Seed defaults
            repository.saveSetting("custom_categories", serializeCategories(Categories.list))
            _categoriesList.value = Categories.list
        }
    }

    fun addCategory(name: String, iconName: String, colorHex: String, type: String, parentName: String? = null) {
        viewModelScope.launch {
            val currentList = _categoriesList.value.toMutableList()
            if (!currentList.any { it.name.lowercase() == name.lowercase() }) {
                currentList.add(FinanceCategory(name, iconName, colorHex, type, parentName))
                repository.saveSetting("custom_categories", serializeCategories(currentList))
                _categoriesList.value = currentList
            }
        }
    }

    fun deleteCategory(category: FinanceCategory) {
        viewModelScope.launch {
            val currentList = _categoriesList.value.filter { it.name.lowercase() != category.name.lowercase() }
            repository.saveSetting("custom_categories", serializeCategories(currentList))
            _categoriesList.value = currentList
        }
    }

    fun updateCategoriesOrder(orderedSubList: List<FinanceCategory>, typeTab: String) {
        viewModelScope.launch {
            val fullList = _categoriesList.value.toMutableList()
            val subNames = orderedSubList.map { it.name.lowercase() }.toSet()
            val newList = mutableListOf<FinanceCategory>()
            var subIndex = 0
            for (cat in fullList) {
                if (cat.name.lowercase() in subNames) {
                    if (subIndex < orderedSubList.size) {
                        newList.add(orderedSubList[subIndex])
                        subIndex++
                    }
                } else {
                    newList.add(cat)
                }
            }
            while (subIndex < orderedSubList.size) {
                newList.add(orderedSubList[subIndex])
                subIndex++
            }
            repository.saveSetting("custom_categories", serializeCategories(newList))
            _categoriesList.value = newList
        }
    }

    fun getCategoryByName(name: String): FinanceCategory {
        return _categoriesList.value.firstOrNull { it.name.lowercase() == name.lowercase() }
            ?: FinanceCategory("Khác", "Category", "#607D8B", "BOTH")
    }

    fun enablePin(pin: String) {
        viewModelScope.launch {
            repository.saveSetting("pin_enabled", "true")
            repository.saveSetting("pin_hash", pin)
            _isPinEnabled.value = true
            _savedPinHash.value = pin
            _isAppUnlocked.value = true
        }
    }

    fun disablePin() {
        viewModelScope.launch {
            repository.saveSetting("pin_enabled", "false")
            repository.saveSetting("pin_hash", "")
            _isPinEnabled.value = false
            _savedPinHash.value = ""
            _isAppUnlocked.value = true
        }
    }

    fun unlockApp(pin: String): Boolean {
        return if (pin == _savedPinHash.value) {
            _isAppUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun lockApp() {
        if (_isPinEnabled.value) {
            _isAppUnlocked.value = false
        }
    }

    // --- RECURRING TRANSACTIONS ENGINE ---
    private suspend fun processRecurringTransactions() {
        // Find existing recurring transactions and check if duplicate is due
        val txs = repository.allTransactions.firstOrNull() ?: return
        val recurringSource = txs.filter { it.isRecurring && it.recurrencePeriod != "NONE" }
        
        val currentTime = System.currentTimeMillis()
        
        for (src in recurringSource) {
            var lastOccurrenceTime = src.timestamp
            // Find other occurrences in history of the same source
            val related = txs.filter { it.note == src.note && it.amount == src.amount && it.walletId == src.walletId && it.categoryName == src.categoryName }
            val latestInstance = related.maxByOrNull { it.timestamp }
            if (latestInstance != null) {
                lastOccurrenceTime = latestInstance.timestamp
            }

            val intervalMs = when (src.recurrencePeriod) {
                "DAILY" -> 24L * 60L * 60L * 1000L
                "WEEKLY" -> 7L * 24L * 60L * 60L * 1000L
                "MONTHLY" -> 30L * 24L * 60L * 60L * 1000L // Simple approximation
                else -> Long.MAX_VALUE
            }

            var nextTime = lastOccurrenceTime + intervalMs
            while (nextTime <= currentTime && intervalMs < Long.MAX_VALUE) {
                // Insert a duplicate dated nextTime!
                val newTx = Transaction(
                    walletId = src.walletId,
                    walletName = src.walletName,
                    type = src.type,
                    amount = src.amount,
                    categoryName = src.categoryName,
                    categoryIcon = src.categoryIcon,
                    categoryColor = src.categoryColor,
                    note = src.note,
                    timestamp = nextTime,
                    isRecurring = src.isRecurring,
                    recurrencePeriod = src.recurrencePeriod
                )
                repository.insertTransaction(newTx)
                nextTime += intervalMs
            }
        }
    }

    // --- WALLETS SERVICES ---
    fun addWallet(name: String, type: String, initialBalance: Double, colorHex: String, iconName: String) {
        viewModelScope.launch {
            val list = allWallets.value
            val maxOrder = list.maxOfOrNull { it.displayOrder } ?: -1
            repository.insertWallet(
                Wallet(
                    name = name,
                    type = type,
                    balance = initialBalance,
                    colorHex = colorHex,
                    iconName = iconName,
                    displayOrder = maxOrder + 1
                )
            )
        }
    }

    fun moveWalletUp(wallet: Wallet) {
        viewModelScope.launch {
            val list = allWallets.value
            val needsReassign = list.map { it.displayOrder }.toSet().size < list.size
            val sortedList = if (needsReassign) {
                list.sortedBy { it.id }.mapIndexed { index, w -> 
                    val updated = w.copy(displayOrder = index)
                    repository.updateWallet(updated)
                    updated
                }
            } else {
                list.sortedWith(compareBy<Wallet> { it.displayOrder }.thenBy { it.id })
            }
            
            val index = sortedList.indexOfFirst { it.id == wallet.id }
            if (index > 0) {
                val prev = sortedList[index - 1]
                val prevOrder = prev.displayOrder
                val currOrder = sortedList[index].displayOrder
                
                repository.updateWallet(prev.copy(displayOrder = currOrder))
                repository.updateWallet(sortedList[index].copy(displayOrder = prevOrder))
            }
        }
    }

    fun moveWalletDown(wallet: Wallet) {
        viewModelScope.launch {
            val list = allWallets.value
            val needsReassign = list.map { it.displayOrder }.toSet().size < list.size
            val sortedList = if (needsReassign) {
                list.sortedBy { it.id }.mapIndexed { index, w -> 
                    val updated = w.copy(displayOrder = index)
                    repository.updateWallet(updated)
                    updated
                }
            } else {
                list.sortedWith(compareBy<Wallet> { it.displayOrder }.thenBy { it.id })
            }
            
            val index = sortedList.indexOfFirst { it.id == wallet.id }
            if (index >= 0 && index < sortedList.lastIndex) {
                val next = sortedList[index + 1]
                val nextOrder = next.displayOrder
                val currOrder = sortedList[index].displayOrder
                
                repository.updateWallet(next.copy(displayOrder = currOrder))
                repository.updateWallet(sortedList[index].copy(displayOrder = nextOrder))
            }
        }
    }

    fun deleteWallet(wallet: Wallet) {
        viewModelScope.launch {
            repository.deleteWallet(wallet)
        }
    }

    fun updateWalletsOrder(orderedList: List<Wallet>) {
        viewModelScope.launch {
            orderedList.forEachIndexed { index, wallet ->
                if (wallet.displayOrder != index) {
                    repository.updateWallet(wallet.copy(displayOrder = index))
                }
            }
        }
    }

    // --- TRANSACTIONS SERVICES ---
    fun addTransaction(
        walletId: Int,
        type: String,
        amount: Double,
        categoryName: String,
        note: String,
        timestamp: Long,
        isRecurring: Boolean = false,
        recurrencePeriod: String = "NONE"
    ) {
        viewModelScope.launch {
            val wallet = repository.getWalletById(walletId) ?: return@launch
            val cat = getCategoryByName(categoryName)
            val tx = Transaction(
                walletId = walletId,
                walletName = wallet.name,
                type = type,
                amount = amount,
                categoryName = categoryName,
                categoryIcon = cat.iconName,
                categoryColor = cat.colorHex,
                note = note.ifEmpty { categoryName },
                timestamp = timestamp,
                isRecurring = isRecurring,
                recurrencePeriod = recurrencePeriod
            )
            repository.insertTransaction(tx)
        }
    }

    fun updateTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(tx)
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    // --- BUDGETS SERVICES ---
    fun addBudget(categoryName: String, limitAmount: Double, month: String) {
        viewModelScope.launch {
            val cat = getCategoryByName(categoryName)
            
            // Calculate current month's spending for this category to seed spentAmount!
            val txs = repository.allTransactions.firstOrNull() ?: emptyList()
            val spent = txs.filter {
                it.type == "EXPENSE" && it.categoryName == categoryName && isTimestampInMonth(it.timestamp, month)
            }.sumOf { it.amount }

            repository.insertBudget(
                Budget(
                    categoryName = categoryName,
                    categoryIcon = cat.iconName,
                    categoryColor = cat.colorHex,
                    limitAmount = limitAmount,
                    spentAmount = spent,
                    month = month
                )
            )
        }
    }

    fun updateBudget(budget: Budget) {
        viewModelScope.launch {
            repository.updateBudget(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    private fun isTimestampInMonth(timestamp: Long, monthStr: String): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val checkMonth = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        return checkMonth == monthStr
    }

    // --- SAVINGS GOALS SERVICES ---
    fun addSavingsGoal(name: String, targetAmount: Double, currentAmount: Double, targetDate: Long, note: String) {
        viewModelScope.launch {
            repository.insertSavingsGoal(
                SavingsGoal(
                    name = name,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    targetDate = targetDate,
                    note = note
                )
            )
        }
    }

    fun updateSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.updateSavingsGoal(goal)
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    // --- STATE FILTERS SETTERS ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: String) {
        _selectedTypeFilter.value = type
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun setDateFilterRange(start: Long?, end: Long?) {
        _filterStartDate.value = start
        _filterEndDate.value = end
    }

    fun setActiveMonth(month: String) {
        _activeMonth.value = month
    }

    // --- GOOGLE CLOUD SYNC SERVICES ---
    fun saveGoogleSheetUrl(url: String) {
        viewModelScope.launch {
            repository.saveSetting("google_sheet_url", url)
            _googleSheetUrl.value = url
        }
    }

    fun saveGoogleDocUrl(url: String) {
        viewModelScope.launch {
            repository.saveSetting("google_doc_url", url)
            _googleDocUrl.value = url
        }
    }

    fun saveGoogleAppsScriptUrl(url: String) {
        viewModelScope.launch {
            repository.saveSetting("google_apps_script_url", url)
            _googleAppsScriptUrl.value = url
        }
    }

    fun clearSyncLogs() {
        _syncStatus.value = "IDLE"
        _syncProgressLogs.value = emptyList()
    }

    fun syncToGoogle(type: String, context: android.content.Context) {
        viewModelScope.launch {
            _syncStatus.value = "SYNCING"
            val logs = mutableListOf<String>()
            fun addLog(msg: String) {
                logs.add("[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}] $msg")
                _syncProgressLogs.value = logs.toList()
            }

            addLog("Bắt đầu tiến trình đồng bộ lên ứng dụng Google $type...")

            val targetUrl = if (type == "SHEETS") _googleSheetUrl.value else _googleDocUrl.value
            if (targetUrl.isBlank()) {
                addLog("Thao tác thất bại: Quý khách chưa liên kết link Google $type.")
                _syncStatus.value = "ERROR"
                return@launch
            }

            // Extract spreadsheet ID or doc ID
            val regex = "/d/([a-zA-Z0-9-_]+)".toRegex()
            val matchResult = regex.find(targetUrl)
            val fileId = matchResult?.groupValues?.get(1)
            if (fileId == null) {
                addLog("Thao tác thất bại: Định dạng đường dẫn Google $type không hợp lệ.")
                _syncStatus.value = "ERROR"
                return@launch
            }
            addLog("Xác định ID tài liệu: ...${fileId.takeLast(8)}")

            // Gather transaction data
            val txs = allTransactions.value
            addLog("Hệ thống nén thành công ${txs.size} giao dịch từ bộ nhớ cục bộ.")

            val webAppUrl = _googleAppsScriptUrl.value
            if (webAppUrl.isNotBlank() && (webAppUrl.startsWith("http://") || webAppUrl.startsWith("https://"))) {
                addLog("Phát hiện Web App Proxy. Đang kết nối API Google...")
                try {
                    val root = org.json.JSONObject()
                    root.put("type", type)
                    root.put("sheetUrl", _googleSheetUrl.value)
                    root.put("docUrl", _googleDocUrl.value)
                    
                    val txArray = org.json.JSONArray()
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
                    txs.forEach { tx ->
                        val obj = org.json.JSONObject()
                        obj.put("id", tx.id)
                        obj.put("walletName", tx.walletName)
                        obj.put("type", tx.type)
                        obj.put("amount", tx.amount)
                        obj.put("categoryName", tx.categoryName)
                        obj.put("note", tx.note ?: "")
                        obj.put("formattedDate", sdf.format(Date(tx.timestamp)))
                        txArray.put(obj)
                    }
                    root.put("transactions", txArray)

                    // Execute request with okhttp
                    val okHttpClient = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    val body = okhttp3.RequestBody.create(
                        "application/json; charset=utf-8".toMediaTypeOrNull(),
                        root.toString()
                    )

                    val request = okhttp3.Request.Builder()
                        .url(webAppUrl)
                        .post(body)
                        .build()

                    addLog("Đang tải dữ liệu lên Máy chủ Cloud của bạn...")
                    withContext(Dispatchers.IO) {
                        okHttpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val bodyStr = response.body?.string() ?: "{}"
                                val respObj = org.json.JSONObject(bodyStr)
                                val successResult = respObj.optBoolean("success", false)
                                if (successResult) {
                                    val nowStr = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(Date())
                                    if (type == "SHEETS") {
                                        repository.saveSetting("google_sheet_last_sync", nowStr)
                                        _googleSheetLastSync.value = nowStr
                                    } else {
                                        repository.saveSetting("google_doc_last_sync", nowStr)
                                        _googleDocLastSync.value = nowStr
                                    }
                                    withContext(Dispatchers.Main) {
                                        addLog("Thành công: " + respObj.optString("message", "Đồng bộ thành công!"))
                                        _syncStatus.value = "SUCCESS"
                                    }
                                } else {
                                    val errorMsg = respObj.optString("error", "Lỗi từ Apps Script của bạn")
                                    withContext(Dispatchers.Main) {
                                        addLog("Lỗi Sync Cloud: $errorMsg")
                                        _syncStatus.value = "ERROR"
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    addLog("Phản hồi lỗi cổng API: HTTP ${response.code}")
                                    _syncStatus.value = "ERROR"
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    addLog("Lỗi đường truyền hoặc phản hồi: ${e.message}")
                    _syncStatus.value = "ERROR"
                }
            } else {
                // FALLBACK: Offline format copy-paste helper
                addLog("Chế độ: Đồng bộ Thông minh (Không dùng Web Script).")
                addLog("Đang xây dựng báo cáo chuyên sâu...")
                
                val content = StringBuilder()
                if (type == "SHEETS") {
                    content.append("ID,Ví,Loại,Số tiền (vnđ),Hạng mục,Ghi chú,Thời gian\n")
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
                    txs.forEach { tx ->
                        val cleanNote = (tx.note ?: "").replace("\"", "\"\"")
                        content.append("${tx.id},\"${tx.walletName}\",${if (tx.type == "EXPENSE") "Chi" else "Thu"},${tx.amount},\"${tx.categoryName}\",\"$cleanNote\",\"${sdf.format(Date(tx.timestamp))}\"\n")
                    }
                } else {
                    content.append("==========================================\n")
                    content.append("BÁO CÁO GIAO DỊCH TỪ SỔ CHI TIÊU\n")
                    content.append("Mốc thời gian tải: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}\n")
                    content.append("==========================================\n\n")
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
                    var totalIn = 0.0
                    var totalOut = 0.0
                    txs.forEach { tx ->
                        val sign = if (tx.type == "EXPENSE") { totalOut += tx.amount; "-" } else { totalIn += tx.amount; "+" }
                        content.append("${sdf.format(Date(tx.timestamp))} | $sign${FormatHelper.formatVND(tx.amount)} | [${tx.walletName}] | ${tx.categoryName} ${if (tx.note.isNullOrBlank()) "" else "| " + tx.note}\n")
                    }
                    content.append("------------------------------------------\n")
                    content.append("TỔNG THU: +${FormatHelper.formatVND(totalIn)}\n")
                    content.append("TỔNG CHI: -${FormatHelper.formatVND(totalOut)}\n")
                    content.append("SỐ DỰ DỰ KIẾN: ${FormatHelper.formatVND(totalIn - totalOut)}\n")
                }

                try {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Sổ Chi Tiêu Sync Data", content.toString())
                    clipboard.setPrimaryClip(clip)
                    addLog("Đã định dạng thành công dữ liệu báo cáo!")
                    addLog("Đã tự động sao chép (Copy) toàn bộ nội dung vào Clipboard của bạn!")
                    addLog("HƯỚNG DẪN: Hãy nhấn nút mở link Tài liệu, dán vào tài liệu của bạn. Dữ liệu sẽ lập tức xuất hiện.")
                    
                    val nowStr = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(Date())
                    if (type == "SHEETS") {
                        repository.saveSetting("google_sheet_last_sync", nowStr)
                        _googleSheetLastSync.value = nowStr
                    } else {
                        repository.saveSetting("google_doc_last_sync", nowStr)
                        _googleDocLastSync.value = nowStr
                    }
                    _syncStatus.value = "SUCCESS"
                } catch (e: Exception) {
                    addLog("Lỗi chuyển dịch Clipboard: ${e.message}")
                    _syncStatus.value = "ERROR"
                }
            }
        }
    }
}
