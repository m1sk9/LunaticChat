package dev.m1sk9.lunaticChat.engine.permission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LunaticChatPermissionNodeTest {
    private val allNodes =
        listOf(
            LunaticChatPermissionNode.Lc,
            LunaticChatPermissionNode.Tell,
            LunaticChatPermissionNode.Reply,
            LunaticChatPermissionNode.Settings,
            LunaticChatPermissionNode.Status,
            LunaticChatPermissionNode.Channel,
            LunaticChatPermissionNode.ChannelCreate,
            LunaticChatPermissionNode.ChannelList,
            LunaticChatPermissionNode.ChannelJoin,
            LunaticChatPermissionNode.ChannelLeave,
            LunaticChatPermissionNode.ChannelSwitch,
            LunaticChatPermissionNode.ChannelStatus,
            LunaticChatPermissionNode.ChannelInfo,
            LunaticChatPermissionNode.ChannelDelete,
            LunaticChatPermissionNode.ChannelInvite,
            LunaticChatPermissionNode.ChannelKick,
            LunaticChatPermissionNode.ChannelBan,
            LunaticChatPermissionNode.ChannelUnban,
            LunaticChatPermissionNode.ChannelMod,
            LunaticChatPermissionNode.ChannelOwnership,
            LunaticChatPermissionNode.ChatMode,
            LunaticChatPermissionNode.ChatModeToggle,
            LunaticChatPermissionNode.VelocityStatus,
            LunaticChatPermissionNode.Spy,
            LunaticChatPermissionNode.NoticeUpdate,
            LunaticChatPermissionNode.ChannelBypass,
        )

    @Test
    fun `all permission nodes should have lunaticchat prefix`() {
        allNodes.forEach { node ->
            assertTrue(
                node.permissionNode.startsWith("lunaticchat."),
                "Expected '${node.permissionNode}' to start with 'lunaticchat.'",
            )
        }
    }

    @Test
    fun `should have 26 permission nodes`() {
        assertEquals(26, allNodes.size)
    }

    @Test
    fun `spot check specific permission node values`() {
        assertEquals("lunaticchat.command.lc", LunaticChatPermissionNode.Lc.permissionNode)
        assertEquals("lunaticchat.command.tell", LunaticChatPermissionNode.Tell.permissionNode)
        assertEquals("lunaticchat.command.reply", LunaticChatPermissionNode.Reply.permissionNode)
        assertEquals("lunaticchat.command.lc.settings", LunaticChatPermissionNode.Settings.permissionNode)
        assertEquals("lunaticchat.command.lc.channel.create", LunaticChatPermissionNode.ChannelCreate.permissionNode)
        assertEquals("lunaticchat.command.lc.chatmode.toggle", LunaticChatPermissionNode.ChatModeToggle.permissionNode)
        assertEquals("lunaticchat.command.lcv.status", LunaticChatPermissionNode.VelocityStatus.permissionNode)
        assertEquals("lunaticchat.spy", LunaticChatPermissionNode.Spy.permissionNode)
        assertEquals("lunaticchat.noticeupdate", LunaticChatPermissionNode.NoticeUpdate.permissionNode)
        assertEquals("lunaticchat.channelbypass", LunaticChatPermissionNode.ChannelBypass.permissionNode)
    }

    @Test
    fun `all permission node strings should be non-empty`() {
        allNodes.forEach { node ->
            assertTrue(
                node.permissionNode.isNotEmpty(),
                "Permission node should not be empty",
            )
        }
    }

    @Test
    fun `all permission node strings should be unique`() {
        val nodeStrings = allNodes.map { it.permissionNode }
        assertEquals(nodeStrings.size, nodeStrings.toSet().size, "All permission nodes should be unique")
    }
}
