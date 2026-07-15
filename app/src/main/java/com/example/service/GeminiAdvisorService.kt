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

        val prompt = """
            Bạn là Trợ lý tài chính AI chuyên nghiệp tại Việt Nam. Dưới đây là thông tin tài chính hiện tại của người dùng:
            
            1. Các ví / tài khoản hiện tại:
            $walletsInfo
            
            2. Tóm tắt thu chi tháng này:
            $monthlySummary
            
            3. Hạng mục chi tiêu nhiều nhất tháng này:
            $topExpenses
            
            4. Các khoản Nợ và Cho vay:
            $debtsInfo
            
            5. Tình hình ngân sách (Hạn mức chi tiêu):
            $budgetsInfo
            
            QUY TẮC RÀNG BUỘC PHÂN TÍCH (LƯU Ý NGHIÊM NGẶT):
            1. CHỈ sử dụng thông tin có trong dữ liệu đầu vào. Không được suy diễn hoặc tự tạo thêm các giao dịch, khoản nợ, ví tiền hoặc ngân sách không tồn tại.
            2. Nếu dữ liệu đầu vào không đủ hoặc rỗng để đưa ra kết luận, hãy ghi rõ trong phần "assessment" là "chưa đủ dữ liệu phân tích".
            3. CHỈ tạo nội dung trong mảng "warnings" khi có dấu hiệu rủi ro thực tế sau đây từ dữ liệu đầu vào:
               - Tổng chi tiêu lớn hơn Tổng thu nhập (Tổng chi > Tổng thu).
               - Có bất kỳ khoản nợ nào bị quá hạn.
               - Ngân sách (Hạn mức chi tiêu) của một hạng mục đã sử dụng vượt quá 90%.
               - Một hạng mục chi tiêu riêng lẻ chiếm trên 40% tổng chi tiêu tháng.
               - Số dư tiền mặt hoặc số dư ví chi tiêu quá thấp (sắp cạn kiệt).
            4. Mỗi đề xuất trong mảng "recommendations" phải liên quan trực tiếp đến ít nhất một dữ liệu đầu vào thực tế ở trên.
            5. TUYỆT ĐỐI không đưa ra lời khuyên chung chung như "Hãy tiết kiệm hơn", "Cố gắng chi tiêu hợp lý". Hãy đưa ra lời khuyên hành động cụ thể rõ ràng kèm số liệu lấy trực tiếp hoặc ước tính tỷ lệ tương đối từ đầu vào, ví dụ:
               - "Giảm ngân sách ăn uống khoảng 15%."
               - "Thu hồi khoản nợ của [Tên người nợ]."
               - "Bổ sung 2.000.000 ₫ vào quỹ khẩn cấp."
               - "Chuyển 5% thu nhập sang tài khoản tiết kiệm."
            
            Dựa trên thông tin và các quy tắc ràng buộc trên, hãy phản hồi CHỈ bằng một đối tượng JSON có định dạng chính xác sau (Không kèm markdown codeblock ```json hoặc bất cứ văn bản nào khác ngoài JSON):
            {
              "assessment": "[Đánh giá tổng quan về sức khỏe tài chính hiện tại của người dùng. Viết ngắn gọn khoảng 2-3 câu, giọng điệu thân thiện, mang tính cố vấn chuyên sâu]",
              "warnings": [
                "[Cảnh báo rủi ro cụ thể 1]",
                "[Cảnh báo rủi ro cụ thể 2]"
              ],
              "recommendations": [
                "[Khuyến nghị cụ thể hành động 1]",
                "[Khuyến nghị cụ thể hành động 2]"
              ]
            }
            
            LƯU Ý: Nếu không có cảnh báo hoặc đề xuất nào thỏa mãn điều kiện thực tế ở trên, hãy trả về mảng rỗng [] cho trường đó.
        """.trimIndent()

        val models = listOf(
            "gemini-3.5-flash",
            "gemini-3.1-pro-preview"
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
}
