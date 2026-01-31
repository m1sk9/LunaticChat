package dev.m1sk9.lunaticChat.paper.chat.channel

import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelMessageLogEntry
import io.ktor.util.logging.Logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bukkit.plugin.Plugin
import java.io.BufferedWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.io.path.deleteIfExists
import kotlin.io.path.fileSize
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * Asynchronous logger for channel messages.
 *
 * Writes messages in NDJSON format to daily rotated log files.
 * Uses a concurrent queue and scheduled flushing to minimize performance impact.
 *
 * @property logsDirectory Directory where log files are stored
 * @property plugin Bukkit plugin instance for scheduling tasks
 * @property logger Logger for diagnostic messages
 * @property maxFileSizeBytes Maximum size of a single log file
 */
class ChannelMessageLogger(
    private val logsDirectory: Path,
    private val plugin: Plugin,
    private val logger: Logger,
    private val maxFileSizeBytes: Long,
) {
    private val pendingEntries = ConcurrentLinkedQueue<ChannelMessageLogEntry>()
    private val json = Json { encodeDefaults = true }
    private var flushTaskId: Int? = null

    companion object {
        private const val LOG_FILE_PREFIX = "channel-messages-"
        private const val LOG_FILE_EXTENSION = ".json"
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private const val FLUSH_INTERVAL_TICKS = 20L // 1 second
    }

    init {
        // Ensure logs directory exists
        try {
            Files.createDirectories(logsDirectory)
            logger.info("Channel message logger initialized at: $logsDirectory")
            schedulePeriodicFlush()
        } catch (e: Exception) {
            logger.error("Failed to initialize channel message logger", e)
        }
    }

    /**
     * Queues a message for asynchronous logging.
     *
     * @param entry The log entry to write
     */
    fun logMessage(entry: ChannelMessageLogEntry) {
        pendingEntries.offer(entry)
    }

    /**
     * Schedules periodic flushing of pending log entries.
     */
    private fun schedulePeriodicFlush() {
        flushTaskId =
            plugin.server.scheduler
                .runTaskTimerAsynchronously(
                    plugin,
                    Runnable { flushPendingEntries() },
                    FLUSH_INTERVAL_TICKS,
                    FLUSH_INTERVAL_TICKS,
                ).taskId
    }

    /**
     * Flushes all pending entries to the current day's log file.
     */
    private fun flushPendingEntries() {
        if (pendingEntries.isEmpty()) {
            return
        }

        val entries = mutableListOf<ChannelMessageLogEntry>()
        while (true) {
            val entry = pendingEntries.poll() ?: break
            entries.add(entry)
        }

        if (entries.isEmpty()) {
            return
        }

        try {
            val logFile = getCurrentLogFile()

            // Check file size before writing
            if (Files.exists(logFile) && logFile.fileSize() >= maxFileSizeBytes) {
                logger.warn("Log file ${logFile.name} exceeded maximum size, skipping flush")
                return
            }

            BufferedWriter(
                Files.newBufferedWriter(
                    logFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                ),
            ).use { writer ->
                for (entry in entries) {
                    val jsonLine = json.encodeToString(entry)
                    writer.write(jsonLine)
                    writer.newLine()
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to flush log entries", e)
            // Re-queue entries for retry
            entries.forEach { pendingEntries.offer(it) }
        }
    }

    /**
     * Synchronously flushes all pending entries.
     * Should be called during plugin shutdown.
     */
    fun flushSync() {
        // Cancel scheduled task
        flushTaskId?.let { plugin.server.scheduler.cancelTask(it) }

        // Flush remaining entries
        flushPendingEntries()
        logger.info("Channel message logger flushed all pending entries")
    }

    /**
     * Deletes log files older than the specified retention period.
     *
     * @param retentionDays Number of days to retain log files
     */
    fun cleanupOldLogs(retentionDays: Int) {
        if (retentionDays <= 0) {
            return
        }

        try {
            val cutoffDate = LocalDate.now(ZoneOffset.UTC).minusDays(retentionDays.toLong())
            val logFiles = logsDirectory.listDirectoryEntries("$LOG_FILE_PREFIX*$LOG_FILE_EXTENSION")

            var deletedCount = 0
            for (logFile in logFiles) {
                val fileName = logFile.name
                val dateStr = fileName.removePrefix(LOG_FILE_PREFIX).removeSuffix(LOG_FILE_EXTENSION)

                try {
                    val fileDate = LocalDate.parse(dateStr, DATE_FORMATTER)
                    if (fileDate.isBefore(cutoffDate)) {
                        logFile.deleteIfExists()
                        deletedCount++
                        logger.info("Deleted old log file: $fileName")
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to parse date from log file: $fileName", e)
                }
            }

            if (deletedCount > 0) {
                logger.info("Cleaned up $deletedCount old log file(s)")
            }
        } catch (e: Exception) {
            logger.error("Failed to cleanup old logs", e)
        }
    }

    /**
     * Gets the log file path for the current UTC date.
     */
    private fun getCurrentLogFile(): Path {
        val currentDate = LocalDate.now(ZoneOffset.UTC)
        val dateStr = currentDate.format(DATE_FORMATTER)
        val fileName = "$LOG_FILE_PREFIX$dateStr$LOG_FILE_EXTENSION"
        return logsDirectory.resolve(fileName)
    }
}
