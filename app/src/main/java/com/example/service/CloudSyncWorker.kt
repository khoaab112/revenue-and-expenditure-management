package com.example.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.FinanceRepository
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(applicationContext)?.account
            if (account == null) {
                return@withContext Result.failure()
            }

            // Get access token for Drive
            val scope = "oauth2:https://www.googleapis.com/auth/drive.file"
            val token = GoogleAuthUtil.getToken(applicationContext, account, scope)

            // Read database (assuming we'll need to export everything to a JSON)
            val database = com.example.data.AppDatabase.getDatabase(applicationContext)
            val repository = com.example.data.FinanceRepository(database.financeDao())
            val exportedData = repository.exportAllDataAsJson()

            val fileName = "finance_backup.json"
            
            // Search if file exists
            val client = OkHttpClient()
            val searchRequest = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files?q=name='$fileName'&spaces=drive")
                .header("Authorization", "Bearer $token")
                .build()
            
            val searchResponse = client.newCall(searchRequest).execute()
            var fileId: String? = null
            if (searchResponse.isSuccessful) {
                val json = searchResponse.body?.string()
                json?.let {
                    val jsonObj = JSONObject(it)
                    val files = jsonObj.optJSONArray("files")
                    if (files != null && files.length() > 0) {
                        fileId = files.getJSONObject(0).getString("id")
                    }
                }
            }
            
            val metadata = JSONObject()
            metadata.put("name", fileName)
            metadata.put("mimeType", "application/json")
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("metadata", null, metadata.toString().toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .addFormDataPart("file", fileName, exportedData.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .build()

            val uploadUrl = if (fileId == null) {
                "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
            } else {
                "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=multipart"
            }
            
            val requestBuilder = Request.Builder()
                .url(uploadUrl)
                .header("Authorization", "Bearer $token")
            
            if (fileId != null) {
                requestBuilder.patch(requestBody)
            } else {
                requestBuilder.post(requestBody)
            }
            
            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                return@withContext Result.success()
            } else {
                return@withContext Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.retry()
        }
    }

    companion object {
        fun setupPeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<CloudSyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "CloudSyncService",
                ExistingPeriodicWorkPolicy.UPDATE,
                syncRequest
            )
        }
    }
}
