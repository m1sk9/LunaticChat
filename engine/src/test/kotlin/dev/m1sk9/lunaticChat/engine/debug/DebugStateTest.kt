package dev.m1sk9.lunaticChat.engine.debug

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugStateTest {
    @Test
    fun `a state starts on the categories it was built with`() {
        val state = DebugState(setOf(DebugCategory.VELOCITY))

        assertTrue(state.isEnabled(DebugCategory.VELOCITY))
        assertFalse(state.isEnabled(DebugCategory.CHAT))
    }

    @Test
    fun `replace drops the categories it does not list`() {
        val state = DebugState(setOf(DebugCategory.VELOCITY, DebugCategory.CHAT))

        state.replace(setOf(DebugCategory.PROTOCOL))

        assertEquals(setOf(DebugCategory.PROTOCOL), state.enabled)
    }

    @Test
    fun `set switches one category without touching the others`() {
        val state = DebugState(setOf(DebugCategory.VELOCITY))

        state.set(DebugCategory.CHAT, on = true)
        state.set(DebugCategory.VELOCITY, on = false)

        assertEquals(setOf(DebugCategory.CHAT), state.enabled)
    }

    @Test
    fun `a state built from a set is unaffected by later changes to it`() {
        val categories = mutableSetOf(DebugCategory.VELOCITY)
        val state = DebugState(categories)

        categories += DebugCategory.CHAT

        assertEquals(setOf(DebugCategory.VELOCITY), state.enabled)
    }
}
