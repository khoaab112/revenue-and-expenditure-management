package com.app.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.app.data.AppDatabase
import com.app.data.Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FinanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: FinanceViewModel
    private lateinit var application: Application
    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(application)
        
        viewModel = FinanceViewModel(application)
    }

    @After
    fun tearDown() = runTest {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            database.clearAllTables()
        }
        Dispatchers.resetMain()
    }

    @Test
    fun `confirmPendingNotificationLog creates valid transaction and updates status`() = runTest {
        // Arrange
        val walletName = "Techcombank"
        val wallet = Wallet(name = walletName, type = "BANK", balance = 5000.0, colorHex = "#FFF", iconName = "Payments")
        val walletId = database.financeDao().insertWallet(wallet).toInt()

        val pendingLog = NotificationLog(
            timestamp = System.currentTimeMillis(),
            title = "Techcombank",
            text = "GD tru 200,000 VND",
            bankName = "Techcombank",
            amount = 200000.0,
            type = "EXPENSE",
            note = "Mua sam",
            walletName = walletName,
            status = "PENDING",
            notificationKey = "test_key_123"
        )

        // Act
        viewModel.confirmPendingNotificationLog(
            log = pendingLog,
            walletId = walletId,
            categoryName = "Mua sắm",
            overrideAmount = null,
            overrideNote = null,
            overrideEventId = null
        )
        
        // Let coroutines finish
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Wait for Room's background IO thread to finish (since it's not mocked)
        var txWalletId = 0
        var txWalletName = ""
        var txType = ""
        var txAmount = 0.0
        var txCategoryName = ""
        var txNote: String? = null
        var txNotificationKey: String? = null
        var found = false

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            for (i in 1..50) {
                var localFound = false
                database.query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 1", null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        txWalletId = cursor.getInt(cursor.getColumnIndexOrThrow("walletId"))
                        txWalletName = cursor.getString(cursor.getColumnIndexOrThrow("walletName"))
                        txType = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                        txAmount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"))
                        txCategoryName = cursor.getString(cursor.getColumnIndexOrThrow("categoryName"))
                        txNote = cursor.getString(cursor.getColumnIndexOrThrow("note"))
                        txNotificationKey = cursor.getString(cursor.getColumnIndexOrThrow("notificationKey"))
                        localFound = true
                    }
                }
                if (localFound) {
                    found = true
                    break
                }
                Thread.sleep(100)
            }
        }

        // Assert
        assertTrue("Transaction was not inserted into database within 5 seconds", found)
        
        assertEquals(walletId, txWalletId)
        assertEquals(walletName, txWalletName)
        assertEquals("EXPENSE", txType)
        assertEquals(200000.0, txAmount, 0.0)
        assertEquals("Mua sắm", txCategoryName)
        assertEquals(pendingLog.note, txNote)
        assertEquals(pendingLog.notificationKey, txNotificationKey)
        
        // Also check if wallet balance is updated
        val updatedWallet = database.financeDao().getWalletById(walletId)
        assertNotNull(updatedWallet)
        assertEquals(5000.0 - 200000.0, updatedWallet!!.balance, 0.0)
    }

    @Test
    fun `deleteWallet prevents deletion when transactions exist and shows warning`() = runTest {
        // Arrange
        val wallet = Wallet(name = "Test Wallet", type = "BANK", balance = 1000.0, colorHex = "#000", iconName = "Icon")
        val walletId = database.financeDao().insertWallet(wallet).toInt()
        val walletWithId = wallet.copy(id = walletId)
        
        val transaction = com.app.data.Transaction(
            walletId = walletId,
            walletName = wallet.name,
            type = "EXPENSE",
            amount = 100.0,
            categoryName = "Ăn uống",
            categoryIcon = "Icon",
            categoryColor = "#FFF",
            note = "",
            timestamp = System.currentTimeMillis()
        )
        database.financeDao().insertTransaction(transaction)
        
        // Act
        viewModel.deleteWallet(walletWithId)
        
        // Assert
        var foundWarning = false
        for (i in 1..50) {
            val notifications = viewModel.appNotifications.value
            if (notifications.any { it.message.contains("Không thể xóa") }) {
                foundWarning = true
                break
            }
            Thread.sleep(50)
            testDispatcher.scheduler.advanceTimeBy(10)
        }
        
        val dbWallet = database.financeDao().getWalletById(walletId)
        assertNotNull("Wallet should not be deleted", dbWallet)
        assertTrue("Should show warning notification", foundWarning)
    }

    @Test
    fun `deleteWallet successfully deletes when no transactions exist`() = runTest {
        // Arrange
        val wallet = Wallet(name = "Test Wallet 2", type = "BANK", balance = 1000.0, colorHex = "#000", iconName = "Icon")
        val walletId = database.financeDao().insertWallet(wallet).toInt()
        val walletWithId = wallet.copy(id = walletId)
        
        // Act
        viewModel.deleteWallet(walletWithId)
        
        // Assert
        var deleted = false
        for (i in 1..50) {
            val dbWallet = database.financeDao().getWalletById(walletId)
            if (dbWallet == null) {
                deleted = true
                break
            }
            Thread.sleep(50)
            testDispatcher.scheduler.advanceTimeBy(10)
        }
        assertTrue("Wallet should be deleted", deleted)
    }

    @Test
    fun `exportAllDataAsJson exports correct JSON structure`() = runTest {
        // Arrange
        val wallet = Wallet(name = "Test Wallet JSON", type = "BANK", balance = 5000.0, colorHex = "#000", iconName = "Icon")
        database.financeDao().insertWallet(wallet)
        
        // Wait for DB insertion to propagate to Flow
        for (i in 1..50) {
            if (database.financeDao().getAllWallets().first().isNotEmpty()) break
            Thread.sleep(50)
        }
        
        // Act
        val repository = com.app.data.FinanceRepository(
            dao = database.financeDao(),
            database = database
        )
        val jsonString = repository.exportAllDataAsJson()
        
        // Assert
        assertNotNull(jsonString)
        assertTrue("JSON must contain version: $jsonString", jsonString.contains(""""version": 2""") || jsonString.contains(""""version":2"""))
        assertTrue("JSON must contain wallet name: $jsonString", jsonString.contains("Test Wallet JSON"))
        assertTrue("JSON must contain wallets key: $jsonString", jsonString.contains(""""wallets":"""))
        assertTrue("JSON must contain transactions key: $jsonString", jsonString.contains(""""transactions":"""))
    }
}
