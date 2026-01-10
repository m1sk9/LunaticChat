package dev.m1sk9.lunaticChat.paper.converter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class GoogleIMEClient(
    private val timeout: Duration = 3.seconds,
    private val httpClient: HttpClient,
) {
    suspend fun convert(input: String): String =
        withContext(Dispatchers.IO) {
            withTimeout(timeout) {
                val res =
                    httpClient.get("https://www.google.com/transliterate") {
                        url {
                            parameters.append("langpair", "ja-Hira|ja")
                            parameters.append("text", input)
                        }
                    }

                if (res.status != io.ktor.http.HttpStatusCode.OK) {
                    throw Exception("Google IME API returned status code: ${res.status.value}")
                }

                val jsonData = res.body<String>()
                parseResponse(jsonData)
            }
        }

    private fun parseResponse(data: String): String {
        val parsed = Json.parseToJsonElement(data)
        val resultArray = parsed.jsonArray

        // Response format: [["input", ["candidate1", "candidate2", ...]], ...]
        if (resultArray.isNotEmpty()) {
            val firstResult = resultArray[0].jsonArray
            if (firstResult.size >= 2) {
                val candidates = firstResult[1].jsonArray
                if (candidates.isNotEmpty()) {
                    return candidates[0].jsonPrimitive.content
                }
            }
        }

        throw IllegalStateException("No conversion result found in the response.")
    }
}
