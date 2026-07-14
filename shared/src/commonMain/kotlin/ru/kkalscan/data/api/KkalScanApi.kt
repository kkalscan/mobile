package ru.kkalscan.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.kkalscan.data.IApiConfig
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.domain.model.ActivityEmulator
import ru.kkalscan.domain.model.ApiErrorBody
import ru.kkalscan.domain.model.BugReportResult
import ru.kkalscan.domain.model.FeatureSearchResult
import ru.kkalscan.domain.model.FoodSearchResult
import ru.kkalscan.domain.model.CreateDiaryEntryResponse
import ru.kkalscan.domain.model.CreateWorkoutResponse
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.Dish
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.domain.model.ProSubscriptionStart
import ru.kkalscan.domain.model.PromoApplyResult
import ru.kkalscan.domain.model.ScanBonusResult
import ru.kkalscan.domain.model.ScanResult
import ru.kkalscan.domain.model.SubscriptionOffers
import ru.kkalscan.domain.model.SubscriptionStatus
import ru.kkalscan.domain.model.WorkoutParseResult
import ru.kkalscan.domain.activity.ActivitySource
import ru.kkalscan.domain.activity.wireName

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

    override suspend fun describeFood(
        deviceId: String,
        description: String,
        timezoneOffsetMinutes: Int,
    ): ScanResult =
        postJson(
            "/scan/text",
            ScanTextRequest(deviceId, description, timezoneOffsetMinutes),
        )

    override suspend fun parseWorkout(deviceId: String, description: String): WorkoutParseResult =
        postJson(
            "/workout/text",
            WorkoutTextRequest(device_id = deviceId, description = description),
        )

    override suspend fun grantScanBonus(deviceId: String): ScanBonusResult =
        postJson("/scan/bonus", BonusRequest(deviceId))

    override suspend fun getDiary(deviceId: String, date: String, timezoneOffsetMinutes: Int): DiaryDay =
        apiGet("/diary?date=$date&timezone_offset_minutes=$timezoneOffsetMinutes", deviceId)

    override suspend fun getActivityEmulator(deviceId: String, timezoneOffsetMinutes: Int): ActivityEmulator =
        apiGet("/activity/emulator?timezone_offset_minutes=$timezoneOffsetMinutes", deviceId)

    override suspend fun addDiaryEntry(
        deviceId: String,
        mealType: MealType,
        scanId: String?,
        dishes: List<Dish>?,
    ): CreateDiaryEntryResponse =
        postJson(
            "/diary/entries",
            DiaryEntryRequest(
                device_id = deviceId,
                meal_type = mealType,
                scan_id = scanId,
                dishes = dishes,
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

    override suspend fun addWorkout(deviceId: String, name: String, kcal: Int): CreateWorkoutResponse =
        postJson(
            "/diary/workouts",
            WorkoutRequest(device_id = deviceId, name = name, kcal = kcal),
        )

    override suspend fun deleteWorkout(deviceId: String, workoutId: String) {
        val response = httpClient.delete("${config.apiBaseUrl}/diary/workouts/$workoutId") {
            header("X-Device-Id", deviceId)
        }
        if (response.status != HttpStatusCode.NoContent && !response.status.isSuccess()) {
            throw mapError(response.status.value, response.bodyAsText())
        }
    }

    override suspend fun syncActivity(
        deviceId: String,
        steps: Int,
        kcal: Int,
        source: ActivitySource,
        timezoneOffsetMinutes: Int,
    ): DiaryDay =
        putJson(
            "/diary/activity",
            ActivitySyncRequest(
                device_id = deviceId,
                steps = steps,
                kcal = kcal,
                source = source.wireName(),
                timezone_offset_minutes = timezoneOffsetMinutes,
            ),
        )

    override suspend fun getSubscriptionStatus(deviceId: String): SubscriptionStatus =
        apiGet("/subscription/status", deviceId)

    override suspend fun getSubscriptionOffers(deviceId: String): SubscriptionOffers =
        apiGet("/subscription/offers?device_id=${deviceId.encodeURLParameter()}", deviceId)

    override suspend fun applyPromo(deviceId: String, promoCode: String): PromoApplyResult =
        postJson("/promo/apply", PromoApplyRequest(device_id = deviceId, promo_code = promoCode))

    override suspend fun startProSubscription(deviceId: String, tariff: String): ProSubscriptionStart =
        postJson("/payments/pro/start", ProSubscriptionStartRequest(deviceId, tariff))

    override suspend fun searchFood(
        deviceId: String,
        query: String,
        limit: Int,
        source: String,
    ): FoodSearchResult {
        val path = buildString {
            append("/food/search?q=")
            append(query.encodeURLParameter())
            append("&limit=$limit&source=")
            append(source.encodeURLParameter())
        }
        return apiGet(path, deviceId)
    }

    override suspend fun searchFeatures(
        deviceId: String,
        query: String,
        limit: Int,
        locale: String,
    ): FeatureSearchResult {
        val path = buildString {
            append("/features/search?q=")
            append(query.encodeURLParameter())
            append("&limit=$limit&locale=")
            append(locale.encodeURLParameter())
        }
        return apiGet(path, deviceId)
    }

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

    private suspend inline fun <reified T> apiGet(path: String, deviceId: String): T =
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

    private suspend inline fun <reified T, reified B> putJson(path: String, body: B): T =
        try {
            val response = httpClient.put("${config.apiBaseUrl}$path") {
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
    private data class ScanTextRequest(
        val device_id: String,
        val description: String,
        val timezone_offset_minutes: Int,
    )

    @Serializable
    private data class ProSubscriptionStartRequest(
        val device_id: String,
        val tariff: String = "pro_monthly_199",
    )

    @Serializable
    private data class PromoApplyRequest(
        val device_id: String,
        val promo_code: String,
    )

    @Serializable
    private data class DiaryEntryRequest(
        val device_id: String,
        val meal_type: MealType,
        val scan_id: String? = null,
        val dishes: List<Dish>? = null,
    )

    @Serializable
    private data class WorkoutTextRequest(
        val device_id: String,
        val description: String,
    )

    @Serializable
    private data class WorkoutRequest(
        val device_id: String,
        val name: String,
        val kcal: Int,
    )

    @Serializable
    private data class ActivitySyncRequest(
        val device_id: String,
        val steps: Int,
        val kcal: Int,
        val source: String,
        val timezone_offset_minutes: Int,
    )
}
