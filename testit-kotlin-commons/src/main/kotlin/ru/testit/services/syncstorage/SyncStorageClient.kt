package ru.testit.services.syncstorage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * HTTP client for Sync Storage API.
 * Provides methods for worker registration, status updates, health checks,
 * and sending in-progress test results.
 */
class SyncStorageClient(
    private val baseUrl: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Check if Sync Storage is healthy.
     */
    fun isHealthy(): Boolean {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/health")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Register a worker with the Sync Storage service.
     * @return RegisterResponse with is_master flag
     */
    fun registerWorker(registerRequest: RegisterRequest): RegisterResponse {
        val body = json.encodeToString(registerRequest).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$baseUrl/register")
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Failed to register worker: HTTP ${response.code}")
            }
            val responseBody = response.body?.string() ?: "{}"
            return json.decodeFromString<RegisterResponse>(responseBody)
        }
    }

    /**
     * Set worker status (in_progress / completed).
     */
    fun setWorkerStatus(statusRequest: SetWorkerStatusRequest): SetWorkerStatusResponse {
        val body = json.encodeToString(statusRequest).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$baseUrl/set-worker-status")
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logger.warn("Failed to set worker status: HTTP ${response.code}")
                return SetWorkerStatusResponse()
            }
            val responseBody = response.body?.string() ?: "{}"
            return json.decodeFromString<SetWorkerStatusResponse>(responseBody)
        }
    }

    /**
     * Send in-progress test result to Sync Storage.
     */
    fun sendInProgressTestResult(testRunId: String, model: TestResultCutApiModel): Boolean {
        return try {
            val body = json.encodeToString(model).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/in_progress_test_result?testRunId=$testRunId")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.warn("Failed to send in-progress test result: HTTP ${response.code}")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            logger.warn("Error sending in-progress test result: ${e.message}")
            false
        }
    }

    /**
     * Wait for Sync Storage completion.
     */
    fun waitCompletion(testRunId: String): Boolean {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/wait-completion?testRunId=$testRunId")
                .get()
                .build()

            val client = httpClient.newBuilder()
                .readTimeout(120, TimeUnit.SECONDS)
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            logger.warn("Error waiting for sync storage completion: ${e.message}")
            false
        }
    }

    fun close() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }
}
