package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotMemberException
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelNotificationHandler
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertIs

class ChannelLeaveCommandTest {
    private val testUUID = UUID.fromString("00000001-0000-0000-0000-000000000000")

    private data class Dependencies(
        val ctx: CommandContext,
        val channelManager: ChannelManager,
        val membershipManager: ChannelMembershipManager,
        val notificationHandler: ChannelNotificationHandler,
        val languageManager: LanguageManager,
    )

    private fun createDependencies(): Dependencies {
        val player = TestUtils.createMockPlayer(uuid = testUUID, name = "Player1")
        val ctx = mockk<CommandContext>(relaxed = true)
        every { ctx.requirePlayer() } returns player

        val channelManager = mockk<ChannelManager>(relaxed = true)
        val membershipManager = mockk<ChannelMembershipManager>(relaxed = true)
        val notificationHandler = mockk<ChannelNotificationHandler>(relaxed = true)
        val languageManager = mockk<LanguageManager>(relaxed = true)
        every { languageManager.getMessage(any(), any()) } returns "test message"
        every { languageManager.getMessage(any()) } returns "test message"

        return Dependencies(ctx, channelManager, membershipManager, notificationHandler, languageManager)
    }

    @Test
    fun `execute should return SuccessWithMessage on leave`() {
        val deps = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command =
            ChannelLeaveCommand(plugin, deps.channelManager, deps.membershipManager, deps.notificationHandler, deps.languageManager)

        every { deps.channelManager.getPlayerChannel(testUUID) } returns "ch-1"
        every { deps.channelManager.getChannel("ch-1") } returns
            Result.success(
                TestUtils.createTestChannel(id = "ch-1", name = "Channel One"),
            )
        every { deps.membershipManager.leaveChannel(testUUID) } returns Result.success(Unit)

        val result = command.execute(deps.ctx)

        assertIs<CommandResult.SuccessWithMessage>(result)
    }

    @Test
    fun `execute should return Failure when no active channel`() {
        val deps = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command =
            ChannelLeaveCommand(plugin, deps.channelManager, deps.membershipManager, deps.notificationHandler, deps.languageManager)

        every { deps.channelManager.getPlayerChannel(testUUID) } returns null
        every { deps.membershipManager.leaveChannel(testUUID) } returns
            Result.failure(
                ChannelNotMemberException(testUUID, ""),
            )

        val result = command.execute(deps.ctx)

        assertIs<CommandResult.Failure>(result)
    }

    @Test
    fun `execute should broadcast leave notification`() {
        val deps = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command =
            ChannelLeaveCommand(plugin, deps.channelManager, deps.membershipManager, deps.notificationHandler, deps.languageManager)

        every { deps.channelManager.getPlayerChannel(testUUID) } returns "ch-1"
        every { deps.channelManager.getChannel("ch-1") } returns
            Result.success(
                TestUtils.createTestChannel(id = "ch-1", name = "Channel One"),
            )
        every { deps.membershipManager.leaveChannel(testUUID) } returns Result.success(Unit)

        command.execute(deps.ctx)

        verify { deps.notificationHandler.broadcastLeave("ch-1", "Player1") }
    }

    @Test
    fun `execute should show channel name in success message`() {
        val deps = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command =
            ChannelLeaveCommand(plugin, deps.channelManager, deps.membershipManager, deps.notificationHandler, deps.languageManager)

        every { deps.channelManager.getPlayerChannel(testUUID) } returns "ch-1"
        every { deps.channelManager.getChannel("ch-1") } returns
            Result.success(
                TestUtils.createTestChannel(id = "ch-1", name = "My Channel"),
            )
        every { deps.membershipManager.leaveChannel(testUUID) } returns Result.success(Unit)

        val result = command.execute(deps.ctx)

        assertIs<CommandResult.SuccessWithMessage>(result)
        verify {
            deps.languageManager.getMessage(
                "channel.leave.success",
                match { it["channelName"] == "My Channel" },
            )
        }
    }
}
