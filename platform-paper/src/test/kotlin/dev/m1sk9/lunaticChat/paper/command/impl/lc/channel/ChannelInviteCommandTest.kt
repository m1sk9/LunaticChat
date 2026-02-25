package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.bukkit.Bukkit
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertIs

class ChannelInviteCommandTest {
    private val testUUID = UUID.fromString("00000001-0000-0000-0000-000000000000")
    private val targetUUID = UUID.fromString("00000002-0000-0000-0000-000000000000")
    private val channelId = "test-channel-1"

    private fun createDependencies(): TestDeps {
        val plugin = mockk<LunaticChat>(relaxed = true)
        val channelManager = mockk<ChannelManager>(relaxed = true)
        val membershipManager = mockk<ChannelMembershipManager>(relaxed = true)
        val languageManager = mockk<LanguageManager>(relaxed = true)
        every { languageManager.getMessage(any(), any()) } returns "test message"

        val mockPlayer = TestUtils.createMockPlayer(uuid = testUUID, name = "Player1")
        val ctx = mockk<CommandContext>(relaxed = true)
        every { ctx.requirePlayer() } returns mockPlayer

        val command = ChannelInviteCommand(plugin, channelManager, membershipManager, languageManager)
        return TestDeps(command, ctx, channelManager, membershipManager, languageManager, mockPlayer)
    }

    private data class TestDeps(
        val command: ChannelInviteCommand,
        val ctx: CommandContext,
        val channelManager: ChannelManager,
        val membershipManager: ChannelMembershipManager,
        val languageManager: LanguageManager,
        val mockPlayer: org.bukkit.entity.Player,
    )

    @Test
    fun `execute should return SuccessWithMessage on invite`() {
        val deps = createDependencies()
        val targetPlayer = TestUtils.createMockPlayer(uuid = targetUUID, name = "TargetPlayer")
        val channel = TestUtils.createTestChannel(id = channelId, ownerId = testUUID)

        every { deps.channelManager.getPlayerChannel(testUUID) } returns channelId
        every { deps.membershipManager.getMemberRoleOrNull(testUUID, channelId) } returns ChannelRole.OWNER
        every { deps.channelManager.isPlayerBanned(channelId, targetUUID) } returns Result.success(false)
        every { deps.membershipManager.joinChannel(targetUUID, channelId, bypassPrivateCheck = true) } returns Result.success(Unit)
        every { deps.channelManager.getChannel(channelId) } returns Result.success(channel)

        mockkStatic(Bukkit::class)
        try {
            every { Bukkit.getPlayer(any<String>()) } returns targetPlayer

            val result = deps.command.execute(deps.ctx, "TargetPlayer")

            assertIs<CommandResult.SuccessWithMessage>(result)
        } finally {
            unmockkStatic(Bukkit::class)
        }
    }

    @Test
    fun `execute should return Failure when inviting self`() {
        val deps = createDependencies()
        val selfTarget = TestUtils.createMockPlayer(uuid = testUUID, name = "Player1")

        every { deps.channelManager.getPlayerChannel(testUUID) } returns channelId
        every { deps.membershipManager.getMemberRoleOrNull(testUUID, channelId) } returns ChannelRole.OWNER

        mockkStatic(Bukkit::class)
        try {
            every { Bukkit.getPlayer(any<String>()) } returns selfTarget

            val result = deps.command.execute(deps.ctx, "Player1")

            assertIs<CommandResult.Failure>(result)
        } finally {
            unmockkStatic(Bukkit::class)
        }
    }

    @Test
    fun `execute should return Failure when target is banned`() {
        val deps = createDependencies()
        val targetPlayer = TestUtils.createMockPlayer(uuid = targetUUID, name = "TargetPlayer")

        every { deps.channelManager.getPlayerChannel(testUUID) } returns channelId
        every { deps.membershipManager.getMemberRoleOrNull(testUUID, channelId) } returns ChannelRole.OWNER
        every { deps.channelManager.isPlayerBanned(channelId, targetUUID) } returns Result.success(true)

        mockkStatic(Bukkit::class)
        try {
            every { Bukkit.getPlayer(any<String>()) } returns targetPlayer

            val result = deps.command.execute(deps.ctx, "TargetPlayer")

            assertIs<CommandResult.Failure>(result)
        } finally {
            unmockkStatic(Bukkit::class)
        }
    }

    @Test
    fun `execute should return Failure when player not found`() {
        val deps = createDependencies()

        every { deps.channelManager.getPlayerChannel(testUUID) } returns channelId
        every { deps.membershipManager.getMemberRoleOrNull(testUUID, channelId) } returns ChannelRole.OWNER

        mockkStatic(Bukkit::class)
        try {
            every { Bukkit.getPlayer(any<String>()) } returns null

            val result = deps.command.execute(deps.ctx, "OfflinePlayer")

            assertIs<CommandResult.Failure>(result)
        } finally {
            unmockkStatic(Bukkit::class)
        }
    }

    @Test
    fun `execute should return Failure when no permission`() {
        val deps = createDependencies()
        val targetPlayer = TestUtils.createMockPlayer(uuid = targetUUID, name = "TargetPlayer")

        every { deps.channelManager.getPlayerChannel(testUUID) } returns channelId
        every { deps.membershipManager.getMemberRoleOrNull(testUUID, channelId) } returns ChannelRole.MEMBER

        mockkStatic(Bukkit::class)
        try {
            every { Bukkit.getPlayer(any<String>()) } returns targetPlayer

            val result = deps.command.execute(deps.ctx, "TargetPlayer")

            assertIs<CommandResult.Failure>(result)
        } finally {
            unmockkStatic(Bukkit::class)
        }
    }
}
