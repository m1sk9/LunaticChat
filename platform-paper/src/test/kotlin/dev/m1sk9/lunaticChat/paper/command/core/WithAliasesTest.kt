package dev.m1sk9.lunaticChat.paper.command.core

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.paper.LunaticChat
import io.mockk.mockk
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@Suppress("UnstableApiUsage")
class WithAliasesTest {
    /**
     * Test helper that exposes the protected [LunaticCommand.withAliases] method.
     */
    private class TestableCommand(
        plugin: LunaticChat,
    ) : LunaticCommand(plugin) {
        override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> = Commands.literal("test")

        /** Expose the protected withAliases for testing. */
        fun testWithAliases(
            primary: LiteralArgumentBuilder<CommandSourceStack>,
            aliases: List<String>,
        ): List<LiteralArgumentBuilder<CommandSourceStack>> = withAliases(primary, aliases)
    }

    private val plugin = mockk<LunaticChat>(relaxed = true)
    private val command = TestableCommand(plugin)

    @Test
    fun `withAliases with empty list returns only primary`() {
        val primary = Commands.literal("status")
        val result = command.testWithAliases(primary, emptyList())

        assertEquals(1, result.size)
        assertSame(primary, result[0])
    }

    @Test
    fun `withAliases with single alias returns primary and alias`() {
        val primary = Commands.literal("status")
        val result = command.testWithAliases(primary, listOf("st"))

        assertEquals(2, result.size)
        assertSame(primary, result[0])
        assertEquals("st", result[1].literal)
    }

    @Test
    fun `withAliases with multiple aliases returns primary and all aliases`() {
        val primary = Commands.literal("channel")
        val result = command.testWithAliases(primary, listOf("ch", "c"))

        assertEquals(3, result.size)
        assertSame(primary, result[0])
        assertEquals("ch", result[1].literal)
        assertEquals("c", result[2].literal)
    }

    @Test
    fun `withAliases copies children to alias nodes`() {
        val primary = Commands.literal("status")
        val child = Commands.literal("verbose")
        primary.then(child)

        val result = command.testWithAliases(primary, listOf("st"))

        assertEquals(2, result.size)
        // The alias node should have the same children as the primary
        assertTrue(result[1].arguments.toList().isNotEmpty())
        assertEquals(
            primary.arguments.toList().size,
            result[1].arguments.toList().size,
        )
    }

    @Test
    fun `withAliases copies executor to alias nodes`() {
        val primary = Commands.literal("status")
        primary.executes { 1 }

        val result = command.testWithAliases(primary, listOf("st"))

        assertEquals(2, result.size)
        // The alias node should have an executor (command) set
        assertEquals(primary.command, result[1].command)
    }

    @Test
    fun `withAliases copies requirement to alias nodes`() {
        val primary = Commands.literal("status")
        val requirement: java.util.function.Predicate<CommandSourceStack> =
            java.util.function.Predicate { false }
        primary.requires(requirement)

        val result = command.testWithAliases(primary, listOf("st"))

        assertEquals(2, result.size)
        // The alias node should have the same requirement
        assertEquals(primary.requirement, result[1].requirement)
    }

    @Test
    fun `withAliases without executor does not set executor on alias`() {
        val primary = Commands.literal("status")
        // Do not set an executor

        val result = command.testWithAliases(primary, listOf("st"))

        assertEquals(2, result.size)
        assertEquals(null, result[1].command)
    }
}
