package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExportHelper {

    fun exportTransactionsToCsv(
        context: Context,
        transactions: List<Transaction>,
        onWarning: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ): Boolean {
        if (transactions.isEmpty()) {
            onWarning("Không có giao dịch nào để xuất!")
            return false
        }

        try {
            // Create a temp file in cache directory
            val sdfFile = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestampStr = sdfFile.format(Date())
            val fileName = "BaoCao_ThuChi_$timestampStr.csv"
            val file = File(context.cacheDir, fileName)

            val fileWriter = FileOutputStream(file)
            
            // Write UTF-8 BOM (\uFEFF) so Excel parses Vietnamese characters correctly
            fileWriter.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            // Headers
            val headers = listOf(
                "ID Giao dịch",
                "Ngày giờ",
                "Loại giao dịch",
                "Số tiền (VND)",
                "Danh mục chi tiêu",
                "Ví/Tài khoản",
                "Ghi chú",
                "Có lặp lại?",
                "Chu kỳ lặp"
            )
            fileWriter.write((headers.joinToString(",") { escapeCsv(it) } + "\n").toByteArray(Charsets.UTF_8))

            val sdfDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build())
            
            for (tx in transactions) {
                val dateStr = sdfDate.format(Date(tx.timestamp))
                val typeStr = if (tx.type == "EXPENSE") "Chi tiêu" else "Thu nhập"
                val recurringStr = if (tx.isRecurring) "Có" else "Không"
                val recurrencePeriodStr = when (tx.recurrencePeriod) {
                    "DAILY" -> "Hàng ngày"
                    "WEEKLY" -> "Hàng tuần"
                    "MONTHLY" -> "Hàng tháng"
                    else -> "Không"
                }

                val row = listOf(
                    tx.id.toString(),
                    dateStr,
                    typeStr,
                    tx.amount.toLong().toString(), // Keep as plain numeric string so Excel/Google Sheets formats/sums it easily
                    tx.categoryName,
                    tx.walletName,
                    tx.note,
                    recurringStr,
                    recurrencePeriodStr
                )
                fileWriter.write((row.joinToString(",") { escapeCsv(it) } + "\n").toByteArray(Charsets.UTF_8))
            }

            fileWriter.flush()
            fileWriter.close()

            // Trigger Share Intent
            shareCsvFile(context, file)
            return true

        } catch (e: Exception) {
            e.printStackTrace()
            onError("Lỗi xảy ra khi xuất báo cáo: ${e.localizedMessage}")
            return false
        }
    }

    private fun escapeCsv(value: String): String {
        var result = value.trim()
        if (result.contains(",") || result.contains("\"") || result.contains("\n") || result.contains("\r")) {
            result = result.replace("\"", "\"\"")
            result = "\"$result\""
        }
        return result
    }

    private fun shareCsvFile(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Báo cáo Ghi chép Thu chi")
            putExtra(Intent.EXTRA_TEXT, "Gửi bạn báo cáo dữ liệu thu chi từ ứng dụng Ghi chép Thu chi.")
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Chia sẻ báo cáo qua")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
