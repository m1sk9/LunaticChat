package dev.m1sk9.lunaticChat.paper.velocity

import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Remembers recently seen message IDs so a message relayed back to its origin server is dropped
 * instead of echoed.
 *
 * Entries expire after [CLEANUP_THRESHOLD_MILLIS]; if the cache is still over [cacheSize] after
 * that, the oldest entries go too. Entries are ordered by millisecond timestamp, so a burst of
 * more than [cacheSize] messages inside one millisecond evicts arbitrarily among them.
 *
 * @param cacheSize Soft upper bound on retained entries
 * @param logger Where cleanup failures are reported
 * @param description Names this cache in log output
 */
class MessageDeduplicationCache(
    private val cacheSize: Int,
    private val logger: Logger,
    private val description: String,
) {
    companion object {
        private const val CLEANUP_THRESHOLD_MILLIS = 60_000L
    }

    private val processedMessages = ConcurrentHashMap<String, Long>()

    /**
     * Records [messageId] as seen, evicting stale entries when the cache outgrows [cacheSize].
     */
    fun markProcessed(messageId: String) {
        processedMessages[messageId] = System.currentTimeMillis()
        if (processedMessages.size > cacheSize) {
            evict()
        }
    }

    /**
     * Returns true when [messageId] has not been seen yet.
     */
    fun isNew(messageId: String): Boolean = !processedMessages.containsKey(messageId)

    private fun evict() {
        try {
            val cutoffTime = System.currentTimeMillis() - CLEANUP_THRESHOLD_MILLIS

            val expired = processedMessages.entries.filter { it.value < cutoffTime }.map { it.key }
            expired.forEach { processedMessages.remove(it) }
            var removedCount = expired.size

            if (processedMessages.size > cacheSize) {
                processedMessages.entries
                    .sortedBy { it.value }
                    .take(processedMessages.size - cacheSize)
                    .forEach {
                        processedMessages.remove(it.key)
                        removedCount++
                    }
            }

            if (removedCount > 0) {
                logger.fine("Cleaned up $removedCount old messages from $description dedup cache")
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to clean up $description dedup cache", e)
        }
    }
}
