package com.example.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object FormatHelper {
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))

    fun formatVND(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        return try {
            "${formatter.format(amount)} ₫"
        } catch (e: Exception) {
            "${String.format("%.0f", amount)} ₫"
        }
    }

    fun parseColor(hex: String): androidx.compose.ui.graphics.Color {
        return try {
            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            androidx.compose.ui.graphics.Color(0xFF607D8B)
        }
    }

    fun formatDate(timestamp: Long): String {
        return try {
            dateFormatter.format(timestamp)
        } catch (e: Exception) {
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
        }
    }
}
