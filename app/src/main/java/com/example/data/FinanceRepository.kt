package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

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
            val newBalance = if (transaction.type == "EXPENSE") {
                wallet.balance - transaction.amount
            } else {
                wallet.balance + transaction.amount
            }
            dao.updateWallet(wallet.copy(balance = newBalance))
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
            val newBalance = if (transaction.type == "EXPENSE") {
                wallet.balance + transaction.amount
            } else {
                wallet.balance - transaction.amount
            }
            dao.updateWallet(wallet.copy(balance = newBalance))
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
            val revertedBalance = if (oldTransaction.type == "EXPENSE") {
                oldWallet.balance + oldTransaction.amount
            } else {
                oldWallet.balance - oldTransaction.amount
            }
            dao.updateWallet(oldWallet.copy(balance = revertedBalance))
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
            val appliedBalance = if (newTransaction.type == "EXPENSE") {
                freshWallet.balance - newTransaction.amount
            } else {
                appliedBalance(freshWallet.balance, newTransaction.amount)
            }
            dao.updateWallet(freshWallet.copy(balance = appliedBalance))
        }

        // Apply new transaction budget spending
        if (newTransaction.type == "EXPENSE") {
            updateBudgetSpending(newTransaction.categoryName, newTransaction.amount, newTransaction.timestamp, isAddition = true)
        }

        // 3. Save new transaction data
        dao.insertTransaction(newTransaction)
    }

    private fun appliedBalance(current: Double, amount: Double): Double {
        return current + amount
    }

    // Helper to automatically find budgets for the transaction's month and adjust spentAmount
    private suspend fun updateBudgetSpending(category: String, amount: Double, timestamp: Long, isAddition: Boolean) {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val monthStr = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        
        // Find existing budgets for this month
        val allBudgetsForMonth = dao.getAllBudgets().firstOrNull()?.filter { it.month == monthStr }
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
    }

    suspend fun deleteAllSettings() = dao.deleteAllSettings()

    suspend fun insertWalletDirect(wallet: Wallet) = dao.insertWallet(wallet)
    suspend fun insertTransactionDirect(transaction: Transaction) = dao.insertTransaction(transaction)
    suspend fun insertBudgetDirect(budget: Budget) = dao.insertBudget(budget)
    suspend fun insertSavingsGoalDirect(goal: SavingsGoal) = dao.insertSavingsGoal(goal)
}
