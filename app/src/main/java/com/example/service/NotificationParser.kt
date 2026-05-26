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

        // 1. Determine Bank / App and corresponding Wallet name for Vietnamese and Foreign Banks
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
            
            // International / Foreign Banks support
            combinedText.contains("hsbc") || packageName.contains("hsbc") -> Pair("HSBC", "Foreign Bank Account")
            combinedText.contains("citibank") || combinedText.contains("citi ") || packageName.contains("citibank") -> Pair("Citibank", "Foreign Bank Account")
            combinedText.contains("standard chartered") || combinedText.contains("stanchart") -> Pair("Standard Chartered", "Foreign Bank Account")
            combinedText.contains("chase") || packageName.contains("chase") -> Pair("Chase Bank", "Foreign Bank Account")
            combinedText.contains("revolut") || packageName.contains("revolut") -> Pair("Revolut", "Foreign Bank Account")
            combinedText.contains("paypal") || packageName.contains("paypal") -> Pair("PayPal", "Online Wallet")
            combinedText.contains("wise") || packageName.contains("wise") || combinedText.contains("transferwise") -> Pair("Wise", "Online Wallet")
            
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

        // 2. Determine Transaction Type (INCOME or EXPENSE) with Vietnamese & English keywords
        val incomeKeywords = listOf(
            "+", "nhận tiền", "cong +", "nạp tiền", "nhận", "co:", "cộng", "transfer in", "chuyển đến", "tăng", "nhận từ", "được cộng",
            "received", "deposited", "refunded", "transfer from", "credit", "incoming", "added"
        )
        val expenseKeywords = listOf(
            "-", "trừ", "thanh toán", "chuyển đi", "nợ:", "trừ tiền", "chuyển khoản đi", "transfer out", "giảm", "rút tiền", "thanh toán hóa đơn", "phí",
            "paid", "spent", "purchase", "withdrew", "charge", "outgoing", "payment for"
        )

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
        val cleanText = text.replace("đ", "vnd")
            .replace("d", "vnd")
            .replace("$", "usd")
            .replace("€", "eur")
            .replace("£", "gbp")
            .lowercase()

        // Match numbers following signs or associated with currency keywords
        val patterns = listOf(
            Pattern.compile("""[+-]\s?(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2})?)\b"""),
            Pattern.compile("""(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2})?)\s?(?:vnd|usd|eur|gbp)\b"""),
            Pattern.compile("""(?:usd|eur|gbp)\s?(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2})?)\b"""),
            Pattern.compile("""\b(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2})?)\b"""),
            Pattern.compile("""\b(\d{4,12})\b""")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(cleanText)
            while (matcher.find()) {
                val groupText = matcher.group(1) ?: continue
                
                var normalized = groupText
                val hasDecimal = normalized.length >= 4 && (normalized[normalized.length - 3] == '.' || normalized[normalized.length - 3] == ',')
                val decimalChar = if (hasDecimal) normalized[normalized.length - 3] else null
                
                if (hasDecimal && decimalChar != null) {
                    val integerPart = normalized.substring(0, normalized.length - 3)
                    val decimalPart = normalized.substring(normalized.length - 2)
                    
                    val cleanInt = integerPart.filter { it.isDigit() }
                    normalized = "$cleanInt.$decimalPart"
                } else {
                    normalized = normalized.filter { it.isDigit() }
                }

                val parsedVal = normalized.toDoubleOrNull() ?: 0.0
                val isForeign = cleanText.contains("usd") || cleanText.contains("eur") || cleanText.contains("gbp")
                if (isForeign) {
                    if (parsedVal >= 1.0) return parsedVal
                } else {
                    if (parsedVal >= 1000.0) return parsedVal
                }
            }
        }

        return 0.0
    }

    private fun extractNote(text: String, bankName: String): String {
        val lowerText = text.lowercase()
        val indicators = listOf("nội dung:", "noidung:", "lý do:", "gd:", "nd:", "ref:", "mô tả:", "nội dung gd:", "note:", "memo:", "desc:")
        for (indicator in indicators) {
            val idx = lowerText.indexOf(indicator)
            if (idx != -1) {
                val rem = text.substring(idx + indicator.length).trim()
                return cleanNoteString(rem)
            }
        }

        val quotePattern = Pattern.compile(""""([^"]+)"""")
        val qMatcher = quotePattern.matcher(text)
        if (qMatcher.find()) {
            return cleanNoteString(qMatcher.group(1) ?: "")
        }

        return "Nhận qua $bankName"
    }

    private fun cleanNoteString(input: String): String {
        return input.replace(Regex("""[\[\]\(\)\{\}]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
