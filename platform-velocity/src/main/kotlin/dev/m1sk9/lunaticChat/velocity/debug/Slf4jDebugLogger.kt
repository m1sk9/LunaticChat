package dev.m1sk9.lunaticChat.velocity.debug

import dev.m1sk9.lunaticChat.engine.debug.DebugLogger
import dev.m1sk9.lunaticChat.engine.debug.DebugState
import dev.m1sk9.lunaticChat.engine.debug.StateDebugLogger
import org.slf4j.Logger

/**
 * Writes debug lines to the proxy log, tagged with the category that asked for them.
 *
 * INFO rather than debug, matching the Paper side: Velocity's stock logger configuration
 * does not print DEBUG, and asking operators to edit it before they can answer a bug report
 * is the friction this whole switch exists to remove.
 */
class Slf4jDebugLogger(
    logger: Logger,
    state: DebugState,
) : DebugLogger by StateDebugLogger(state, logger::info)
