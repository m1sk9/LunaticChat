package dev.m1sk9.lunaticChat.engine.debug

/**
 * Writes a diagnostic line, but only for the categories that are switched on.
 *
 * [message] is a lambda rather than a String so that instrumenting a path taken once per chat
 * message costs nothing while debugging is off: the interpolation never runs. The
 * `if (debugMode) logger.info("... $word")` guards this replaced paid for their strings either way.
 */
interface DebugLogger {
    fun isEnabled(category: DebugCategory): Boolean

    fun log(
        category: DebugCategory,
        message: () -> String,
    )

    /** A logger for callers that have no debug state to consult, such as tests of unrelated code. */
    object Disabled : DebugLogger {
        override fun isEnabled(category: DebugCategory): Boolean = false

        override fun log(
            category: DebugCategory,
            message: () -> String,
        ) = Unit
    }
}
