package dev.m1sk9.lunaticChat.engine.debug

/**
 * The debug categories currently switched on, which a reload or `/lc debug` may replace while the
 * server runs.
 *
 * Held apart from the configuration for the same reason the message formats are: taking this type
 * says the value is live, while taking a configuration says it was frozen at startup. Every writer
 * replaces the whole set under a lock, so a reader never sees a half-built one.
 */
class DebugState(
    initial: Set<DebugCategory> = emptySet(),
) {
    @Volatile
    var enabled: Set<DebugCategory> = initial.toSet()
        private set

    fun isEnabled(category: DebugCategory): Boolean = category in enabled

    @Synchronized
    fun replace(categories: Set<DebugCategory>) {
        enabled = categories.toSet()
    }

    @Synchronized
    fun set(
        category: DebugCategory,
        on: Boolean,
    ) {
        enabled = if (on) enabled + category else enabled - category
    }
}
