package dev.m1sk9.lunaticChat.paper.storage

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Coalesces a burst of save requests into one asynchronous write.
 *
 * The first [request] after an idle period schedules the write [delaySeconds] later; requests
 * arriving before it fires are absorbed by it, so a player toggling a setting repeatedly costs one
 * file write rather than one per toggle.
 *
 * A request arriving while a write is pending is dropped rather than queued, so one saver serves
 * exactly one file - sharing it would silently lose the other file's save. [FileStore] owns one
 * each so that rule cannot be broken by wiring.
 */
class DebouncedSaver(
    private val scheduler: AsyncScheduler,
    private val delaySeconds: Long = 5,
) {
    private val pending = AtomicBoolean(false)

    /**
     * Schedules [save] to run asynchronously, unless a write is already pending.
     */
    fun request(save: () -> Unit) {
        if (!pending.compareAndSet(false, true)) return

        scheduler.runDelayed(delaySeconds) {
            pending.set(false)
            save()
        }
    }
}
