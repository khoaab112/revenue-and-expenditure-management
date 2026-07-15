package com.example.service

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object GeminiReceiptScanner {
    private const val TAG = "GeminiReceiptScanner"
    
    // Configured with 60-second timeouts as per best practice
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class ScannedItem(
        val name: String,
        val amount: Double,
        val category: String
    )

    data class ScanResult(
        val totalAmount: Double?,
        val note: String?,
        val transactionType: String?, // EXPENSE or INCOME
        val success: Boolean,
        val errorMessage: String? = null,
        val items: List<ScannedItem> = emptyList(),
        val dateTimeStr: String? = null,
        val shopName: String? = null,
        val taxAmount: Double? = null,
        val discountAmount: Double? = null
    )

    const val GEMINI_PROMPT_TEXT = "Bạn là trợ lý tài chính thông minh chuyên nghiệp tại Việt Nam. Hãy phân tích cực kỳ chính xác ảnh hóa đơn/biên lai/phiếu thu được chụp.\n" +
            "Nhiệm vụ của bạn:\n" +
            "1. Nhận dạng Tổng tiền thanh toán THỰC TẾ (tiền cuối cùng khách phải trả, sau tất cả thuế, phụ phí, giảm giá/chiết khấu. Các từ khóa: 'Tổng cộng', 'Tổng tiền', 'Thành tiền VAT', 'Total Payment', 'Grand Total').\n" +
            "2. Nhận dạng các mặt hàng chi tiết trong hóa đơn (Tên từng sản phẩm/dịch vụ, Đơn giá/Thành tiền tương ứng, số lượng). Đưa chúng vào danh sách 'items'.\n" +
            "3. Dự đoán phân mục cho từng mặt hàng (chỉ lấy trong danh sách: \"Ăn uống\", \"Di chuyển\", \"Mua sắm\", \"Hóa đơn\", \"Giải trí\", \"Giáo dục\", \"Sức khỏe\", \"Nhà cửa\", \"Khác\").\n" +
            "4. Nhận dạng Ngày giờ của hóa đơn dạng 'HH:mm dd/MM/yyyy'. Việt Nam thường ghi '01 thg 7, 2023' hoặc 'Tháng 07' có nghĩa là ngày 01/07/2023. Nếu thiếu ngày hoặc giờ, sử dụng ngày hiện tại hoặc giờ hiện tại.\n" +
            "5. Nhận dạng Tên cửa hàng/đơn vị bán hàng dạng 'shopName'. Ngắn gọn, súc tích.\n" +
            "6. Xác định loại giao dịch chính là EXPENSE (chi phí) hay INCOME (khoản thu).\n" +
            "7. Nhận dạng số tiền Thuế VAT ('taxAmount'): Tổng số tiền thuế giá trị gia tăng hoặc phụ thu dịch vụ bổ sung trên hóa đơn. TRẢ VỀ DẠNG SỐ NGUYÊN (ví dụ: nếu ghi 82.600 hay 82,600 thì trả về 82600. Nếu không tìm thấy, đặt bằng 0).\n" +
            "8. Nhận dạng số tiền Giảm giá / Chiết khấu / Voucher / Ưu đãi ('discountAmount'): Tổng số tiền được trừ hoặc giảm đi trên hóa đơn. TRẢ VỀ DẠNG SỐ NGUYÊN (ví dụ: nếu giảm 50.000, 50,000 thì trả về 50000. Nếu không tìm thấy, đặt bằng 0).\n\n" +
            "LƯU Ý NGHIÊM NGẶT ĐỂ TRÁNH LỖI PHÂN TÍCH:\n" +
            "- Hóa đơn tại Việt Nam cực kỳ phổ biến việc dùng dấu chấm \".\" làm dấu phân cách hàng nghìn (ví dụ: \"199.000\" mang nghĩa là một trăm chín mươi chín nghìn đồng, không phải là một trăm chín mươi chín lẻ không đồng. Bạn phải đổi nó thành số nguyên 199000).\n" +
            "- Hãy cẩn thận bỏ qua các hàng phụ biểu thị số lượng hoặc đơn giá lặp lại của chính sản phẩm phía trên nó. Ví dụ: dòng '199,000 x1' hoặc dòng đơn giá trùng lặp chỉ là thông tin đơn giá cho 'Bún chấm gạch cua' ngay phía trên, tuyệt đối KHÔNG ĐƯỢC coi là một mặt hàng riêng biệt! Mỗi sản phẩm chỉ được tạo MỘT đối tượng duy nhất trong 'items'.\n" +
            "- Tuyệt đối không đưa các dòng tổng phụ hoặc tổng kết như 'Cộng tiền hàng', 'Thành tiền', 'Tiền thuế(VAT: 10%)', 'Thành tiền VAT', 'Tổng tiền' vào danh mục các món hàng trong danh sách 'items'. Hãy lọc sạch chỉ giữ lại các sản phẩm thực tế khách mua hàng.\n\n" +
            "Hãy phản hồi CHỈ bằng một đối tượng JSON có đúng các trường sau:\n" +
            "{\n" +
            "  \"totalAmount\": [số tiền tổng cộng thực tế cả hóa đơn cuối cùng, kiểu số nguyên dài, ví dụ: 908600],\n" +
            "  \"taxAmount\": [số tiền thuế VAT hoặc dịch vụ phụ khác, ví dụ: 82600],\n" +
            "  \"discountAmount\": [số tiền được giảm giá / voucher / chiết khấu, ví dụ: 0],\n" +
            "  \"shopName\": \"[Tên đơn vị bán hoặc cửa hàng, ví dụ: Nhà hàng Cô Ba]\",\n" +
            "  \"dateTime\": \"[ngày giờ của hóa đơn dạng HH:mm dd/MM/yyyy, ví dụ: 13:29 01/07/2023]\",\n" +
            "  \"type\": \"EXPENSE\",\n" +
            "  \"items\": [\n" +
            "    {\n" +
            "      \"name\": \"[tên sản phẩm/mặt hàng thứ nhất, ví dụ: Bún chấm gạch cua]\",\n" +
            "      \"amount\": [số tiền mặt hàng này, ví dụ: 199000],\n" +
            "      \"category\": \"Ăn uống\"\n" +
            "    }\n" +
            "  ]\n" +
            "}"

    fun parseManualJson(textResponse: String): ScanResult {
        return try {
            val jsonStart = textResponse.indexOf("{")
            val jsonEnd = textResponse.lastIndexOf("}")
            val sanitizedJson = if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                textResponse.substring(jsonStart, jsonEnd + 1)
            } else {
                textResponse
            }

            val innerJson = JSONObject(sanitizedJson)
            val totalAmountVal = if (innerJson.isNull("totalAmount")) null else innerJson.optDouble("totalAmount")
            val taxAmountVal = if (innerJson.isNull("taxAmount")) null else innerJson.optDouble("taxAmount")
            val discountAmountVal = if (innerJson.isNull("discountAmount")) null else innerJson.optDouble("discountAmount")
            val shopNameVal = innerJson.optString("shopName", "")
            val dateTimeVal = innerJson.optString("dateTime", "")
            val typeVal = innerJson.optString("type", "EXPENSE")

            val itemsList = mutableListOf<ScannedItem>()
            val itemsJson = innerJson.optJSONArray("items")
            if (itemsJson != null) {
                for (i in 0 until itemsJson.length()) {
                    val itemObj = itemsJson.getJSONObject(i)
                    val name = itemObj.optString("name", "Mặt hàng")
                    val amount = itemObj.optDouble("amount", 0.0)
                    val category = itemObj.optString("category", "Khác")
                    if (amount > 0) {
                        itemsList.add(ScannedItem(name, amount, category))
                    }
                }
            }

            ScanResult(
                totalAmount = totalAmountVal,
                taxAmount = taxAmountVal,
                discountAmount = discountAmountVal,
                note = if (shopNameVal.isNotBlank()) shopNameVal else "Hóa đơn mua sắm",
                transactionType = typeVal,
                success = true,
                items = itemsList,
                dateTimeStr = dateTimeVal,
                shopName = shopNameVal
            )
        } catch (e: Exception) {
            Log.e(TAG, "Manual JSON extraction/parsing failed", e)
            ScanResult(
                totalAmount = null,
                taxAmount = null,
                discountAmount = null,
                note = null,
                transactionType = null,
                success = false,
                errorMessage = "Không thể phân tích dữ liệu JSON: ${e.message}"
            )
        }
    }

    suspend fun scanReceipt(bitmap: Bitmap, customApiKey: String? = null): ScanResult = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER")) {
            return@withContext ScanResult(
                totalAmount = null,
                note = null,
                transactionType = null,
                success = false,
                errorMessage = "API Key chưa được cấu hình! Vui lòng cài đặt Gemini API Key trong phần Cài đặt."
            )
        }

        try {
            // Scale bitmap down if it is too massive to save memory, bandwidth and speed-up OCR
            val maxDimension = 1200
            val width = bitmap.width
            val height = bitmap.height
            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val newWidth: Int
                val newHeight: Int
                if (ratio > 1) {
                    newWidth = maxDimension
                    newHeight = (maxDimension / ratio).toInt()
                } else {
                    newHeight = maxDimension
                    newWidth = (maxDimension * ratio).toInt()
                }
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else {
                bitmap
            }

            // Compress scaled bitmap to JPEG Base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, byteArrayOutputStream)
            val base64Image = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)

            // Define the prompt inside structured JSON to return itemized list
            val promptText = GEMINI_PROMPT_TEXT

            // Construct manual JSON body safely
            val jsonRequestBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val parts = JSONArray().apply {
                        // Text Part
                        val textPart = JSONObject().apply {
                            put("text", promptText)
                        }
                        // Image Part
                        val imagePart = JSONObject().apply {
                            val inlineData = JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            }
                            put("inlineData", inlineData)
                        }
                        
                        put(textPart)
                        put(imagePart)
                    }
                    
                    val contentObj = JSONObject().apply {
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                // structured output response MIME type config
                val generationConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.1)
                    
                    val responseSchema = JSONObject().apply {
                        put("type", "OBJECT")
                        val properties = JSONObject().apply {
                            put("totalAmount", JSONObject().apply { put("type", "INTEGER"); put("nullable", true) })
                            put("taxAmount", JSONObject().apply { put("type", "INTEGER"); put("nullable", true) })
                            put("discountAmount", JSONObject().apply { put("type", "INTEGER"); put("nullable", true) })
                            put("shopName", JSONObject().apply { put("type", "STRING") })
                            put("dateTime", JSONObject().apply { put("type", "STRING") })
                            put("type", JSONObject().apply { put("type", "STRING") })
                            put("items", JSONObject().apply {
                                put("type", "ARRAY")
                                put("items", JSONObject().apply {
                                    put("type", "OBJECT")
                                    val itemProperties = JSONObject().apply {
                                        put("name", JSONObject().apply { put("type", "STRING") })
                                        put("amount", JSONObject().apply { put("type", "NUMBER") })
                                        put("category", JSONObject().apply { put("type", "STRING") })
                                    }
                                    put("properties", itemProperties)
                                    put("required", JSONArray().apply { put("name"); put("amount"); put("category") })
                                })
                            })
                        }
                        put("properties", properties)
                    }
                    put("responseSchema", responseSchema)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = jsonRequestBody.toString().toRequestBody("application/json".toMediaType())

            var lastErrorMsg = "Lỗi kết nối mạng khi quét hóa đơn"
            // Using modern preview models
            val models = listOf(
                "gemini-3.5-flash",
                "gemini-3.1-pro-preview"
            )

            for (attempt in 1..models.size) {
                val currentModel = models[attempt - 1]
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$currentModel:generateContent?key=$apiKey"
                Log.d(TAG, "Trying scan with model: $currentModel (Attempt $attempt)")

                try {
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val responseBodyStr = response.body?.string() ?: ""
                            Log.d(TAG, "Raw Response: $responseBodyStr")

                            val rootJson = JSONObject(responseBodyStr)
                            val candidates = rootJson.optJSONArray("candidates")
                            if (candidates == null || candidates.length() == 0) {
                                lastErrorMsg = "Không tìm thấy nội dung phản hồi từ Gemini!"
                                if (attempt < models.size) {
                                    kotlinx.coroutines.delay(1000L * attempt)
                                    return@use
                                }
                            } else {
                                val content = candidates.getJSONObject(0).optJSONObject("content")
                                val parts = content?.optJSONArray("parts")
                                if (parts == null || parts.length() == 0) {
                                    lastErrorMsg = "Nội dung phản hồi từ AI rỗng!"
                                    if (attempt < models.size) {
                                        kotlinx.coroutines.delay(1000L * attempt)
                                        return@use
                                    }
                                } else {
                                    val textResponse = parts.getJSONObject(0).optString("text", "")
                                    if (textResponse.isBlank()) {
                                        lastErrorMsg = "Văn bản AI trả về rỗng!"
                                        if (attempt < models.size) {
                                            kotlinx.coroutines.delay(1000L * attempt)
                                            return@use
                                        }
                                    } else {
                                        Log.d(TAG, "AI Content: $textResponse")
                                        try {
                                            return@withContext parseManualJson(textResponse)
                                        } catch (e: Exception) {
                                            Log.e(TAG, "JSON extraction/parsing failed", e)
                                            lastErrorMsg = "Không thể phân tích dữ liệu JSON từ AI: ${e.message}"
                                            if (attempt < models.size) {
                                                kotlinx.coroutines.delay(1000L * attempt)
                                                return@use
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val code = response.code
                            lastErrorMsg = "API không phản hồi thành công (Lỗi $code)"
                            Log.w(TAG, "Model $currentModel returned error code: $code")
                            if (attempt < models.size) {
                                kotlinx.coroutines.delay(1000L * attempt)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Attempt $attempt using $currentModel failed: ${e.message}")
                    lastErrorMsg = e.localizedMessage ?: "Lỗi kết nối mạng"
                    if (attempt < models.size) {
                        kotlinx.coroutines.delay(1000L * attempt)
                    }
                }
            }

            Log.w(TAG, "All API model attempts failed.")
            return@withContext ScanResult(
                totalAmount = null,
                note = null,
                transactionType = null,
                success = false,
                errorMessage = lastErrorMsg
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during receipt scanning", e)
            return@withContext ScanResult(
                totalAmount = null,
                note = null,
                transactionType = null,
                success = false,
                errorMessage = "Gặp sự cố khi kết nối API: ${e.localizedMessage}"
            )
        }
    }
}
