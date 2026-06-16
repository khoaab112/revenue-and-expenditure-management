package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FinanceRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: FinanceDao
    private lateinit var repository: FinanceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.financeDao()
        repository = FinanceRepository(dao)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insert transaction updates wallet balance correctly`() = runTest {
        // 1. Create a logical wallet
        val walletId = repository.insertWallet(
            Wallet(
                name = "Test Wallet",
                type = "CASH",
                balance = 1000.0,
                colorHex = "#FFFFFF",
                iconName = "Icon"
            )
        ).toInt()

        // 2. Add an EXPENSE transaction
        repository.insertTransaction(
            Transaction(
                walletId = walletId,
                walletName = "Test Wallet",
                type = "EXPENSE",
                amount = 200.0,
                categoryName = "Food",
                categoryIcon = "Food",
                categoryColor = "#FFF",
                note = "Lunch",
                timestamp = System.currentTimeMillis()
            )
        )

        // 3. Check wallet balance
        var wallet = repository.getWalletById(walletId)
        requireNotNull(wallet)
        assertEquals(800.0, wallet.balance, 0.0)

        // 4. Add an INCOME transaction
        repository.insertTransaction(
            Transaction(
                walletId = walletId,
                walletName = "Test Wallet",
                type = "INCOME",
                amount = 500.0,
                categoryName = "Salary",
                categoryIcon = "Salary",
                categoryColor = "#FFF",
                note = "Bonus",
                timestamp = System.currentTimeMillis()
            )
        )

        // 5. Check wallet balance again
        wallet = repository.getWalletById(walletId)
        requireNotNull(wallet)
        assertEquals(1300.0, wallet.balance, 0.0)
    }

    @Test
    fun `delete transaction reverts wallet balance`() = runTest {
        val walletId = repository.insertWallet(
            Wallet(
                name = "Test Wallet",
                type = "CASH",
                balance = 1000.0,
                colorHex = "#FFFFFF",
                iconName = "Icon"
            )
        ).toInt()

        val txId = repository.insertTransaction(
            Transaction(
                walletId = walletId,
                walletName = "Test Wallet",
                type = "EXPENSE",
                amount = 300.0,
                categoryName = "Food",
                categoryIcon = "Food",
                categoryColor = "#FFF",
                note = "Dinner",
                timestamp = System.currentTimeMillis()
            )
        )

        var wallet = repository.getWalletById(walletId)
        assertEquals(700.0, wallet!!.balance, 0.0)

        val tx = repository.getTransactionById(txId.toInt())
        requireNotNull(tx)

        repository.deleteTransaction(tx)

        wallet = repository.getWalletById(walletId)
        assertEquals(1000.0, wallet!!.balance, 0.0)
    }

    @Test
    fun `update transaction correctly adjusts wallet balance`() = runTest {
        val walletId = repository.insertWallet(
            Wallet(
                name = "Test Wallet",
                type = "CASH",
                balance = 1000.0,
                colorHex = "#FFFFFF",
                iconName = "Icon"
            )
        ).toInt()

        val txId = repository.insertTransaction(
            Transaction(
                walletId = walletId,
                walletName = "Test Wallet",
                type = "EXPENSE",
                amount = 200.0,
                categoryName = "Food",
                categoryIcon = "Food",
                categoryColor = "#FFF",
                note = "Lunch",
                timestamp = System.currentTimeMillis()
            )
        )

        // Balance should be 800
        var wallet = repository.getWalletById(walletId)
        assertEquals(800.0, wallet!!.balance, 0.0)

        // Now modify the transaction amount to 150
        val tx = repository.getTransactionById(txId.toInt())
        requireNotNull(tx)

        repository.updateTransaction(tx.copy(amount = 150.0))

        // Reverting -200 means +200 -> 1000, then applying -150 -> 850
        wallet = repository.getWalletById(walletId)
        assertEquals(850.0, wallet!!.balance, 0.0)
    }

    @Test
    fun `expense transaction also updates corresponding budget`() = runTest {
        // Setup budget for the current month
        val cal = Calendar.getInstance()
        val currentMonth = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        val budgetId = repository.insertBudget(
            Budget(
                categoryName = "Ăn uống",
                categoryIcon = "Food",
                categoryColor = "#FFF",
                limitAmount = 2000.0,
                spentAmount = 0.0,
                month = currentMonth
            )
        ).toInt()

        val walletId = repository.insertWallet(
            Wallet(
                name = "Test Wallet",
                type = "CASH",
                balance = 10000.0,
                colorHex = "#FFFFFF",
                iconName = "Icon"
            )
        ).toInt()

        // Insert Expense
        repository.insertTransaction(
            Transaction(
                walletId = walletId,
                walletName = "Test Wallet",
                type = "EXPENSE",
                amount = 500.0,
                categoryName = "Ăn uống",
                categoryIcon = "Food",
                categoryColor = "#FFF",
                note = "Lunch",
                timestamp = System.currentTimeMillis()
            )
        )

        // Check budget spentAmount
        val budgets = dao.getBudgetsByMonth(currentMonth).first()
        val foodBudget = budgets.find { it.categoryName == "Ăn uống" }
        requireNotNull(foodBudget)
        assertEquals(500.0, foodBudget.spentAmount, 0.0)
    }
}
