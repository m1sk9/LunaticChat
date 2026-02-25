package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import dev.m1sk9.lunaticChat.engine.chat.channel.Channel
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelNotificationHandler
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs

class ChannelKickCommandTest {
    private val testUUID = UUID.fromString("00000001-0000-0000-0000-000000000000")
    private val targetUUID = UUID.fromString("00000002-0000-0000-0000-000000000000")
    private val channelId = "test-ch"

    private val plugin = mockk<LunaticChat>(relaxed = true)
    private val channelManager = mockk<ChannelManager>(relaxed = true)
    private val membershipManager = mockk<ChannelMembershipManager>(relaxed = true)
    private val notificationHandler = mockk<ChannelNotificationHandler>(relaxed = true)
    private val languageManager = mockk<LanguageManager>(relaxed = true)

    private val command = ChannelKickCommand(plugin, channelManager, membershipManager, notificationHandler, languageManager)

    @BeforeTest
    fun setUp() {
        mockkStatic(Bukkit::class)
        every { languageManager.getMessage(any(), any()) } returns "test message"
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Bukkit::class)
    }

    private fun createContext(): CommandContext {
        val mockPlayer = TestUtils.createMockPlayer(uuid = testUUID, name = "Player1")
        val ctx = mockk<CommandContext>(relaxed = true)
        every { ctx.requirePlayer() } returns mockPlayer
        return ctx
    }

    private fun setupOfflinePlayer(
        hasPlayedBefore: Boolean = true,
        isOnline: Boolean = false,
    ): OfflinePlayer {
        val offlinePlayer = mockk<OfflinePlayer>(relaxed = true)
        every { offlinePlayer.uniqueId } returns targetUUID
        every { offlinePlayer.hasPlayedBefore() } returns hasPlayedBefore
        every { offlinePlayer.isOnline } returns isOnline
        every { offlinePlayer.name } returns "TargetPlayer"
        every { Bukkit.getOfflinePlayer(any<String>()) } returns offlinePlayer
        return offlinePlayer
    }

    @Test
    fun `execute should return SuccessWithMessage on kick`() {
        val ctx = createContext()
        every { channelManager.getPlayerChannel(testUUID) } returns channelId
        every { membershipManager.getMemberRoleOrNull(testUUID, channelId) } returns ChannelRole.OWNER
        setupOfflinePlayer()
        every { Bukkit.getPlayer(any<String>()) } returns null
        every { membershipManager.isMember(targetUUID, channelId) } returns Result.success(true)

        val channel = Channel(id = channelId, name = "Test Channel", ownerId = testUUID, createdAt = 1000L)
        every { channelManager.removeMember(channelId, targetUUID) } returns Result.success(Unit)
        every { channelManager.getPlayerChannel(targetUUID) } returns channelId
        every { channelManager.getChannel(channelId) } returns Result.success(channel)

        val result = command.execute(ctx, "TargetPlayer")

        assertIs<CommandResult.SuccessWithMessage>(result)
        verify { channelManager.removeMember(channelId, targetUUID) }
    }

    @Test
    fun `execute should return Failure when no active channel`() {
        val ctx = createContext()
        every { channelManager.getPlayerChannel(testUUID) } returns null

        val result = command.execute(ctx, "TargetPlayer")

        assertIs<CommandResult.Failure>(result)
    }

    @Test
    fun `execute should return Failure when no permission`() {
        val ctx = createContext()
        every { channelManager.getPlayerChannel(testUUID) } returns channelId
        every { membershipManager.getMemberRoleOrNull(testUUID, channelId) } returns ChannelRole.MEMBER

        val result = command.execute(ctx, "TargetPlayer")

        assertIs<CommandResult.Failure>(result)
    }

    @Test
    fun `execute should return Failure when target not member`() {
        val ctx = createContext()
        every { channelManager.getPlayerChannel(testUUID) } returns channelId
        every { membershipManager.getMemberRoleOrNull(testUUID, channelId) } returns ChannelRole.OWNER
        setupOfflinePlayer()
        every { Bukkit.getPlayer(any<String>()) } returns null
        every { membershipManager.isMember(targetUUID, channelId) } returns Result.success(false)

        val result = command.execute(ctx, "TargetPlayer")

        assertIs<CommandResult.Failure>(result)
    }

    @Test
    fun `execute should return Failure when player not found`() {
        val ctx = createContext()
        every { channelManager.getPlayerChannel(testUUID) } returns channelId
        every { membershipManager.getMemberRoleOrNull(testUUID, channelId) } returns ChannelRole.OWNER
        setupOfflinePlayer(hasPlayedBefore = false, isOnline = false)

        val result = command.execute(ctx, "TargetPlayer")

        assertIs<CommandResult.Failure>(result)
    }
}
