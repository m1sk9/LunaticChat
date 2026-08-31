package dev.m1sk9.lunaticChat.paper.debug

import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.engine.debug.DebugState
import dev.m1sk9.lunaticChat.paper.TestUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JulDebugLoggerTest {
    private val logger = TestUtils.TestLogger()

    @Test
    fun `a disabled category evaluates neither the message nor the log`() {
        var evaluated = false
        val debug = JulDebugLogger(logger, DebugState(setOf(DebugCategory.CHAT)))

        debug.log(DebugCategory.VELOCITY) {
            evaluated = true
            "handshake sent"
        }

        assertFalse(evaluated)
        assertEquals(emptyList(), logger.infoMessages)
    }

    @Test
    fun `an enabled category is logged with the category it came from`() {
        val debug = JulDebugLogger(logger, DebugState(setOf(DebugCategory.VELOCITY)))

        debug.log(DebugCategory.VELOCITY) { "handshake sent" }

        assertEquals(listOf("[LC/velocity] handshake sent"), logger.infoMessages)
    }

    @Test
    fun `isEnabled follows the state as it is changed`() {
        val state = DebugState()
        val debug = JulDebugLogger(logger, state)

        assertFalse(debug.isEnabled(DebugCategory.STORAGE))
        state.set(DebugCategory.STORAGE, on = true)
        assertTrue(debug.isEnabled(DebugCategory.STORAGE))
    }
}
