package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.AppDatabase
import com.example.data.Categories
import com.example.data.FinanceRepository
import com.example.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class BankNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    companion object {
        @Volatile
        var instance: BankNotificationListenerService? = null
            private set
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val context = applicationContext
        val packageName = sbn.packageName ?: ""
        val extras = sbn.notification?.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (text.isBlank()) return

        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repository = FinanceRepository(db.financeDao())

                // 1. Check if notification reader is enabled in settings
                val isEnabled = repository.getSetting("notification_reader_enabled")?.value == "true"
                if (!isEnabled) {
                    return@launch
                }

                // 2. Parse the notification
                val parsed = NotificationParser.parse(title, text, packageName)
                if (!parsed.success) {
                    // Log failed parsing is also good for diagnostic/history purposes
                    saveLog(repository, title, text, parsed, "FAILED_PARSE", null)
                    return@launch
                }

                // 3. Find matched Wallet from DB
                val wallets = repository.allWallets.firstOrNull() ?: emptyList()
                val matchedWallet = wallets.find { it.name.lowercase().contains(parsed.bankName.lowercase()) }
                    ?: wallets.find { it.name.lowercase().contains(parsed.detectedWalletName.lowercase()) }
                    ?: wallets.find { it.type == "BANK" }
                    ?: wallets.firstOrNull()

                val walletNameInput = matchedWallet?.name ?: parsed.detectedWalletName

                // 4. Save as PENDING so that user can review and confirm with complete certainty
                saveLog(repository, title, text, parsed, "PENDING", walletNameInput)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun selectCategory(type: String, note: String): String {
        val lowerNote = note.lowercase()
        if (type == "INCOME") {
            return when {
                lowerNote.contains("luong") || lowerNote.contains("salary") -> "Lương"
                lowerNote.contains("thuong") || lowerNote.contains("gift") || lowerNote.contains("tang") -> "Thưởng"
                lowerNote.contains("ban hang") || lowerNote.contains("kinh doanh") || lowerNote.contains("sales") -> "Kinh doanh"
                else -> "Khác"
            }
        } else {
            return when {
                lowerNote.contains("an uong") || lowerNote.contains("restaurant") || lowerNote.contains("coffee") || lowerNote.contains("milktea") || lowerNote.contains("tra sua") || lowerNote.contains("com") || lowerNote.contains("pho") || lowerNote.contains("food") -> "Ăn uống"
                lowerNote.contains("grab") || lowerNote.contains("xe") || lowerNote.contains("beemin") || lowerNote.contains("gojek") || lowerNote.contains("gas") || lowerNote.contains("xang") || lowerNote.contains("petrol") || lowerNote.contains("taxi") -> "Di chuyển"
                lowerNote.contains("shopee") || lowerNote.contains("tiki") || lowerNote.contains("lazada") || lowerNote.contains("mua sam") || lowerNote.contains("shopping") || lowerNote.contains("quan ao") || lowerNote.contains("quanao") || lowerNote.contains("sieu thi") -> "Mua sắm"
                lowerNote.contains("cuoc") || lowerNote.contains("cuoc dt") || lowerNote.contains("the cao") || lowerNote.contains("wifi") || lowerNote.contains("dien nuoc") || lowerNote.contains("internet") || lowerNote.contains("hoa don") || lowerNote.contains("receipt") || lowerNote.contains("tien dien") -> "Hóa đơn"
                lowerNote.contains("game") || lowerNote.contains("phim") || lowerNote.contains("cgv") || lowerNote.contains("netflix") || lowerNote.contains("giai tri") || lowerNote.contains("ticket") -> "Giải trí"
                lowerNote.contains("hoc") || lowerNote.contains("school") || lowerNote.contains("sach") || lowerNote.contains("course") || lowerNote.contains("giao duc") -> "Giáo dục"
                lowerNote.contains("thuoc") || lowerNote.contains("hospital") || lowerNote.contains("benh vien") || lowerNote.contains("suc khoe") -> "Sức khỏe"
                lowerNote.contains("nha cua") || lowerNote.contains("thue phong") || lowerNote.contains("homestay") -> "Nhà cửa"
                else -> "Khác"
            }
        }
    }

    private suspend fun saveLog(
        repository: FinanceRepository,
        title: String,
        text: String,
        parsed: NotificationParser.ParsedNotification,
        status: String,
        walletName: String?
    ) {
        val logsSetting = repository.getSetting("notification_logs")?.value ?: "[]"
        val jsonArray = try { JSONArray(logsSetting) } catch (e: Exception) { JSONArray() }

        val logObj = JSONObject()
        logObj.put("timestamp", System.currentTimeMillis())
        logObj.put("title", title)
        logObj.put("text", text)
        logObj.put("bankName", parsed.bankName)
        logObj.put("amount", parsed.amount)
        logObj.put("type", parsed.type)
        logObj.put("note", parsed.note)
        logObj.put("walletName", walletName ?: parsed.detectedWalletName)
        logObj.put("status", status)

        val newList = JSONArray()
        newList.put(logObj)
        val limitTime = System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000)
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val timestamp = item.optLong("timestamp", 0L)
            val statusVal = item.optString("status", "")
            if (statusVal == "PENDING" || statusVal == "DELETED" || timestamp >= limitTime) {
                newList.put(item)
            }
        }

        repository.saveSetting("notification_logs", newList.toString())
    }
}
