package dev.m1sk9.lunaticChat.paper.converter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
            // withTimeoutOrNull rather than withTimeout: the latter reports a timeout as a
            // CancellationException, which callers must rethrow rather than degrade. A slow request
            // then reached the delivery queue looking like shutdown and ended the worker, so the
            // sender's later messages were buffered and never delivered.
            withTimeoutOrNull(timeout) {
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
            } ?: throw ConversionTimeoutException(timeout)
        }

    private fun parseResponse(data: String): String {
        val parsed = Json.parseToJsonElement(data)
        val resultArray = parsed.jsonArray

        // Response format: [["input", ["candidate1", "candidate2", ...]], ...]
        // For multi-word input, there are multiple arrays, one per segment
        val result = StringBuilder()

        for (segment in resultArray) {
            val segmentArray = segment.jsonArray
            if (segmentArray.size >= 2) {
                val candidates = segmentArray[1].jsonArray
                if (candidates.isNotEmpty()) {
                    result.append(candidates[0].jsonPrimitive.content)
                }
            }
        }

        if (result.isEmpty()) {
            throw IllegalStateException("No conversion result found in the response.")
        }

        return result.toString()
    }
}
