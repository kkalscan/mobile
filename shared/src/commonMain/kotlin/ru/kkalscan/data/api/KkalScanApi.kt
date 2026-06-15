package ru.kkalscan.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.kkalscan.data.IApiConfig
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.domain.model.ApiErrorBody
import ru.kkalscan.domain.model.BugReportResult
import ru.kkalscan.domain.model.CreateDiaryEntryResponse
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.domain.model.ScanBonusResult
import ru.kkalscan.domain.model.ScanResult
import ru.kkalscan.domain.model.SubscriptionStatus

class KkalScanApi(
    private val httpClient: HttpClient,
    private val config: IApiConfig,
) : IKkalScanApi {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun scanPhoto(deviceId: String, photoBytes: ByteArray, timezoneOffsetMinutes: Int): ScanResult =
        try {
            val response = httpClient.submitFormWithBinaryData(
                url = "${config.apiBaseUrl}/scan?timezone_offset_minutes=$timezoneOffsetMinutes",
                formData = formData {
                    append("photo", photoBytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=food.jpg")
                    })
                },
            ) {
                header("X-Device-Id", deviceId)
            }
            if (!response.status.isSuccess()) throw mapError(response.status.value, response.bodyAsText())
            response.body()
        } catch (e: KkalScanException) {
            throw e
        } catch (e: Exception) {
            throw KkalScanException.Network(e.message ?: "Network error")
        }

    override suspend fun grantScanBonus(deviceId: String): ScanBonusResult =
        postJson("/scan/bonus", BonusRequest(deviceId))

    override suspend fun getDiary(deviceId: String, date: String, timezoneOffsetMinutes: Int): DiaryDay =
        get("/diary?date=$date&timezone_offset_minutes=$timezoneOffsetMinutes", deviceId)

    override suspend fun addDiaryEntry(deviceId: String, mealType: MealType, scanId: String?): CreateDiaryEntryResponse =
        postJson(
            "/diary/entries",
            DiaryEntryRequest(
                device_id = deviceId,
                meal_type = mealType,
                scan_id = scanId,
            ),
        )

    override suspend fun deleteDiaryEntry(deviceId: String, entryId: String) {
        val response = httpClient.delete("${config.apiBaseUrl}/diary/entries/$entryId") {
            header("X-Device-Id", deviceId)
        }
        if (response.status != HttpStatusCode.NoContent && !response.status.isSuccess()) {
            throw mapError(response.status.value, response.bodyAsText())
        }
    }

    override suspend fun getSubscriptionStatus(deviceId: String): SubscriptionStatus =
        get("/subscription/status", deviceId)

    override suspend fun submitBugReport(
        deviceId: String,
        email: String,
        description: String,
        screenshots: List<ByteArray>,
    ): BugReportResult =
        try {
            val response = httpClient.submitFormWithBinaryData(
                url = "${config.apiBaseUrl}/feedback/bug",
                formData = formData {
                    append("email", email)
                    append("description", description)
                    screenshots.forEachIndexed { index, bytes ->
                        append("screenshot", bytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=screenshot$index.jpg")
                        })
                    }
                },
            ) {
                header("X-Device-Id", deviceId)
            }
            if (!response.status.isSuccess()) throw mapError(response.status.value, response.bodyAsText())
            response.body()
        } catch (e: KkalScanException) {
            throw e
        } catch (e: Exception) {
            throw KkalScanException.Network(e.message ?: "Network error")
        }

    private suspend inline fun <reified T> get(path: String, deviceId: String): T =
        try {
            val response = httpClient.get("${config.apiBaseUrl}$path") {
                header("X-Device-Id", deviceId)
            }
            if (!response.status.isSuccess()) throw mapError(response.status.value, response.bodyAsText())
            response.body()
        } catch (e: KkalScanException) {
            throw e
        } catch (e: Exception) {
            throw KkalScanException.Network(e.message ?: "Network error")
        }

    private suspend inline fun <reified T, reified B> postJson(path: String, body: B): T =
        try {
            val response = httpClient.post("${config.apiBaseUrl}$path") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (!response.status.isSuccess()) throw mapError(response.status.value, response.bodyAsText())
            response.body()
        } catch (e: KkalScanException) {
            throw e
        } catch (e: Exception) {
            throw KkalScanException.Network(e.message ?: "Network error")
        }

    private fun mapError(status: Int, body: String): KkalScanException {
        val parsed = runCatching { json.decodeFromString<ApiErrorBody>(body) }.getOrNull()
        if (parsed?.error == "limit_hit") return KkalScanException.LimitHit(parsed.scansLeft ?: 0)
        if (parsed?.error == "bug_report_already_used") {
            return KkalScanException.Api(parsed.message)
        }
        return KkalScanException.Api(parsed?.message ?: "HTTP $status")
    }

    @Serializable
    private data class BonusRequest(val device_id: String)

    @Serializable
    private data class DiaryEntryRequest(
        val device_id: String,
        val meal_type: MealType,
        val scan_id: String? = null,
    )
}
