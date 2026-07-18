package com.example.service

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiAdvisorService {
    private const val TAG = "GeminiAdvisorService"

    const val DEFAULT_PROMPT = """Bạn là Trợ lý tài chính AI chuyên nghiệp tại Việt Nam. Dưới đây là thông tin tài chính hiện tại của người dùng:

1. Các ví / tài khoản hiện tại:
[Dữ liệu tài khoản]

2. Tóm tắt thu chi tháng này và tháng trước:
[Dữ liệu thu chi]

3. Hạng mục chi tiêu nhiều nhất tháng này:
[Dữ liệu hạng mục]

4. Các khoản Nợ và Cho vay:
[Dữ liệu khoản nợ]

5. Tình hình ngân sách (Hạn mức chi tiêu):
[Dữ liệu hạn mức]

QUY TẮC RÀNG BUỘC PHÂN TÍCH (LƯU Ý NGHIÊM NGẶT):
1. CHỈ sử dụng thông tin có trong dữ liệu đầu vào. Không được suy diễn hoặc tự tạo thêm các giao dịch, khoản nợ, ví tiền hoặc ngân sách không tồn tại.
2. Nếu dữ liệu đầu vào không đủ hoặc rỗng để đưa ra kết luận, hãy ghi rõ trong phần "assessment" là "chưa đủ dữ liệu phân tích".
3. TRONG MẢNG "warnings" (CẢNH BÁO RỦI RO): Cần mang tính tiên đoán/dự báo tương lai nhiều hơn là chỉ thống kê hiện tại:
   - Dựa vào tốc độ chi tiêu hàng ngày trong tháng, hãy tính toán và dự đoán xem một hạng mục (ví dụ: **Ăn uống**, **Đi lại**) có nguy cơ vượt hạn mức ngân sách trong bao nhiêu ngày tới (ví dụ: *"Với tình hình chi tiêu hiện tại, chi phí **Ăn uống** của bạn sẽ vượt hạn mức trong **3 ngày tới**"*).
   - Hãy tiên đoán xem tài khoản khả dụng/ví tiền có nguy cơ bị **cháy tài khoản** (hết sạch tiền) trong bao nhiêu ngày tới (ví dụ: *"Tài khoản của bạn có thể bị **cháy** trong **3 ngày tới** nếu với tình hình chi tiêu cho **Ăn uống** như hiện tại"*).
   - Chỉ ra các rủi ro thực tế khác nếu có (Tổng chi > Tổng thu, Nợ quá hạn, Ví cạn kiệt...).
4. TRONG MẢNG "recommendations" (KHUYẾN NGHỊ): Hãy đưa ra lời khuyên cụ thể, mang tính hành động kèm con số định mức chi tiêu giới hạn mỗi ngày từ nay đến cuối tháng để người dùng không vượt quá hạn mức:
   - Ví dụ: *"Chi phí **Ăn uống** của bạn nên ở mức **25.000đ/1 ngày** để không vượt quá hạn mức ngân sách"* hoặc đề xuất mức giảm chi tiêu cụ thể.
   - TUYỆT ĐỐI không đưa ra lời khuyên chung chung như "Hãy tiết kiệm hơn", "Cố gắng chi tiêu hợp lý".
5. TRONG MẢNG "assessment" (ĐÁNH GIÁ): Hãy chia nhỏ thành các dòng nhận định ngắn gọn rành mạch. Phải có ít nhất một dòng đánh giá so sánh xu hướng tăng/giảm chi tiết (Ví dụ: *"Bạn đã giảm chi phí **Đi lại** được **50%** so với tuần trước/tháng trước"*).
6. ĐÁNH DẤU TỪ KHÓA BẰNG DẤU SAO KÉP: Bạn bắt buộc phải bọc các từ khóa quan trọng trong câu bằng ký tự `**` (ví dụ: "**Ăn uống**", "**Ví Cash**", "**cháy trong 3 ngày tới**", "**25.000đ/1 ngày**", "**giảm 50%**", "**vượt hạn mức**") để hệ thống tự động tô đậm trên giao diện.
7. CHIA Ý NHỎ, NGẮN GỌN, RÀNH MẠCH: Các câu phân tích trong trường "assessment" phải ngắn gọn, súc tích và rành mạch. Phân tách thành 3-4 dòng riêng biệt bằng ký tự xuống dòng `\n` để tạo các gạch đầu dòng ngắn, không viết thành một đoạn văn dài dòng liên tục.

Dựa trên thông tin và các quy tắc ràng buộc trên, hãy phản hồi CHỈ bằng một đối tượng JSON có định dạng chính xác sau (Không kèm markdown codeblock ```json hoặc bất cứ văn bản nào khác ngoài JSON):
{
  "assessment": "[Đánh giá tổng quan chia thành 3-4 dòng ý nhỏ rành mạch, phân tách nhau bằng ký tự xuống dòng \n, mỗi dòng là một nhận định ngắn gọn kèm từ khóa được tô đậm]",
  "warnings": [
    "[Cảnh báo rủi ro cụ thể ngắn gọn, rành mạch 1]",
    "[Cảnh báo rủi ro cụ thể ngắn gọn, rành mạch 2]"
  ],
  "recommendations": [
    "[Khuyến nghị cụ thể hành động ngắn gọn, rành mạch 1]",
    "[Khuyến nghị cụ thể hành động ngắn gọn, rành mạch 2]"
  ]
}

LƯU Ý: Nếu không có cảnh báo hoặc đề xuất nào thỏa mãn điều kiện thực tế ở trên, hãy trả về mảng rỗng [] cho trường đó."""

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class AdvisorResult(
        val assessment: String,
        val warnings: List<String>,
        val recommendations: List<String>,
        val success: Boolean,
        val errorMessage: String? = null
    )

    suspend fun getFinancialAdvice(
        walletsInfo: String,
        monthlySummary: String,
        topExpenses: String,
        debtsInfo: String,
        budgetsInfo: String,
        customApiKey: String? = null
    ): AdvisorResult = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER")) {
            return@withContext AdvisorResult(
                assessment = "",
                warnings = emptyList(),
                recommendations = emptyList(),
                success = false,
                errorMessage = "API Key chưa được cấu hình! Vui lòng cài đặt Gemini API Key trong phần Cài đặt."
            )
        }

        val prompt = DEFAULT_PROMPT
            .replace("[Dữ liệu tài khoản]", walletsInfo)
            .replace("[Dữ liệu thu chi]", monthlySummary)
            .replace("[Dữ liệu hạng mục]", topExpenses)
            .replace("[Dữ liệu khoản nợ]", debtsInfo)
            .replace("[Dữ liệu hạn mức]", budgetsInfo)

        val models = listOf(
            "gemini-flash-latest",
            "gemini-flash-lite-latest",
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-pro-latest"
        )
        
        var lastErrorMsg = "Lỗi kết nối Gemini API"
        var finalResult: AdvisorResult? = null

        for (attempt in 1..models.size) {
            val currentModel = models[attempt - 1]
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$currentModel:generateContent?key=$apiKey"
            
            try {
                val jsonRequest = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val partsArray = JSONArray().apply {
                                val partObj = JSONObject().apply {
                                    put("text", prompt)
                                }
                                put(partObj)
                            }
                            put("parts", partsArray)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArray)
                }

                val requestBody = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Request failed for model $currentModel: $bodyStr")
                        lastErrorMsg = "Gemini API trả về lỗi: ${response.code}"
                        return@use
                    }

                    val jsonResponse = JSONObject(bodyStr)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates == null || candidates.length() == 0) {
                        lastErrorMsg = "Không tìm thấy câu trả lời từ Gemini!"
                        return@use
                    }

                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts == null || parts.length() == 0) {
                        lastErrorMsg = "Phản hồi rỗng từ Gemini!"
                        return@use
                    }

                    val rawText = parts.getJSONObject(0).optString("text", "")
                    val cleanedText = cleanJsonBody(rawText)
                    
                    try {
                        val resultJson = JSONObject(cleanedText)
                        val assessmentVal = resultJson.optString("assessment", "")
                        
                        val warningsList = mutableListOf<String>()
                        val warningsArr = resultJson.optJSONArray("warnings")
                        if (warningsArr != null) {
                            for (i in 0 until warningsArr.length()) {
                                warningsList.add(warningsArr.getString(i))
                            }
                        }

                        val recommendationsList = mutableListOf<String>()
                        val recommendationsArr = resultJson.optJSONArray("recommendations")
                        if (recommendationsArr != null) {
                            for (i in 0 until recommendationsArr.length()) {
                                recommendationsList.add(recommendationsArr.getString(i))
                            }
                        }

                        finalResult = AdvisorResult(
                            assessment = assessmentVal,
                            warnings = warningsList,
                            recommendations = recommendationsList,
                            success = true
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "JSON parsing error from text: $cleanedText", e)
                        finalResult = AdvisorResult(
                            assessment = rawText,
                            warnings = emptyList(),
                            recommendations = emptyList(),
                            success = true
                        )
                    }
                }
                
                if (finalResult != null && finalResult.success) {
                    return@withContext finalResult!!
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to call model $currentModel", e)
                lastErrorMsg = "Lỗi kết nối Gemini API: ${e.message}"
            }
        }
        
        return@withContext finalResult ?: AdvisorResult(
            assessment = "",
            warnings = emptyList(),
            recommendations = emptyList(),
            success = false,
            errorMessage = lastErrorMsg
        )
    }

    private fun cleanJsonBody(rawText: String): String {
        var text = rawText.trim()
        if (text.startsWith("```json")) {
            text = text.substring(7)
        } else if (text.startsWith("```")) {
            text = text.substring(3)
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length - 3)
        }
        return text.trim()
    }

    data class AITransactionResult(
        val success: Boolean,
        val transactions: List<AIParsedTransaction> = emptyList(),
        val errorMessage: String? = null
    )

    data class AIParsedTransaction(
        val type: String, // "INCOME" or "EXPENSE"
        val amount: Double,
        val categoryName: String,
        val walletName: String,
        val note: String
    )

    suspend fun parseTransactionsFromText(
        inputText: String,
        walletsInfo: String,
        categoriesInfo: String,
        customApiKey: String? = null
    ): AITransactionResult = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER")) {
            return@withContext AITransactionResult(
                success = false,
                errorMessage = "Chưa cài đặt API Key cho Gemini. Vui lòng vào Cài đặt để cấu hình."
            )
        }

        val prompt = """Bạn là trợ lý tài chính AI thông minh. Người dùng vừa nhập đoạn văn bản sau để ghi chép chi tiêu/thu nhập:
"$inputText"

Dưới đây là danh sách các "Ví/Tài khoản" hiện có của người dùng:
$walletsInfo

Dưới đây là danh sách các "Hạng mục" hiện có:
$categoriesInfo

Nhiệm vụ của bạn:
1. Phân tích văn bản của người dùng để trích xuất các giao dịch (có thể có 1 hoặc nhiều giao dịch trong câu).
2. Trả về kết quả DƯỚI DẠNG MẢNG JSON (JSON Array), mỗi phần tử là một đối tượng JSON đại diện cho 1 giao dịch.
3. KHÔNG trả về bất cứ văn bản nào khác ngoài JSON.

Cấu trúc mỗi đối tượng JSON bắt buộc:
{
  "type": "EXPENSE" hoặc "INCOME" (nếu là thu nhập thì INCOME, nếu chi tiêu là EXPENSE),
  "amount": <Số tiền dạng số thực (Double), ví dụ: 50000, 200000>,
  "categoryName": "<Tên hạng mục phù hợp nhất TỪ DANH SÁCH HẠNG MỤC Ở TRÊN, nếu không chắc hãy để là 'Khác'>",
  "walletName": "<Tên ví phù hợp nhất TỪ DANH SÁCH VÍ Ở TRÊN, nếu người dùng không nói rõ, hãy chọn tên ví xuất hiện đầu tiên trong danh sách>",
  "note": "<Trích xuất mô tả ngắn gọn cho giao dịch này, ví dụ: 'Ăn phở', 'Tiền điện'>"
}

Chú ý: Phân tích số tiền chính xác, ví dụ "50k" -> 50000, "1 củ" -> 1000000.
Nếu không nhận diện được bất kỳ giao dịch nào, hãy trả về mảng rỗng []."""

        val models = listOf(
            "gemini-flash-latest",
            "gemini-flash-lite-latest",
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-pro-latest"
        )
        
        var lastErrorMsg = "Lỗi kết nối Gemini API"
        var finalResult: AITransactionResult? = null

        for (attempt in 1..models.size) {
            val currentModel = models[attempt - 1]
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$currentModel:generateContent?key=$apiKey"
            
            try {
                val jsonRequest = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val partsArray = JSONArray().apply {
                                val partObj = JSONObject().apply {
                                    put("text", prompt)
                                }
                                put(partObj)
                            }
                            put("parts", partsArray)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArray)
                }

                val requestBody = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Request failed for model $currentModel: $bodyStr")
                        lastErrorMsg = if (response.code == 429) {
                            "Vượt quá giới hạn gọi API (Lỗi 429) cho model $currentModel. Vui lòng thử lại sau."
                        } else {
                            "Gemini API trả về lỗi (${response.code}) cho model $currentModel"
                        }
                        return@use // try next model
                    }

                    val jsonResponse = JSONObject(bodyStr)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates == null || candidates.length() == 0) {
                        lastErrorMsg = "Không tìm thấy câu trả lời từ Gemini ($currentModel)!"
                        return@use
                    }

                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts == null || parts.length() == 0) {
                        lastErrorMsg = "Phản hồi rỗng từ Gemini ($currentModel)!"
                        return@use
                    }

                    val rawText = parts.getJSONObject(0).optString("text", "")
                    val cleanedText = cleanJsonBody(rawText)
                    
                    try {
                        val jsonArray = JSONArray(cleanedText)
                        val resultList = mutableListOf<AIParsedTransaction>()
                        
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            resultList.add(
                                AIParsedTransaction(
                                    type = obj.optString("type", "EXPENSE"),
                                    amount = obj.optDouble("amount", 0.0),
                                    categoryName = obj.optString("categoryName", "Khác"),
                                    walletName = obj.optString("walletName", ""),
                                    note = obj.optString("note", "")
                                )
                            )
                        }
                        
                        finalResult = AITransactionResult(
                            success = true,
                            transactions = resultList
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "JSON parsing error for AITransaction from text: $cleanedText", e)
                        lastErrorMsg = "Lỗi phân tích cú pháp từ AI ($currentModel): Không phải định dạng JSON hợp lệ."
                        return@use
                    }
                }
                
                if (finalResult != null && finalResult.success) {
                    return@withContext finalResult!!
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to call model $currentModel", e)
                lastErrorMsg = "Lỗi kết nối mạng ($currentModel): ${e.message}"
            }
        }
        
        return@withContext finalResult ?: AITransactionResult(
            success = false,
            errorMessage = lastErrorMsg
        )
    }
}
