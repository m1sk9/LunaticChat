package dev.m1sk9.lunaticChat.engine.debug

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StateDebugLoggerTest {
    private val lines = mutableListOf<String>()

    @Test
    fun `a disabled category evaluates neither the message nor the sink`() {
        var evaluated = false
        val debug = StateDebugLogger(DebugState(setOf(DebugCategory.CHAT)), lines::add)

        debug.log(DebugCategory.VELOCITY) {
            evaluated = true
            "handshake sent"
        }

        assertFalse(evaluated)
        assertEquals(emptyList(), lines)
    }

    @Test
    fun `an enabled category is written with the category it came from`() {
        val debug = StateDebugLogger(DebugState(setOf(DebugCategory.VELOCITY)), lines::add)

        debug.log(DebugCategory.VELOCITY) { "handshake sent" }

        assertEquals(listOf("[LC/velocity] handshake sent"), lines)
    }

    @Test
    fun `isEnabled follows the state as it is changed`() {
        val state = DebugState()
        val debug = StateDebugLogger(state, lines::add)

        assertFalse(debug.isEnabled(DebugCategory.STORAGE))
        state.set(DebugCategory.STORAGE, on = true)
        assertTrue(debug.isEnabled(DebugCategory.STORAGE))
    }
}
