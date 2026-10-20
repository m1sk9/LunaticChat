package dev.m1sk9.lunaticChat.paper.velocity

import java.util.logging.Level
import java.util.logging.Logger

/**
 * Remembers recently seen message IDs so a message relayed back to its origin server is dropped
 * instead of echoed.
 *
 * Entries expire after [CLEANUP_THRESHOLD_MILLIS]; if the cache is still over [cacheSize] after
 * that, the oldest entries go too. Entries are kept in the order they were recorded, so eviction
 * only ever looks at the oldest end and costs O(1) amortized per recorded message.
 *
 * @param cacheSize Soft upper bound on retained entries
 * @param logger Where cleanup failures are reported
 * @param description Names this cache in log output
 * @param clock Source of the timestamps used for expiry
 */
class MessageDeduplicationCache(
    private val cacheSize: Int,
    private val logger: Logger,
    private val description: String,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    companion object {
        private const val CLEANUP_THRESHOLD_MILLIS = 60_000L
    }

    // A single lock rather than a ConcurrentHashMap: eviction depends on recording order, which a
    // concurrent map does not keep, and sorting it by timestamp on every message at capacity is
    // exactly the cost this structure avoids.
    private val lock = Any()
    private val processedMessages = LinkedHashMap<String, Long>()

    /**
     * Records [messageId] as seen, evicting stale entries when the cache outgrows [cacheSize].
     */
    fun markProcessed(messageId: String) {
        synchronized(lock) {
            processedMessages.remove(messageId)
            processedMessages[messageId] = clock()
            if (processedMessages.size > cacheSize) {
                evict()
            }
        }
    }

    /**
     * Returns true when [messageId] has not been seen yet.
     */
    fun isNew(messageId: String): Boolean = synchronized(lock) { !processedMessages.containsKey(messageId) }

    private fun evict() {
        try {
            val cutoffTime = clock() - CLEANUP_THRESHOLD_MILLIS
            val oldestFirst = processedMessages.values.iterator()
            var removedCount = 0

            while (oldestFirst.hasNext()) {
                val recordedAt = oldestFirst.next()
                if (recordedAt >= cutoffTime && processedMessages.size <= cacheSize) break
                oldestFirst.remove()
                removedCount++
            }

            if (removedCount > 0) {
                logger.fine("Cleaned up $removedCount old messages from $description dedup cache")
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to clean up $description dedup cache", e)
        }
    }
}
