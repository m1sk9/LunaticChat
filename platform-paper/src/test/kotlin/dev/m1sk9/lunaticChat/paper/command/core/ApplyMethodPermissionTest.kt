package dev.m1sk9.lunaticChat.paper.command.core

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import io.mockk.every
import io.mockk.mockk
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

@Suppress("UnstableApiUsage")
class ApplyMethodPermissionTest {
    /**
     * Test helper that exposes [LunaticCommand.applyMethodPermission] and has
     * an annotated method for testing.
     */
    private class TestableCommand(
        plugin: LunaticChat,
    ) : LunaticCommand(plugin) {
        override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> = Commands.literal("test")

        /** Expose the protected applyMethodPermission for testing. */
        fun testApplyMethodPermission(
            methodName: String,
            builder: LiteralArgumentBuilder<CommandSourceStack>,
        ): LiteralArgumentBuilder<CommandSourceStack> = applyMethodPermission(methodName, builder)

        @Permission(LunaticChatPermissionNode.Status::class)
        fun annotatedMethod(): LiteralArgumentBuilder<CommandSourceStack> = Commands.literal("annotated")

        fun unannotatedMethod(): LiteralArgumentBuilder<CommandSourceStack> = Commands.literal("unannotated")
    }

    private val plugin = mockk<LunaticChat>(relaxed = true)
    private val command = TestableCommand(plugin)

    @Test
    fun `applyMethodPermission adds requirement when method has Permission annotation`() {
        val builder = Commands.literal("test")
        val result = command.testApplyMethodPermission("annotatedMethod", builder)

        // The builder should have a requirement set (not the default always-true)
        val source = mockk<CommandSourceStack>()
        every { source.sender.hasPermission("lunaticchat.command.lc.status") } returns false
        assertNotNull(result.requirement)
    }

    @Test
    fun `applyMethodPermission returns builder unchanged when method has no Permission annotation`() {
        val builder = Commands.literal("test")
        val result = command.testApplyMethodPermission("unannotatedMethod", builder)

        assertSame(builder, result)
    }

    @Test
    fun `applyMethodPermission returns builder unchanged when method does not exist`() {
        val builder = Commands.literal("test")
        val result = command.testApplyMethodPermission("nonExistentMethod", builder)

        assertSame(builder, result)
    }
}
