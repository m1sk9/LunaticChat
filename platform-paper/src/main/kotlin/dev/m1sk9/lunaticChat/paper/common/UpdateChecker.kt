package dev.m1sk9.lunaticChat.paper.common

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.logging.Logger

@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("name")
    val name: String,
    @SerialName("published_at")
    val publishedAt: String,
    @SerialName("html_url")
    val htmlUrl: String,
)

class UpdateChecker(
    private val currentVersion: String,
    private val httpClient: HttpClient,
    private val logger: Logger,
) {
    private val githubAPIURL = "https://api.github.com/repos/m1sk9/LunaticChat/releases/latest"
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Check LunaticChat Updates
     *
     * @throws Exception Failed to check for updates
     */
    suspend fun checkForUpdates(): UpdateCheckResult {
        return withContext(Dispatchers.IO) {
            try {
                val res = httpClient.get(githubAPIURL)
                val release = json.decodeFromString<GitHubRelease>(res.body<String>())
                val latestVersion = release.tagName.removePrefix("v")

                if (!isNewer(latestVersion, currentVersion)) {
                    return@withContext UpdateCheckResult.NotUpdate
                }

                UpdateCheckResult.ExistUpdate
            } catch (e: Exception) {
                logger.warning("Failed to check for latest version of latest version: ${e.message}")
                UpdateCheckResult.FailedUpdate
            }
        }
    }

    private fun isNewer(
        latest: String,
        current: String,
    ): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrNull(i) ?: 0
            val c = currentParts.getOrNull(i) ?: 0
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}

sealed class UpdateCheckResult {
    object ExistUpdate : UpdateCheckResult()

    object NotUpdate : UpdateCheckResult()

    object FailedUpdate : UpdateCheckResult()
}
