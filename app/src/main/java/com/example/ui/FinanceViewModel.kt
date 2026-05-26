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

data class NotificationLog(
    val timestamp: Long,
    val title: String,
    val text: String,
    val bankName: String,
    val amount: Double,
    val type: String,
    val note: String,
    val walletName: String,
    val status: String // "AUTO_ADDED", "FAILED_PARSE", "NO_WALLET"
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    // Notification listener flows
    private val _notificationReaderEnabled = MutableStateFlow(false)
    val notificationReaderEnabled: StateFlow<Boolean> = _notificationReaderEnabled.asStateFlow()

    private val _notificationLogs = MutableStateFlow<List<NotificationLog>>(emptyList())
    val notificationLogs: StateFlow<List<NotificationLog>> = _notificationLogs.asStateFlow()

    // Local Backup Flow Variables
    private val _localBackupLastTime = MutableStateFlow("Chưa sao lưu")
    val localBackupLastTime: StateFlow<String> = _localBackupLastTime.asStateFlow()

    private val _localBackupCount = MutableStateFlow(0)
    val localBackupCount: StateFlow<Int> = _localBackupCount.asStateFlow()

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
            loadNotificationSettings()
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

        val lastTime = repository.getSetting("local_backup_last_time")?.value ?: "Chưa sao lưu"
        _localBackupLastTime.value = lastTime
        
        try {
            val context = getApplication<Application>()
            val backupDir = context.getExternalFilesDir("Backups")
            val filesList = backupDir?.listFiles { _, name -> name.endsWith(".json") }
            _localBackupCount.value = filesList?.size ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- BANK NOTIFICATION READER LOGIC ---
    private suspend fun loadNotificationSettings() {
        val enabledSetting = repository.getSetting("notification_reader_enabled")
        _notificationReaderEnabled.value = enabledSetting?.value == "true"
        loadNotificationLogs()
    }

    fun loadNotificationLogs() {
        viewModelScope.launch {
            val logsSetting = repository.getSetting("notification_logs")?.value ?: "[]"
            val list = mutableListOf<NotificationLog>()
            var modified = false
            val now = System.currentTimeMillis()
            val limitTime = now - (3L * 24 * 60 * 60 * 1000) // 3 days in ms
            try {
                val array = org.json.JSONArray(logsSetting)
                val newJSONArray = org.json.JSONArray()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val timestamp = obj.optLong("timestamp", now)
                    if (timestamp >= limitTime) {
                        list.add(
                            NotificationLog(
                                timestamp = timestamp,
                                title = obj.optString("title", ""),
                                text = obj.optString("text", ""),
                                bankName = obj.optString("bankName", ""),
                                amount = obj.optDouble("amount", 0.0),
                                type = obj.optString("type", "EXPENSE"),
                                note = obj.optString("note", ""),
                                walletName = obj.optString("walletName", ""),
                                status = obj.optString("status", "")
                            )
                        )
                        newJSONArray.put(obj)
                    } else {
                        modified = true
                    }
                }
                if (modified) {
                    repository.saveSetting("notification_logs", newJSONArray.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _notificationLogs.value = list
        }
    }

    fun setNotificationReaderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("notification_reader_enabled", enabled.toString())
            _notificationReaderEnabled.value = enabled
        }
    }

    fun clearNotificationLogs() {
        viewModelScope.launch {
            repository.saveSetting("notification_logs", "[]")
            _notificationLogs.value = emptyList()
        }
    }

    fun simulateBankNotification(title: String, text: String, packageName: String) {
        viewModelScope.launch {
            try {
                val parsed = com.example.service.NotificationParser.parse(title, text, packageName)
                
                val wallets = repository.allWallets.firstOrNull() ?: emptyList()
                val matchedWallet = wallets.find { it.name.lowercase().contains(parsed.bankName.lowercase()) }
                    ?: wallets.find { it.name.lowercase().contains(parsed.detectedWalletName.lowercase()) }
                    ?: wallets.find { it.type == "BANK" }
                    ?: wallets.firstOrNull()

                if (matchedWallet != null && parsed.success) {
                    val categoryName = when (parsed.type) {
                        "INCOME" -> {
                            val lowerNote = parsed.note.lowercase()
                            when {
                                lowerNote.contains("luong") || lowerNote.contains("salary") -> "Lương"
                                lowerNote.contains("thuong") || lowerNote.contains("gift") || lowerNote.contains("tang") -> "Thưởng"
                                lowerNote.contains("ban hang") || lowerNote.contains("kinh doanh") -> "Kinh doanh"
                                else -> "Khác"
                            }
                        }
                        else -> {
                            val lowerNote = parsed.note.lowercase()
                            when {
                                lowerNote.contains("an uong") || lowerNote.contains("restaurant") || lowerNote.contains("coffee") || lowerNote.contains("milktea") || lowerNote.contains("tra sua") -> "Ăn uống"
                                lowerNote.contains("grab") || lowerNote.contains("xe") || lowerNote.contains("petrol") || lowerNote.contains("xang") -> "Di chuyển"
                                lowerNote.contains("shopee") || lowerNote.contains("tiki") || lowerNote.contains("lazada") || lowerNote.contains("mua sam") -> "Mua sắm"
                                lowerNote.contains("cuoc") || lowerNote.contains("dien nuoc") || lowerNote.contains("hoa don") || lowerNote.contains("tien dien") -> "Hóa đơn"
                                lowerNote.contains("cgv") || lowerNote.contains("netflix") || lowerNote.contains("giai tri") -> "Giải trí"
                                else -> "Khác"
                            }
                        }
                    }

                    val catDetails = Categories.getByCategoryName(categoryName)
                    val tx = Transaction(
                        walletId = matchedWallet.id,
                        walletName = matchedWallet.name,
                        type = parsed.type,
                        amount = parsed.amount,
                        categoryName = categoryName,
                        categoryIcon = catDetails.iconName,
                        categoryColor = catDetails.colorHex,
                        note = parsed.note,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertTransaction(tx)
                    
                    // Log success
                    saveLocalLog(title, text, parsed, "AUTO_ADDED", matchedWallet.name)
                } else {
                    val walletLabel = matchedWallet?.name ?: "Không tìm thấy ví"
                    saveLocalLog(title, text, parsed, if (!parsed.success) "FAILED_PARSE" else "NO_WALLET", walletLabel)
                }
                
                // Reload logs
                loadNotificationLogs()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun saveLocalLog(
        title: String,
        text: String,
        parsed: com.example.service.NotificationParser.ParsedNotification,
        status: String,
        walletName: String?
    ) {
        val logsSetting = repository.getSetting("notification_logs")?.value ?: "[]"
        val jsonArray = try { org.json.JSONArray(logsSetting) } catch (e: Exception) { org.json.JSONArray() }

        val logObj = org.json.JSONObject()
        logObj.put("timestamp", System.currentTimeMillis())
        logObj.put("title", title)
        logObj.put("text", text)
        logObj.put("bankName", parsed.bankName)
        logObj.put("amount", parsed.amount)
        logObj.put("type", parsed.type)
        logObj.put("note", parsed.note)
        logObj.put("walletName", walletName ?: parsed.detectedWalletName)
        logObj.put("status", status)

        val newList = org.json.JSONArray()
        newList.put(logObj)
        val limitTime = System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000) // 3 days in ms
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val timestamp = item.optLong("timestamp", 0L)
            if (timestamp >= limitTime) {
                newList.put(item)
            }
        }

        repository.saveSetting("notification_logs", newList.toString())
    }

    fun addManualTransactionFromLog(log: NotificationLog, walletId: Int, categoryName: String) {
        viewModelScope.launch {
            val wallet = repository.getWalletById(walletId) ?: return@launch
            val cat = getCategoryByName(categoryName)
            val tx = Transaction(
                walletId = walletId,
                walletName = wallet.name,
                type = log.type,
                amount = log.amount,
                categoryName = categoryName,
                categoryIcon = cat.iconName,
                categoryColor = cat.colorHex,
                note = log.note.ifEmpty { "Ghi từ thông báo" },
                timestamp = log.timestamp
            )
            repository.insertTransaction(tx)
        }
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
    fun clearSyncLogs() {
        _syncStatus.value = ""
        _syncProgressLogs.value = emptyList()
    }

    fun exportLocalBackup(context: android.content.Context) {
        viewModelScope.launch {
            _syncStatus.value = "SYNCING"
            val logs = mutableListOf<String>()
            fun addLog(msg: String) {
                logs.add("[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}] $msg")
                _syncProgressLogs.value = logs.toList()
            }

            try {
                addLog("Bắt đầu tạo bản sao lưu cục bộ...")

                val walletsList = allWallets.value
                val transactionsList = allTransactions.value
                val budgetsList = allBudgets.value
                val savingsGoalsList = allSavingsGoals.value

                addLog("Đang nén dữ liệu: ${walletsList.size} ví, ${transactionsList.size} giao dịch, ${budgetsList.size} ngân sách, ${savingsGoalsList.size} mục tiêu tích lũy.")

                val root = org.json.JSONObject()
                root.put("version", 1)
                root.put("backup_timestamp", System.currentTimeMillis())

                // Wallets
                val walletsArray = org.json.JSONArray()
                walletsList.forEach { w ->
                    val obj = org.json.JSONObject()
                    obj.put("id", w.id)
                    obj.put("name", w.name)
                    obj.put("type", w.type)
                    obj.put("balance", w.balance)
                    obj.put("colorHex", w.colorHex)
                    obj.put("iconName", w.iconName)
                    obj.put("displayOrder", w.displayOrder)
                    walletsArray.put(obj)
                }
                root.put("wallets", walletsArray)

                // Transactions
                val transactionsArray = org.json.JSONArray()
                transactionsList.forEach { t ->
                    val obj = org.json.JSONObject()
                    obj.put("id", t.id)
                    obj.put("walletId", t.walletId)
                    obj.put("walletName", t.walletName)
                    obj.put("type", t.type)
                    obj.put("amount", t.amount)
                    obj.put("categoryName", t.categoryName)
                    obj.put("categoryIcon", t.categoryIcon)
                    obj.put("categoryColor", t.categoryColor)
                    obj.put("note", t.note)
                    obj.put("timestamp", t.timestamp)
                    obj.put("isRecurring", t.isRecurring)
                    obj.put("recurrencePeriod", t.recurrencePeriod)
                    transactionsArray.put(obj)
                }
                root.put("transactions", transactionsArray)

                // Budgets
                val budgetsArray = org.json.JSONArray()
                budgetsList.forEach { b ->
                    val obj = org.json.JSONObject()
                    obj.put("id", b.id)
                    obj.put("categoryName", b.categoryName)
                    obj.put("categoryIcon", b.categoryIcon)
                    obj.put("categoryColor", b.categoryColor)
                    obj.put("limitAmount", b.limitAmount)
                    obj.put("spentAmount", b.spentAmount)
                    obj.put("month", b.month)
                    budgetsArray.put(obj)
                }
                root.put("budgets", budgetsArray)

                // Savings Goals
                val savingsGoalsArray = org.json.JSONArray()
                savingsGoalsList.forEach { s ->
                    val obj = org.json.JSONObject()
                    obj.put("id", s.id)
                    obj.put("name", s.name)
                    obj.put("targetAmount", s.targetAmount)
                    obj.put("currentAmount", s.currentAmount)
                    obj.put("targetDate", s.targetDate)
                    obj.put("note", s.note ?: "")
                    savingsGoalsArray.put(obj)
                }
                root.put("savingsGoals", savingsGoalsArray)

                // Custom Categories
                val categoriesSetting = repository.getSetting("custom_categories")?.value ?: ""
                root.put("customCategories", categoriesSetting)

                val jsonString = root.toString(2)

                // Save to context.getExternalFilesDir("Backups")
                val backupDir = context.getExternalFilesDir("Backups") ?: context.filesDir
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                val formatSdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val filename = "SoChiTieu_Backup_${formatSdf.format(Date())}.json"
                val file = java.io.File(backupDir, filename)

                withContext(Dispatchers.IO) {
                    file.writeText(jsonString)
                }

                addLog("Đã viết file hệ thống thành công: $filename")
                addLog("Đường dẫn file hệ thống: ${file.absolutePath}")

                // Save copies in the Public Downloads folder under "SoChiTieuBackups" as well
                var publicSavedPath: String? = null
                withContext(Dispatchers.IO) {
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            val resolver = context.contentResolver
                            val contentValues = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/SoChiTieuBackups")
                            }
                            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                            if (uri != null) {
                                resolver.openOutputStream(uri)?.use { outputStream ->
                                    outputStream.write(jsonString.toByteArray())
                                }
                                publicSavedPath = "Bộ nhớ trong > Download > SoChiTieuBackups > $filename"
                            }
                        } else {
                            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                            val subDir = java.io.File(downloadsDir, "SoChiTieuBackups")
                            if (!subDir.exists()) {
                                subDir.mkdirs()
                            }
                            val publicFile = java.io.File(subDir, filename)
                            publicFile.writeText(jsonString)
                            publicSavedPath = publicFile.absolutePath
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (publicSavedPath != null) {
                    addLog("✨ Đã tự động nhân bản file sao lưu công khai vào thư mục Downloads của bạn!")
                    addLog("Đường dẫn: $publicSavedPath")
                }

                val nowStr = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(Date())
                repository.saveSetting("local_backup_last_time", nowStr)
                _localBackupLastTime.value = nowStr

                // Dọn dẹp các bản sao lưu cũ, chỉ giữ lại tối đa 3 bản gần đây nhất (bao gồm cả bản hiện tại)
                val allBackupFiles = backupDir.listFiles { _, name -> name.endsWith(".json") }
                if (allBackupFiles != null && allBackupFiles.size > 3) {
                    val sortedFiles = allBackupFiles.sortedByDescending { it.lastModified() }
                    for (i in 3 until sortedFiles.size) {
                        try {
                            val fileToDelete = sortedFiles[i]
                            val deleted = fileToDelete.delete()
                            if (deleted) {
                                addLog("🗑️ Đã xóa bản sao lưu cũ thừa: ${fileToDelete.name}")
                            }
                        } catch (ex: Exception) {
                            addLog("⚠️ Lỗi khi tự động xóa file cũ: ${ex.message}")
                        }
                    }
                }

                val filesList = backupDir.listFiles { _, name -> name.endsWith(".json") }
                _localBackupCount.value = filesList?.size ?: 0

                addLog("🎉 SAO LƯU THÀNH CÔNG!")
                _syncStatus.value = "SUCCESS"

                // Launch Share intent
                withContext(Dispatchers.Main) {
                    try {
                        val fileUri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Sổ Chi Tiêu Backup File")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        val chooserIntent = android.content.Intent.createChooser(shareIntent, "Chia sẻ & Sao lưu dữ liệu JSON")
                        chooserIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooserIntent)
                    } catch (e: Exception) {
                        // Fallback
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Sổ Chi Tiêu JSON Backup", jsonString)
                        clipboard.setPrimaryClip(clip)
                        addLog("⚠️ Không thể khởi chạy trình FileProvider. Dữ liệu JSON đã được copy vào bộ nhớ tạm để thay thế.")
                        android.widget.Toast.makeText(context, "Đã sao chép nội dung JSON vào Clipboard!", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                addLog("Sao lưu lỗi: ${e.localizedMessage ?: "Lỗi không xác định"}")
                _syncStatus.value = "ERROR"
                e.printStackTrace()
            }
        }
    }

    fun openBackupFolder(context: android.content.Context) {
        viewModelScope.launch {
            _syncStatus.value = "SYNCING"
            val logs = mutableListOf<String>()
            fun addLog(msg: String) {
                logs.add("[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}] $msg")
                _syncProgressLogs.value = logs.toList()
            }

            try {
                addLog("Đang kết nối đến thư mục lưu trữ...")
                val backupDir = context.getExternalFilesDir("Backups") ?: context.filesDir
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                val publicPath = "Download/SoChiTieuBackups"
                addLog("Thư mục sao lưu công khai: $publicPath")

                // Copy path to clipboard as backup option
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Đường dẫn sao lưu Sổ Chi Tiêu", publicPath)
                clipboard.setPrimaryClip(clip)
                addLog("📋 ĐÃ COPY ĐƯỜNG DẪN 'Download/SoChiTieuBackups' VÀO CLIPBOARD.")

                addLog("Đang kích hoạt trình quản lý tệp trên thiết bị...")

                try {
                    // Start an intent to view the Download directory
                    val intent = android.content.Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    addLog("🚀 Mở thư mục Tải xuống thành công!")
                } catch (e: Exception) {
                    try {
                        val fileUri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            backupDir
                        )
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(fileUri, "resource/folder")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        addLog("🚀 Mở thư mục ứng dụng thành công!")
                    } catch (ex: Exception) {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                                type = "*/*"
                                addCategory(android.content.Intent.CATEGORY_OPENABLE)
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            addLog("🚀 Mở trình chọn tệp hệ thống thành công!")
                        } catch (ey: Exception) {
                            addLog("⚠️ Thiết bị thiếu ứng dụng File Manager tương ứng.")
                        }
                    }
                }

                addLog("\n💡 HƯỚNG DẪN TÌM THỦ CÔNG:")
                addLog("1. Mở ứng dụng 'Tệp' (Files by Google), 'Tệp của tôi' hoặc bất kỳ app quản lý file nào.")
                addLog("2. Điều hướng vào: Bộ nhớ trong > Download > SoChiTieuBackups")
                addLog("3. Tại đây bạn sẽ thấy tất cả file backup .json đã được tạo.")
                
                _syncStatus.value = "SUCCESS"
            } catch (e: Exception) {
                addLog("Mở thư mục lỗi: ${e.localizedMessage ?: "Lỗi không xác định"}")
                _syncStatus.value = "ERROR"
                e.printStackTrace()
            }
        }
    }

    fun clearAllData(context: android.content.Context) {
        viewModelScope.launch {
            try {
                _syncStatus.value = "SYNCING"
                val logs = mutableListOf<String>()
                fun addLog(msg: String) {
                    logs.add("[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}] $msg")
                    _syncProgressLogs.value = logs.toList()
                }
                addLog("Khởi chạy tiến trình xóa sạch dữ liệu...")
                repository.clearAllData()
                addLog("Xóa sạch toàn bộ giao dịch, ví, ngân sách thành công!")
                _syncStatus.value = "SUCCESS"
                android.widget.Toast.makeText(context, "Đã xóa toàn bộ dữ liệu!", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                _syncStatus.value = "ERROR"
                e.printStackTrace()
            }
        }
    }

    fun importLocalBackup(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            _syncStatus.value = "SYNCING"
            val logs = mutableListOf<String>()
            fun addLog(msg: String) {
                logs.add("[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}] $msg")
                _syncProgressLogs.value = logs.toList()
            }

            try {
                addLog("Bắt đầu giải nén file sao lưu...")
                val contentResolver = context.contentResolver
                val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }

                if (jsonString.isNullOrBlank()) {
                    addLog("Lỗi: File trống hoặc không đọc được.")
                    _syncStatus.value = "ERROR"
                    return@launch
                }

                val root = org.json.JSONObject(jsonString)
                val version = root.optInt("version", 1)
                addLog("Phân tích file phiên bản: $version")

                // Start clear databases first
                addLog("Khởi tạo tiến trình dọn dẹp cơ sở dữ liệu...")
                repository.clearAllData()

                // 1. Wallets
                val walletsArray = root.optJSONArray("wallets")
                if (walletsArray != null) {
                    addLog("Đang nhập lại ${walletsArray.length()} ví tài chính...")
                    for (i in 0 until walletsArray.length()) {
                        val obj = walletsArray.getJSONObject(i)
                        val w = com.example.data.Wallet(
                            id = obj.optInt("id", 0),
                            name = obj.optString("name", "Ví"),
                            type = obj.optString("type", "CASH"),
                            balance = obj.optDouble("balance", 0.0),
                            colorHex = obj.optString("colorHex", "#9E9E9E"),
                            iconName = obj.optString("iconName", "AccountBalanceWallet"),
                            displayOrder = obj.optInt("displayOrder", 0)
                        )
                        repository.insertWalletDirect(w)
                    }
                }

                // 2. Transactions
                val transactionsArray = root.optJSONArray("transactions")
                if (transactionsArray != null) {
                    addLog("Đang nhập lại ${transactionsArray.length()} giao dịch tài chính...")
                    for (i in 0 until transactionsArray.length()) {
                        val obj = transactionsArray.getJSONObject(i)
                        val t = com.example.data.Transaction(
                            id = obj.optInt("id", 0),
                            walletId = obj.optInt("walletId", 0),
                            walletName = obj.optString("walletName", ""),
                            type = obj.optString("type", "EXPENSE"),
                            amount = obj.optDouble("amount", 0.0),
                            categoryName = obj.optString("categoryName", ""),
                            categoryIcon = obj.optString("categoryIcon", ""),
                            categoryColor = obj.optString("categoryColor", ""),
                            note = obj.optString("note", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isRecurring = obj.optBoolean("isRecurring", false),
                            recurrencePeriod = obj.optString("recurrencePeriod", "NONE")
                        )
                        repository.insertTransactionDirect(t)
                    }
                }

                // 3. Budgets
                val budgetsArray = root.optJSONArray("budgets")
                if (budgetsArray != null) {
                    addLog("Đang nhập lại ${budgetsArray.length()} hạn mức ngân sách...")
                    for (i in 0 until budgetsArray.length()) {
                        val obj = budgetsArray.getJSONObject(i)
                        val b = com.example.data.Budget(
                            id = obj.optInt("id", 0),
                            categoryName = obj.optString("categoryName", ""),
                            categoryIcon = obj.optString("categoryIcon", ""),
                            categoryColor = obj.optString("categoryColor", ""),
                            limitAmount = obj.optDouble("limitAmount", 0.0),
                            spentAmount = obj.optDouble("spentAmount", 0.0),
                            month = obj.optString("month", "")
                        )
                        repository.insertBudgetDirect(b)
                    }
                }

                // 4. Savings Goals
                val savingsGoalsArray = root.optJSONArray("savingsGoals")
                if (savingsGoalsArray != null) {
                    addLog("Đang nhập lại ${savingsGoalsArray.length()} mục tiêu tích lũy...")
                    for (i in 0 until savingsGoalsArray.length()) {
                        val obj = savingsGoalsArray.getJSONObject(i)
                        val s = com.example.data.SavingsGoal(
                            id = obj.optInt("id", 0),
                            name = obj.optString("name", "Mục tiêu"),
                            targetAmount = obj.optDouble("targetAmount", 0.0),
                            currentAmount = obj.optDouble("currentAmount", 0.0),
                            targetDate = obj.optLong("targetDate", System.currentTimeMillis()),
                            note = obj.optString("note", "")
                        )
                        repository.insertSavingsGoalDirect(s)
                    }
                }

                // 5. Custom Categories
                val customCategories = root.optString("customCategories", "")
                if (customCategories.isNotEmpty()) {
                    repository.saveSetting("custom_categories", customCategories)
                }

                addLog("🎉 KHÔI PHỤC DỮ LIỆU THÀNH CÔNG HOÀN TOÀN!")
                _syncStatus.value = "SUCCESS"
                android.widget.Toast.makeText(context, "Khôi phục dữ liệu từ bản sao lưu thành công!", android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                addLog("Lỗi khôi phục: ${e.localizedMessage ?: "File JSON lỗi cấu trúc."}")
                _syncStatus.value = "ERROR"
                e.printStackTrace()
            }
        }
    }
}
