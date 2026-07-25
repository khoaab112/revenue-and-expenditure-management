package com.app.ui.screens

import com.app.ui.NotificationLog
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTransferMatcherTest {

    private fun log(
        key: String,
        wallet: String,
        type: String,
        amount: Double = 500_000.0,
        timestamp: Long = 1_000_000L
    ) = NotificationLog(
        timestamp = timestamp,
        title = "Bank alert",
        text = key,
        bankName = "Bank",
        amount = amount,
        type = type,
        note = "Transfer",
        walletName = wallet,
        status = "PENDING",
        notificationKey = key
    )

    @Test
    fun `opposite entries with same amount in different wallets form an internal transfer`() {
        val groups = groupPendingNotifications(listOf(
            log("outgoing", "VCB", "EXPENSE"),
            log("incoming", "MoMo", "INCOME", timestamp = 1_060_000L)
        ))

        assertEquals(1, groups.size)
        assertEquals("TRANSFER_PAIR", groups.single().type)
    }

    @Test
    fun `opposite entries in the same wallet are not treated as an internal transfer`() {
        val groups = groupPendingNotifications(listOf(
            log("expense", "VCB", "EXPENSE"),
            log("income", "VCB", "INCOME", timestamp = 1_060_000L)
        ))

        assertEquals(listOf("SINGLE", "SINGLE"), groups.map { it.type })
    }
}
