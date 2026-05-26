package com.example.service

import java.util.regex.Pattern

object NotificationParser {
    data class ParsedNotification(
        val amount: Double,
        val type: String, // "INCOME" or "EXPENSE"
        val bankName: String,
        val detectedWalletName: String,
        val note: String,
        val success: Boolean
    )

    fun parse(title: String, text: String, packageName: String): ParsedNotification {
        val lowerTitle = title.lowercase()
        val lowerText = text.lowercase()
        val combinedText = "$lowerTitle $lowerText"

        // 1. Determine Bank / App and corresponding Wallet name in Vietnamese
        val (bankName, detectedWalletName) = when {
            combinedText.contains("momo") || packageName.contains("momo") -> Pair("MoMo", "Ví MoMo")
            combinedText.contains("vietcombank") || packageName.contains("vietcombank") || combinedText.contains("vcb") -> Pair("Vietcombank", "Tài khoản ngân hàng")
            combinedText.contains("techcom") || packageName.contains("techcombank") || combinedText.contains("tcb") -> Pair("Techcombank", "Tài khoản ngân hàng")
            combinedText.contains("mb bank") || combinedText.contains("mbbank") || packageName.contains("mbmobile") || combinedText.contains("mb_bank") -> Pair("MB Bank", "Tài khoản ngân hàng")
            combinedText.contains("tpbank") || packageName.contains("tpbank") -> Pair("TPBank", "Tài khoản ngân hàng")
            combinedText.contains("agribank") || packageName.contains("agribank") -> Pair("Agribank", "Tài khoản ngân hàng")
            combinedText.contains("acb") || packageName.contains("acb") -> Pair("ACB", "Tài khoản ngân hàng")
            combinedText.contains("viettel pay") || combinedText.contains("viettel money") || packageName.contains("viettelpay") -> Pair("Viettel Money", "Ví MoMo")
            combinedText.contains("zalopay") || packageName.contains("zalopay") || combinedText.contains("zalo pay") -> Pair("ZaloPay", "Ví MoMo")
            else -> {
                // Heuristic detection based on common bank abbreviations
                val words = listOf("bidv", "vpbank", "sacombank", "vbi", "shb", "vib", "msb", "ocb", "hdbank", "scb")
                val matched = words.find { combinedText.contains(it) }
                if (matched != null) {
                    Pair(matched.uppercase(), "Tài khoản ngân hàng")
                } else {
                    Pair("Ngân hàng", "Tài khoản ngân hàng")
                }
            }
        }

        // 2. Determine Transaction Type (INCOME or EXPENSE)
        val incomeKeywords = listOf("+", "nhận tiền", "cong +", "nạp tiền", "nhận", "co:", "cộng", "transfer in", "chuyển đến", "tăng", "nhận từ", "được cộng")
        val expenseKeywords = listOf("-", "trừ", "thanh toán", "chuyển đi", "nợ:", "trừ tiền", "chuyển khoản đi", "transfer out", "giảm", "rút tiền", "thanh toán hóa đơn", "phí")

        var type = "EXPENSE" // default
        val hasPlus = combinedText.contains("+")
        val hasMinus = combinedText.contains("-")

        if (hasPlus && !hasMinus) {
            type = "INCOME"
        } else if (hasMinus && !hasPlus) {
            type = "EXPENSE"
        } else {
            // Count matched keywords
            val incomeScore = incomeKeywords.count { combinedText.contains(it) }
            val expenseScore = expenseKeywords.count { combinedText.contains(it) }
            if (incomeScore > expenseScore) {
                type = "INCOME"
            }
        }

        // 3. Extract Amount
        val rawAmount = extractAmount(text)

        // 4. Extract clean transaction note
        var note = extractNote(text, bankName)
        if (note.length > 80) {
            note = note.substring(0, 77) + "..."
        }
        if (note.isBlank()) {
            note = "Biến động số dư $bankName"
        }

        return ParsedNotification(
            amount = rawAmount,
            type = type,
            bankName = bankName,
            detectedWalletName = detectedWalletName,
            note = note,
            success = rawAmount > 0
        )
    }

    private fun extractAmount(text: String): Double {
        // Regex to match typical Vietnamese currency strings
        // E.g. "+1.200.000d", "giam -50,000 VND", "+ 500.000 d", "+10,000,000 d"
        // Try to match a pattern like [+ or -]? [numbers with dots or commas as thousand separators] [options: VND, đ, d, vnđ]
        val cleanText = text.replace("đ", "vnd")
            .replace("d", "vnd")
            .replace("vnvnd", "vnd") // in case "vnđ" -> "vnvnd"
            .lowercase()

        // Match numbers following signs or preceding currency markers
        val patterns = listOf(
            // Pattern for signed numbers +500.000 or -15.000
            Pattern.compile("""[+-]\s?(\d{1,3}(?:[.,]\d{3})+)"""),
            // Pattern for numbers before currency label, e.g. "500.000 vnd" or "50,000vnd"
            Pattern.compile("""(\d{1,3}(?:[.,]\d{3})+)\s?vnd"""),
            // Pattern for raw numbers with standard separator followed by text or boundaries
            Pattern.compile("""\b(\d{1,3}(?:[.,]\d{3})+)\b"""),
            // Fallback for simple integer numbers over 5000 (standard format without dots/commas, e.g. "chuyển khoản 200000")
            Pattern.compile("""\b(\d{4,12})\b""")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(cleanText)
            while (matcher.find()) {
                val groupText = matcher.group(1) ?: continue
                val normalizedNumStr = groupText.replace(".", "").replace(",", "")
                val parsedVal = normalizedNumStr.toDoubleOrNull() ?: 0.0
                if (parsedVal >= 1000.0) { // filter out small timestamps, transaction codes, or pins under 1000
                    return parsedVal
                }
            }
        }

        return 0.0
    }

    private fun extractNote(text: String, bankName: String): String {
        val lowerText = text.lowercase()
        // Typical prefixes showing transaction note
        val indicators = listOf("nội dung:", "noidung:", "lý do:", "gd:", "nd:", "ref:", "mô tả:", "nội dung gd:")
        for (indicator in indicators) {
            val idx = lowerText.indexOf(indicator)
            if (idx != -1) {
                val rem = text.substring(idx + indicator.length).trim()
                // Clean characters often found inside SMS
                return cleanNoteString(rem)
            }
        }

        // Fallback: If it's a MoMo or simple app, search for quoted text or text in parentheses
        val quotePattern = Pattern.compile(""""([^"]+)"""")
        val qMatcher = quotePattern.matcher(text)
        if (qMatcher.find()) {
            return cleanNoteString(qMatcher.group(1) ?: "")
        }

        return "Nhận qua $bankName"
    }

    private fun cleanNoteString(input: String): String {
        return input.replace(Regex("""[\[\]\(\)\{\}]"""), "") // remove braces
            .replace(Regex("""\s+"""), " ") // normalize spacing
            .trim()
    }
}
