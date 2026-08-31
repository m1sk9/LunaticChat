package dev.m1sk9.lunaticChat.velocity.debug

import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.engine.debug.DebugState
import io.mockk.mockk
import io.mockk.verify
import org.slf4j.Logger
import kotlin.test.Test
import kotlin.test.assertFalse

class Slf4jDebugLoggerTest {
    private val logger = mockk<Logger>(relaxed = true)

    @Test
    fun `a disabled category evaluates neither the message nor the log`() {
        var evaluated = false
        val debug = Slf4jDebugLogger(logger, DebugState(setOf(DebugCategory.CHAT)))

        debug.log(DebugCategory.VELOCITY) {
            evaluated = true
            "handshake received"
        }

        assertFalse(evaluated)
        verify(exactly = 0) { logger.info(any<String>(), any(), any()) }
    }

    @Test
    fun `an enabled category is logged with the category it came from`() {
        val debug = Slf4jDebugLogger(logger, DebugState(setOf(DebugCategory.VELOCITY)))

        debug.log(DebugCategory.VELOCITY) { "handshake received" }

        verify { logger.info("[LC/{}] {}", "velocity", "handshake received") }
    }
}
