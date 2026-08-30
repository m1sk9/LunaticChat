package dev.m1sk9.lunaticChat.paper.command.impl.lc

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.engine.debug.DebugState
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DebugCommandTest {
    private val debugState = DebugState()

    private val languageManager =
        mockk<LanguageManager>(relaxed = true) {
            // Echoed back so the assertions read the key the command chose, not a translation.
            every { getMessage(any(), any()) } answers { firstArg() }
            every { getMessage(any()) } answers { firstArg() }
        }

    private val command = DebugCommand(mockk<LunaticChat>(relaxed = true), languageManager, debugState)

    @Test
    fun `switching a category on makes it log`() {
        val result = command.toggle("velocity", on = true)

        assertIs<CommandResult.SuccessWithMessage>(result)
        assertEquals("debug.enabled", result.message)
        assertEquals(setOf(DebugCategory.VELOCITY), debugState.enabled)
    }

    @Test
    fun `switching a category off leaves the others logging`() {
        debugState.replace(setOf(DebugCategory.VELOCITY, DebugCategory.CHAT))

        val result = command.toggle("chat", on = false)

        assertIs<CommandResult.SuccessWithMessage>(result)
        assertEquals("debug.disabled", result.message)
        assertEquals(setOf(DebugCategory.VELOCITY), debugState.enabled)
    }

    @Test
    fun `all switches every category at once`() {
        command.toggle("all", on = true)
        assertEquals(DebugCategory.entries.toSet(), debugState.enabled)

        command.toggle("all", on = false)
        assertEquals(emptySet(), debugState.enabled)
    }

    @Test
    fun `an unknown category is refused without changing what is logging`() {
        debugState.replace(setOf(DebugCategory.VELOCITY))

        val result = command.toggle("proxy", on = true)

        assertIs<CommandResult.Failure>(result)
        assertEquals("debug.unknownCategory", result.message)
        assertEquals(setOf(DebugCategory.VELOCITY), debugState.enabled)
    }

    @Test
    fun `a category is spelled the way config yml spells it, whatever case it was typed in`() {
        val result = command.toggle("VELOCITY", on = true)

        assertIs<CommandResult.SuccessWithMessage>(result)
        assertEquals(setOf(DebugCategory.VELOCITY), debugState.enabled)
    }

    @Test
    fun `listing reports what is logging`() {
        debugState.replace(setOf(DebugCategory.VELOCITY))

        assertIs<CommandResult.SuccessWithMessage>(command.list())
    }
}
