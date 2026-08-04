package dev.m1sk9.lunaticChat.paper.storage

import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists

/**
 * One persisted file, written whole and written atomically.
 *
 * Durability lives here rather than at each write site: a file added later is written atomically
 * because it is a [FileStore], not because its author remembered to reach for the right helper.
 *
 * Each store also owns its own [DebouncedSaver] rather than accepting one, because a saver drops a
 * request while a write is pending and so serves exactly one file. That rule used to hold only
 * because the wiring happened to construct a separate saver per file.
 *
 * Decoding is left to the caller: the stores differ in what an unreadable file means - channels fail
 * loudly, settings fall back to empty - and that is a policy the file cannot know.
 */
class FileStore(
    private val file: Path,
    scheduler: AsyncScheduler,
    private val logger: Logger,
    debounceSeconds: Long = 5,
) {
    private val saver = DebouncedSaver(scheduler, debounceSeconds)

    /** The file's name, for log messages that tell the operator which file went wrong. */
    val name: String get() = file.fileName.toString()

    /** Reads the file, or returns null when it is not there yet. */
    fun read(): String? {
        if (!file.exists()) return null
        return file.bufferedReader().use { it.readText() }
    }

    /** Replaces the file with [contents] so nothing ever reads a half-written file. */
    fun write(contents: String) = file.writeTextAtomically(contents)

    /**
     * Queues a debounced asynchronous write.
     *
     * A failure is reported rather than thrown: there is no caller left to hand it to by the time the
     * write runs, and because the write is atomic the previous file is still intact, so the next save
     * simply tries again.
     *
     * @param contents Supplies what to write. It is called when the write runs rather than when it is
     *   queued, so a burst of changes costs one snapshot and one file write instead of one of each.
     */
    fun queueWrite(contents: () -> String) =
        saver.request {
            try {
                write(contents())
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed to save $name", e)
            }
        }
}
