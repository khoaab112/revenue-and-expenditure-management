package com.app.ui

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
    val status: String, // "AUTO_ADDED", "FAILED_PARSE", "NO_WALLET"
    val notificationKey: String = ""
)

data class SmartWalletMapping(
    val bankName: String,
    val refKey: String,
    val walletId: Int,
    val walletName: String,
    val confidenceScore: Int,
    val lastConfirmed: Long
)

enum class NotificationType {
    SUCCESS, WARNING, ERROR
}

data class AppNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val message: String,
    val type: NotificationType,
    val durationMs: Long = 3500L
)
