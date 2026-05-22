package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

data class FilterCriteria(
    val query: String,
    val type: String,
    val category: String,
    val start: Long?,
    val end: Long?
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    // Base flows
    val allWallets: StateFlow<List<Wallet>>
    val allTransactions: StateFlow<List<Transaction>>
    val allBudgets: StateFlow<List<Budget>>
    val allSavingsGoals: StateFlow<List<SavingsGoal>>

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
            allTransactions,
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
            repository.insertWallet(
                Wallet(
                    name = name,
                    type = type,
                    balance = initialBalance,
                    colorHex = colorHex,
                    iconName = iconName
                )
            )
        }
    }

    fun deleteWallet(wallet: Wallet) {
        viewModelScope.launch {
            repository.deleteWallet(wallet)
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
            val cat = Categories.getByCategoryName(categoryName)
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
            val cat = Categories.getByCategoryName(categoryName)
            
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
}
