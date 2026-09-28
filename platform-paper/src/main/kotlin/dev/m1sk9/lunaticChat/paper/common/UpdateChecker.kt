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
    val draft: Boolean = false,
    val prerelease: Boolean = false,
)

class UpdateChecker(
    private val currentVersion: String,
    private val httpClient: HttpClient,
    private val logger: Logger,
) {
    // Not releases/latest: that is whichever release was published last, and a
    // Velocity-only release (velocity/vX.Y.Z) would then hide every Paper update.
    private val githubAPIURL = "https://api.github.com/repos/m1sk9/LunaticChat/releases?per_page=30"
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
                val releases = json.decodeFromString<List<GitHubRelease>>(res.body<String>())
                val latestVersion =
                    latestPaperVersion(releases)
                        ?: return@withContext UpdateCheckResult.NotUpdate

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

    companion object {
        // Paper releases are tagged vX.Y.Z; paper/vX.Y.Z is the retired form still on older releases.
        private val PAPER_TAG = Regex("""^(?:paper/)?v(\d+\.\d+\.\d+)$""")

        internal fun latestPaperVersion(releases: List<GitHubRelease>): String? =
            releases
                .asSequence()
                .filterNot { it.draft || it.prerelease }
                .mapNotNull { PAPER_TAG.matchEntire(it.tagName)?.groupValues?.get(1) }
                .reduceOrNull { newest, candidate -> if (isNewer(candidate, newest)) candidate else newest }

        internal fun isNewer(
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
}

sealed class UpdateCheckResult {
    object ExistUpdate : UpdateCheckResult()

    object NotUpdate : UpdateCheckResult()

    object FailedUpdate : UpdateCheckResult()
}
