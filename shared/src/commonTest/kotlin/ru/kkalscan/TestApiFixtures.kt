package ru.kkalscan

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import ru.kkalscan.data.DefaultApiConfig
import ru.kkalscan.data.api.KkalScanApi
import ru.kkalscan.data.createTestHttpClient

object TestApiFixtures {
    const val DEVICE_ID = "11111111-1111-1111-1111-111111111111"
    const val TODAY = "2026-06-14"

    fun mockEngine(): MockEngine = MockEngine { request ->
        val path = request.url.encodedPath
        when {
            path.endsWith("/scan") && request.method.value == "POST" -> respond(
                content = """
                    {
                      "scan_id": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                      "dishes": [{"name":"Тест","grams":200,"kcal":350,"protein":15,"fat":10,"carbs":40,"fiber":6}],
                      "total_kcal": 350,
                      "total_protein": 15,
                      "total_fat": 10,
                      "total_carbs": 40,
                      "total_fiber": 6,
                      "scans_left": 3,
                      "is_pro": false
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
            path.endsWith("/scan/text") && request.method.value == "POST" -> respond(
                content = """
                    {
                      "scan_id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                      "dishes": [{"name":"Борщ по описанию","grams":300,"kcal":250,"protein":12,"fat":8,"carbs":22,"fiber":5.5}],
                      "total_kcal": 250,
                      "total_protein": 12,
                      "total_fat": 8,
                      "total_carbs": 22,
                      "total_fiber": 5.5,
                      "scans_left": 3,
                      "is_pro": false
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
            path.endsWith("/diary/entries") && request.method.value == "POST" -> respond(
                content = """
                    {
                      "entry": {
                        "id": "entry-1",
                        "created_at": "2026-06-14T12:00:00Z",
                        "meal_type": "lunch",
                        "total_kcal": 350,
                        "dishes": [{"name":"Тест","grams":200,"kcal":350,"protein":15,"fat":10,"carbs":40,"fiber":6}]
                      },
                      "scans_left": 2
                    }
                """.trimIndent(),
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
            path.contains("/activity/emulator") && request.method.value == "GET" -> respond(
                content = """
                    {
                      "mode": "population_default",
                      "estimated_active_kcal": 400,
                      "estimated_steps": 10000,
                      "avg_consumed_kcal_per_day": null,
                      "diary_days_with_entries": 0,
                      "lookback_days": 30
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
            path.contains("/diary") && request.method.value == "GET" -> respond(
                content = """
                    {
                      "date": "$TODAY",
                      "total_kcal": 350,
                      "scans_left": 2,
                      "is_pro": false,
                      "account_linked": false,
                      "linked_providers": [],
                      "entries": [{
                        "id": "entry-1",
                        "created_at": "2026-06-14T12:00:00Z",
                        "meal_type": "lunch",
                        "total_kcal": 350,
                        "dishes": [{"name":"Тест","grams":200,"kcal":350,"protein":15,"fat":10,"carbs":40,"fiber":6}]
                      }]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
            path.endsWith("/scan/bonus") -> respond(
                content = """{"scans_left":5,"bonus_granted":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
            path.endsWith("/subscription/status") -> respond(
                content = """{"is_pro":false,"account_linked":false,"linked_providers":[]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
            path.endsWith("/feedback/bug") && request.method.value == "POST" -> respond(
                content = """
                    {
                      "report_id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                      "is_pro": true,
                      "pro_until": "2026-07-14T12:00:00Z",
                      "message": "Спасибо! Pro на месяц активирован."
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
            request.method.value == "DELETE" -> respond("", HttpStatusCode.NoContent)
            else -> respond("{}", HttpStatusCode.NotFound)
        }
    }

    fun api(): KkalScanApi = KkalScanApi(createTestHttpClient(mockEngine()), DefaultApiConfig)
}
