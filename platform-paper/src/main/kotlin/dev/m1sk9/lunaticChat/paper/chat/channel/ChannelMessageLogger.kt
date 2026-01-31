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
 * @property retentionDays Number of days to retain log files (0 = keep forever)
 */
class ChannelMessageLogger(
    private val logsDirectory: Path,
    private val plugin: Plugin,
    private val logger: Logger,
    private val maxFileSizeBytes: Long,
    private val retentionDays: Int,
) {
    private val pendingEntries = ConcurrentLinkedQueue<ChannelMessageLogEntry>()
    private val json = Json { encodeDefaults = true }
    private var flushTaskId: Int? = null
    private var cleanupTaskId: Int? = null

    companion object {
        private const val LOG_FILE_PREFIX = "channel-messages-"
        private const val LOG_FILE_EXTENSION = ".json"
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private const val FLUSH_INTERVAL_TICKS = 20L // 1 second
        private const val CLEANUP_INTERVAL_TICKS = 24 * 60 * 60 * 20L // 24 hours
        private const val CLEANUP_INITIAL_DELAY_TICKS = 5 * 60 * 20L // 5 minutes
    }

    init {
        // Ensure logs directory exists
        try {
            Files.createDirectories(logsDirectory)
            logger.info("Channel message logger initialized at: $logsDirectory")
            schedulePeriodicFlush()
            schedulePeriodicCleanup()
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
     * Schedules periodic cleanup of old log files.
     */
    private fun schedulePeriodicCleanup() {
        if (retentionDays <= 0) {
            logger.info("Log retention disabled (retentionDays = $retentionDays)")
            return
        }

        cleanupTaskId =
            plugin.server.scheduler
                .runTaskTimerAsynchronously(
                    plugin,
                    Runnable { cleanupOldLogs(retentionDays) },
                    CLEANUP_INITIAL_DELAY_TICKS,
                    CLEANUP_INTERVAL_TICKS,
                ).taskId

        logger.info("Scheduled log cleanup task (retention: $retentionDays days)")
    }

    /**
     * Flushes all pending entries to the current day's log file.
     * Automatically creates new files with suffixes when size limit is exceeded.
     *
     * This method is synchronized to prevent race conditions between periodic
     * flush operations and shutdown flush.
     */
    @Synchronized
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
     * Shuts down the logger by cancelling scheduled tasks and flushing pending entries.
     * Should be called during plugin shutdown.
     */
    fun shutdown() {
        // Cancel scheduled tasks
        flushTaskId?.let { plugin.server.scheduler.cancelTask(it) }
        cleanupTaskId?.let { plugin.server.scheduler.cancelTask(it) }

        // Flush remaining entries
        flushPendingEntries()
        logger.info("Channel message logger shut down (flushed all pending entries)")
    }

    /**
     * Deletes log files older than the specified retention period.
     * Handles both base files (YYYY-MM-DD.json) and suffixed files (YYYY-MM-DD-N.json).
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

            // Pattern: channel-messages-YYYY-MM-DD(-N)?.json
            val datePattern = Regex("""${Regex.escape(LOG_FILE_PREFIX)}(\d{4}-\d{2}-\d{2})(?:-\d+)?${Regex.escape(LOG_FILE_EXTENSION)}""")

            var deletedCount = 0
            for (logFile in logFiles) {
                val fileName = logFile.name
                val matchResult = datePattern.matchEntire(fileName)

                if (matchResult != null) {
                    val dateStr = matchResult.groupValues[1]
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
                } else {
                    logger.warn("Log file name does not match expected pattern: $fileName")
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
     * If the current file exceeds the size limit, returns a new file with a suffix.
     * Filenames follow the pattern: channel-messages-YYYY-MM-DD(-N).json
     */
    private fun getCurrentLogFile(): Path {
        val currentDate = LocalDate.now(ZoneOffset.UTC)
        val dateStr = currentDate.format(DATE_FORMATTER)

        // Try base filename first
        var fileName = "$LOG_FILE_PREFIX$dateStr$LOG_FILE_EXTENSION"
        var logFile = logsDirectory.resolve(fileName)

        // If file exists and exceeds size limit, find next available suffix
        var suffix = 1
        while (Files.exists(logFile) && logFile.fileSize() >= maxFileSizeBytes) {
            fileName = "$LOG_FILE_PREFIX$dateStr-$suffix$LOG_FILE_EXTENSION"
            logFile = logsDirectory.resolve(fileName)
            suffix++

            // Safety limit to prevent infinite loop
            if (suffix > 1000) {
                logger.error("Too many log files for date $dateStr (limit: 1000), using latest")
                break
            }
        }

        return logFile
    }
}
