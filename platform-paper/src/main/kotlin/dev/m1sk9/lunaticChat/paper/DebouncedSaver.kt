package dev.m1sk9.lunaticChat.paper

import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Coalesces a burst of save requests into one asynchronous write.
 *
 * The first [request] after an idle period schedules the write [delaySeconds] later; requests
 * arriving before it fires are absorbed by it, so a player toggling a setting repeatedly costs one
 * file write rather than one per toggle.
 */
class DebouncedSaver(
    private val plugin: JavaPlugin,
    private val delaySeconds: Long = 5,
) {
    private val pending = AtomicBoolean(false)

    /**
     * Schedules [save] to run asynchronously, unless a write is already pending.
     */
    fun request(save: () -> Unit) {
        if (!pending.compareAndSet(false, true)) return

        plugin.server.asyncScheduler.runDelayed(
            plugin,
            {
                pending.set(false)
                save()
            },
            delaySeconds,
            TimeUnit.SECONDS,
        )
    }
}
