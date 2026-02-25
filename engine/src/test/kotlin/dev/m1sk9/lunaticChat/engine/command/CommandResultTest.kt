package dev.m1sk9.lunaticChat.engine.command

import net.kyori.adventure.text.Component
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CommandResultTest {
    @Test
    fun `Success toBrigadierResult should return 1`() {
        assertEquals(1, CommandResult.Success.toBrigadierResult())
    }

    @Test
    fun `SuccessWithMessage toBrigadierResult should return 1`() {
        val result = CommandResult.SuccessWithMessage(Component.text("ok"))
        assertEquals(1, result.toBrigadierResult())
    }

    @Test
    fun `Failure toBrigadierResult should return 0`() {
        val result = CommandResult.Failure(Component.text("error"))
        assertEquals(0, result.toBrigadierResult())
    }

    @Test
    fun `InvalidUsage toBrigadierResult should return 0`() {
        val result = CommandResult.InvalidUsage("/cmd <arg>")
        assertEquals(0, result.toBrigadierResult())
    }

    @Test
    fun `SuccessWithMessage should preserve message`() {
        val message = Component.text("Test message")
        val result = CommandResult.SuccessWithMessage(message)
        assertIs<CommandResult.SuccessWithMessage>(result)
        assertEquals(message, result.message)
    }

    @Test
    fun `Failure should preserve message`() {
        val message = Component.text("Error message")
        val result = CommandResult.Failure(message)
        assertIs<CommandResult.Failure>(result)
        assertEquals(message, result.message)
    }

    @Test
    fun `InvalidUsage should preserve usage hint`() {
        val result = CommandResult.InvalidUsage("/lc setting <key> <on|off>")
        assertIs<CommandResult.InvalidUsage>(result)
        assertEquals("/lc setting <key> <on|off>", result.usageHint)
    }
}
