package dev.m1sk9.lunaticChat.paper.command.core

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import io.mockk.every
import io.mockk.mockk
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("UnstableApiUsage")
class LunaticSubCommandTest {
    private class GatedSubCommand(
        plugin: LunaticChat,
        override val permissionNode: LunaticChatPermissionNode?,
        override val aliases: List<String> = emptyList(),
    ) : LunaticSubCommand(plugin) {
        override val literal = "status"

        override fun build(): LiteralArgumentBuilder<CommandSourceStack> = Commands.literal(literal)
    }

    private val plugin = mockk<LunaticChat>(relaxed = true)

    private fun sourceWith(
        permission: String,
        granted: Boolean,
    ): CommandSourceStack =
        mockk<CommandSourceStack>().also {
            every { it.sender.hasPermission(permission) } returns granted
        }

    @Test
    fun `buildAll admits a sender holding the declared permission`() {
        val command = GatedSubCommand(plugin, LunaticChatPermissionNode.Status)

        val primary = command.buildAll().first()

        assertTrue(primary.requirement.test(sourceWith("lunaticchat.command.lc.status", granted = true)))
    }

    @Test
    fun `buildAll rejects a sender lacking the declared permission`() {
        val command = GatedSubCommand(plugin, LunaticChatPermissionNode.Status)

        val primary = command.buildAll().first()

        assertFalse(primary.requirement.test(sourceWith("lunaticchat.command.lc.status", granted = false)))
    }

    @Test
    fun `buildAll leaves the node ungated when no permission is declared`() {
        val command = GatedSubCommand(plugin, permissionNode = null)

        val primary = command.buildAll().first()

        assertTrue(primary.requirement.test(mockk<CommandSourceStack>()))
    }

    @Test
    fun `buildAll gates alias nodes the same as the primary`() {
        val command = GatedSubCommand(plugin, LunaticChatPermissionNode.Status, aliases = listOf("st"))

        val nodes = command.buildAll()

        assertEquals(2, nodes.size)
        assertEquals("st", nodes[1].literal)
        assertFalse(nodes[1].requirement.test(sourceWith("lunaticchat.command.lc.status", granted = false)))
    }
}
