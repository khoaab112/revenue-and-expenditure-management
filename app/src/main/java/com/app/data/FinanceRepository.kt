package com.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

import kotlinx.coroutines.flow.first
import org.json.JSONObject

class FinanceRepository(private val dao: FinanceDao) {

    // --- Wallets ---
    val allWallets: Flow<List<Wallet>> = dao.getAllWallets()

    suspend fun getWalletById(id: Int): Wallet? = dao.getWalletById(id)

    suspend fun insertWallet(wallet: Wallet): Long = dao.insertWallet(wallet)

    suspend fun updateWallet(wallet: Wallet) = dao.updateWallet(wallet)

    suspend fun deleteWallet(wallet: Wallet) = dao.deleteWallet(wallet)

    // --- Transactions ---
    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactions()

    fun getTransactionsByWallet(walletId: Int): Flow<List<Transaction>> =
        dao.getTransactionsByWallet(walletId)

    suspend fun getTransactionById(id: Int): Transaction? = dao.getTransactionById(id)

    suspend fun insertTransaction(transaction: Transaction): Long {
        // 1. Save Transaction
        val id = dao.insertTransaction(transaction)
        
        // 2. Adjust Wallet Balance
        val wallet = dao.getWalletById(transaction.walletId)
        if (wallet != null) {
            val newBalance = when (transaction.type) {
                "EXPENSE" -> wallet.balance - transaction.amount
                "INCOME" -> wallet.balance + transaction.amount
                "TRANSFER" -> wallet.balance - transaction.amount
                "ADJUSTMENT" -> {
                    if (transaction.note.contains("tăng")) {
                        wallet.balance + transaction.amount
                    } else {
                        wallet.balance - transaction.amount
                    }
                }
                else -> wallet.balance
            }
            dao.updateWallet(wallet.copy(balance = newBalance))
        }

        // Handle TRANSFER destination wallet
        if (transaction.type == "TRANSFER" && transaction.destinationWalletId != null) {
            val destWallet = dao.getWalletById(transaction.destinationWalletId)
            if (destWallet != null) {
                dao.updateWallet(destWallet.copy(balance = destWallet.balance + transaction.amount))
            }
        }

        // 3. Update Budget if it's an Expense
        if (transaction.type == "EXPENSE") {
            updateBudgetSpending(transaction.categoryName, transaction.amount, transaction.timestamp, isAddition = true)
        }

        return id
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        // 1. Revert Wallet Balance
        val wallet = dao.getWalletById(transaction.walletId)
        if (wallet != null) {
            val newBalance = when (transaction.type) {
                "EXPENSE" -> wallet.balance + transaction.amount
                "INCOME" -> wallet.balance - transaction.amount
                "TRANSFER" -> wallet.balance + transaction.amount
                "ADJUSTMENT" -> {
                    if (transaction.note.contains("tăng")) {
                        wallet.balance - transaction.amount
                    } else {
                        wallet.balance + transaction.amount
                    }
                }
                else -> wallet.balance
            }
            dao.updateWallet(wallet.copy(balance = newBalance))
        }

        // Handle TRANSFER destination wallet revert
        if (transaction.type == "TRANSFER" && transaction.destinationWalletId != null) {
            val destWallet = dao.getWalletById(transaction.destinationWalletId)
            if (destWallet != null) {
                dao.updateWallet(destWallet.copy(balance = destWallet.balance - transaction.amount))
            }
        }

        // 2. Revert Budget if Expense
        if (transaction.type == "EXPENSE") {
            updateBudgetSpending(transaction.categoryName, transaction.amount, transaction.timestamp, isAddition = false)
        }

        // 3. Delete from DB
        dao.deleteTransaction(transaction)
    }

    suspend fun updateTransaction(newTransaction: Transaction) {
        val oldTransaction = dao.getTransactionById(newTransaction.id) ?: return

        // 1. Revert old transaction wallet balance
        val oldWallet = dao.getWalletById(oldTransaction.walletId)
        if (oldWallet != null) {
            val revertedBalance = when (oldTransaction.type) {
                "EXPENSE" -> oldWallet.balance + oldTransaction.amount
                "INCOME" -> oldWallet.balance - oldTransaction.amount
                "TRANSFER" -> oldWallet.balance + oldTransaction.amount
                "ADJUSTMENT" -> {
                    if (oldTransaction.note.contains("tăng")) {
                        oldWallet.balance - oldTransaction.amount
                    } else {
                        oldWallet.balance + oldTransaction.amount
                    }
                }
                else -> oldWallet.balance
            }
            dao.updateWallet(oldWallet.copy(balance = revertedBalance))
        }

        // Revert old transaction destination wallet balance
        if (oldTransaction.type == "TRANSFER" && oldTransaction.destinationWalletId != null) {
            val oldDestWallet = dao.getWalletById(oldTransaction.destinationWalletId)
            if (oldDestWallet != null) {
                dao.updateWallet(oldDestWallet.copy(balance = oldDestWallet.balance - oldTransaction.amount))
            }
        }

        // Revert old transaction budget spending
        if (oldTransaction.type == "EXPENSE") {
            updateBudgetSpending(oldTransaction.categoryName, oldTransaction.amount, oldTransaction.timestamp, isAddition = false)
        }

        // 2. Apply new transaction wallet balance
        val newWallet = dao.getWalletById(newTransaction.walletId)
        if (newWallet != null) {
            // Need to fetch fresh wallet state since it might be the same wallet as oldWallet, which has changed balance!
            val freshWallet = dao.getWalletById(newTransaction.walletId) ?: newWallet
            val appliedBalance = when (newTransaction.type) {
                "EXPENSE" -> freshWallet.balance - newTransaction.amount
                "INCOME" -> freshWallet.balance + newTransaction.amount
                "TRANSFER" -> freshWallet.balance - newTransaction.amount
                "ADJUSTMENT" -> {
                    if (newTransaction.note.contains("tăng")) {
                        freshWallet.balance + newTransaction.amount
                    } else {
                        freshWallet.balance - newTransaction.amount
                    }
                }
                else -> freshWallet.balance
            }
            dao.updateWallet(freshWallet.copy(balance = appliedBalance))
        }

        // Apply new transaction destination wallet balance
        if (newTransaction.type == "TRANSFER" && newTransaction.destinationWalletId != null) {
            // Fetch fresh destination wallet because it might be the same as the source wallet or old destination wallet
            val freshDestWallet = dao.getWalletById(newTransaction.destinationWalletId)
            if (freshDestWallet != null) {
                dao.updateWallet(freshDestWallet.copy(balance = freshDestWallet.balance + newTransaction.amount))
            }
        }

        // Apply new transaction budget spending
        if (newTransaction.type == "EXPENSE") {
            updateBudgetSpending(newTransaction.categoryName, newTransaction.amount, newTransaction.timestamp, isAddition = true)
        }

        // 3. Save new transaction data
        dao.insertTransaction(newTransaction)
    }

    // Helper to automatically find budgets for the transaction's month and adjust spentAmount
    private suspend fun updateBudgetSpending(category: String, amount: Double, timestamp: Long, isAddition: Boolean) {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val monthStr = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        
        // Find existing budgets for this month
        val allBudgetsForMonth = dao.getBudgetsByMonth(monthStr).firstOrNull()
        if (allBudgetsForMonth.isNullOrEmpty()) return

        // To support parent-child categories, we need to know the hierarchy
        val catSetting = dao.getSetting("custom_categories")
        val categoryHierarchy = mutableMapOf<String, String?>() // child -> parent
        if (catSetting != null) {
            val parts = catSetting.value.split(";;")
            for (p in parts) {
                val segs = p.split("|")
                if (segs.size >= 4) {
                    val catName = segs[0]
                    val parentName = if (segs.size >= 5 && segs[4].isNotEmpty()) segs[4] else null
                    categoryHierarchy[catName] = parentName
                }
            }
        }

        val categoriesForBudgetUpdate = mutableSetOf<String>()
        categoriesForBudgetUpdate.add(category)
        var currentCat = category
        while (true) {
            val parent = categoryHierarchy[currentCat]
            if (parent != null) {
                categoriesForBudgetUpdate.add(parent)
                currentCat = parent // Walk up
            } else {
                break
            }
        }

        val matchedBudgets = allBudgetsForMonth.filter { it.categoryName in categoriesForBudgetUpdate }
        for (budget in matchedBudgets) {
            val newSpent = if (isAddition) {
                budget.spentAmount + amount
            } else {
                budget.spentAmount - amount
            }
            dao.updateBudget(budget.copy(spentAmount = Math.max(0.0, newSpent)))
        }
    }

    // --- Budgets ---
    fun getAllBudgets(): Flow<List<Budget>> = dao.getAllBudgets()
    fun getBudgetsForMonth(month: String): Flow<List<Budget>> = dao.getBudgetsByMonth(month)
    suspend fun insertBudget(budget: Budget): Long = dao.insertBudget(budget)
    suspend fun updateBudget(budget: Budget) = dao.updateBudget(budget)
    suspend fun deleteBudget(budget: Budget) = dao.deleteBudget(budget)

    suspend fun updateCategoryInRelatedData(oldName: String, newName: String, newIcon: String, newColor: String) {
        dao.updateCategoryInTransactions(oldName, newName, newIcon, newColor)
        dao.updateCategoryInBudgets(oldName, newName, newIcon, newColor)
    }

    suspend fun exportAllDataAsJson(): String {
        val wallets = dao.getAllWallets().first()
        val transactions = dao.getAllTransactions().first()
        val budgets = dao.getAllBudgets().first()
        val goals = dao.getAllSavingsGoals().first()
        val events = dao.getAllEvents().first()
        val debts = dao.getAllDebts().first()
        val settingsList = dao.getAllSettings()
        
        val moshi = com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
            
        val root = org.json.JSONObject()
        root.put("version", 2)
        root.put("backup_timestamp", System.currentTimeMillis())
        
        root.put("wallets", org.json.JSONArray(moshi.adapter<List<Wallet>>(com.squareup.moshi.Types.newParameterizedType(List::class.java, Wallet::class.java)).toJson(wallets)))
        root.put("transactions", org.json.JSONArray(moshi.adapter<List<Transaction>>(com.squareup.moshi.Types.newParameterizedType(List::class.java, Transaction::class.java)).toJson(transactions)))
        root.put("budgets", org.json.JSONArray(moshi.adapter<List<Budget>>(com.squareup.moshi.Types.newParameterizedType(List::class.java, Budget::class.java)).toJson(budgets)))
        root.put("savingsGoals", org.json.JSONArray(moshi.adapter<List<SavingsGoal>>(com.squareup.moshi.Types.newParameterizedType(List::class.java, SavingsGoal::class.java)).toJson(goals)))
        root.put("events", org.json.JSONArray(moshi.adapter<List<Event>>(com.squareup.moshi.Types.newParameterizedType(List::class.java, Event::class.java)).toJson(events)))
        root.put("debts", org.json.JSONArray(moshi.adapter<List<Debt>>(com.squareup.moshi.Types.newParameterizedType(List::class.java, Debt::class.java)).toJson(debts)))
        
        val settingsObj = org.json.JSONObject()
        settingsList.forEach { settingsObj.put(it.key, it.value) }
        root.put("app_settings", settingsObj)
        
        return root.toString()
    }

    // --- Savings Goals ---
    val allSavingsGoals: Flow<List<SavingsGoal>> = dao.getAllSavingsGoals()
    suspend fun insertSavingsGoal(goal: SavingsGoal): Long = dao.insertSavingsGoal(goal)
    suspend fun updateSavingsGoal(goal: SavingsGoal) = dao.updateSavingsGoal(goal)
    suspend fun deleteSavingsGoal(goal: SavingsGoal) = dao.deleteSavingsGoal(goal)

    // --- Settings ---
    suspend fun getAllSettings(): List<AppSetting> = dao.getAllSettings()
    suspend fun getSetting(key: String): AppSetting? = dao.getSetting(key)
    fun observeSetting(key: String): Flow<AppSetting?> = dao.observeSetting(key)
    suspend fun saveSetting(key: String, value: String) {
        dao.insertSetting(AppSetting(key, value))
    }

    // --- Events ---
    val allEvents: Flow<List<Event>> = dao.getAllEvents()
    suspend fun insertEvent(event: Event): Long = dao.insertEvent(event)
    suspend fun updateEvent(event: Event) = dao.updateEvent(event)
    suspend fun deleteEvent(event: Event) = dao.deleteEvent(event)

    // --- Debts ---
    val allDebts: Flow<List<Debt>> = dao.getAllDebts()
    suspend fun insertDebt(debt: Debt): Long = dao.insertDebt(debt)
    suspend fun updateDebt(debt: Debt) = dao.updateDebt(debt)
    suspend fun deleteDebt(debt: Debt) = dao.deleteDebt(debt)

    // --- Seeding helper ---
    suspend fun checkAndSeedDatabase() {
        val wallets = dao.getAllWallets().firstOrNull()
        if (wallets.isNullOrEmpty()) {
            // Seed default wallets
            dao.insertWallet(Wallet(name = "Tiền mặt", type = "CASH", balance = 5000000.0, colorHex = "#FF5722", iconName = "Payments", displayOrder = 0))
            dao.insertWallet(Wallet(name = "Tài khoản ngân hàng", type = "BANK", balance = 15000000.0, colorHex = "#2196F3", iconName = "AccountBalance", displayOrder = 1))
            dao.insertWallet(Wallet(name = "Ví MoMo", type = "WALLET", balance = 2000000.0, colorHex = "#E91E63", iconName = "AccountBalanceWallet", displayOrder = 2))
            dao.insertWallet(Wallet(name = "Hũ Tiết Kiệm", type = "SAVINGS", balance = 10000000.0, colorHex = "#4CAF50", iconName = "Savings", displayOrder = 3))

            // Seed default budgets for the current month
            val cal = Calendar.getInstance()
            val currentMonth = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            
            dao.insertBudget(Budget(categoryName = "Ăn uống", categoryIcon = "Restaurant", categoryColor = "#FF9800", limitAmount = 4000000.0, spentAmount = 0.0, month = currentMonth))
            dao.insertBudget(Budget(categoryName = "Di chuyển", categoryIcon = "DirectionsCar", categoryColor = "#2196F3", limitAmount = 1000000.0, spentAmount = 0.0, month = currentMonth))
            dao.insertBudget(Budget(categoryName = "Mua sắm", categoryIcon = "ShoppingBag", categoryColor = "#E91E63", limitAmount = 3000000.0, spentAmount = 0.0, month = currentMonth))
            dao.insertBudget(Budget(categoryName = "Hóa đơn", categoryIcon = "Receipt", categoryColor = "#FFC107", limitAmount = 2000000.0, spentAmount = 0.0, month = currentMonth))

            // Seed a sample savings goal to help the user get started
            val targetCal = Calendar.getInstance().apply { add(Calendar.MONTH, 6) }
            dao.insertSavingsGoal(SavingsGoal(name = "Mua Điện Thoại Mới", targetAmount = 15000000.0, currentAmount = 5000000.0, targetDate = targetCal.timeInMillis, note = "Cần tiết kiệm thêm"))
        }
    }

    suspend fun clearAllData() {
        dao.deleteAllTransactions()
        dao.deleteAllWallets()
        dao.deleteAllBudgets()
        dao.deleteAllSavingsGoals()
        dao.deleteAllEvents()
        dao.deleteAllDebts()
    }

    suspend fun deleteAllSettings() = dao.deleteAllSettings()

    suspend fun insertWalletDirect(wallet: Wallet) = dao.insertWallet(wallet)
    suspend fun insertTransactionDirect(transaction: Transaction) = dao.insertTransaction(transaction)
    suspend fun insertBudgetDirect(budget: Budget) = dao.insertBudget(budget)
    suspend fun insertSavingsGoalDirect(goal: SavingsGoal) = dao.insertSavingsGoal(goal)
    suspend fun insertEventDirect(event: Event) = dao.insertEvent(event)
    suspend fun insertDebtDirect(debt: Debt) = dao.insertDebt(debt)
}
