package dev.m1sk9.lunaticChat.paper.debug

import dev.m1sk9.lunaticChat.engine.debug.DebugLogger
import dev.m1sk9.lunaticChat.engine.debug.DebugState
import dev.m1sk9.lunaticChat.engine.debug.StateDebugLogger
import java.util.logging.Logger

/**
 * Writes debug lines to the server log, tagged with the category that asked for them.
 *
 * INFO rather than FINE: Paper ships a log4j configuration that drops everything below INFO,
 * so a line logged at the level its content deserves would never reach the operator asking
 * for it. The category prefix is what keeps the log readable instead.
 */
class JulDebugLogger(
    logger: Logger,
    state: DebugState,
) : DebugLogger by StateDebugLogger(state, logger::info)
