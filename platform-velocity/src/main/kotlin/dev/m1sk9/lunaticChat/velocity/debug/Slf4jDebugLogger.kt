package dev.m1sk9.lunaticChat.velocity.debug

import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.engine.debug.DebugLogger
import dev.m1sk9.lunaticChat.engine.debug.DebugState
import org.slf4j.Logger

/**
 * Writes debug lines to the proxy log, tagged with the category that asked for them.
 */
class Slf4jDebugLogger(
    private val logger: Logger,
    private val state: DebugState,
) : DebugLogger {
    override fun isEnabled(category: DebugCategory): Boolean = state.isEnabled(category)

    override fun log(
        category: DebugCategory,
        message: () -> String,
    ) {
        if (!state.isEnabled(category)) return
        // INFO rather than debug, matching the Paper side: Velocity's stock logger configuration
        // does not print DEBUG, and asking operators to edit it before they can answer a bug report
        // is the friction this whole switch exists to remove.
        logger.info("[LC/{}] {}", category.key, message())
    }
}
