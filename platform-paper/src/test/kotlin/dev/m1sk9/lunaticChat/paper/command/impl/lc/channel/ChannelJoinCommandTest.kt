package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelAlreadyActiveException
import dev.m1sk9.lunaticChat.engine.exception.ChannelMemberAlreadyException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerBannedException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPrivateRequiresInvitationException
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelNotificationHandler
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertIs

class ChannelJoinCommandTest {
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

        every { channelManager.getChannel(any()) } returns
            Result.success(
                TestUtils.createTestChannel(id = "test-ch", name = "Test Channel"),
            )

        return Dependencies(ctx, channelManager, membershipManager, notificationHandler, languageManager)
    }

    @Test
    fun `execute should return SuccessWithMessage on join`() {
        val deps = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command =
            ChannelJoinCommand(plugin, deps.channelManager, deps.membershipManager, deps.notificationHandler, deps.languageManager)

        every { deps.membershipManager.joinChannel(testUUID, "test-ch") } returns Result.success(Unit)

        val result = command.execute(deps.ctx, "test-ch")

        assertIs<CommandResult.SuccessWithMessage>(result)
    }

    @Test
    fun `execute should return Failure when channel not found`() {
        val deps = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command =
            ChannelJoinCommand(plugin, deps.channelManager, deps.membershipManager, deps.notificationHandler, deps.languageManager)

        every { deps.membershipManager.joinChannel(testUUID, "unknown-ch") } returns
            Result.failure(
                ChannelNotFoundException("unknown-ch"),
            )

        val result = command.execute(deps.ctx, "unknown-ch")

        assertIs<CommandResult.Failure>(result)
    }

    @Test
    fun `execute should return Failure when already active`() {
        val deps = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command =
            ChannelJoinCommand(plugin, deps.channelManager, deps.membershipManager, deps.notificationHandler, deps.languageManager)

        every { deps.membershipManager.joinChannel(testUUID, "test-ch") } returns
            Result.failure(
                ChannelAlreadyActiveException(testUUID, "test-ch"),
            )

        val result = command.execute(deps.ctx, "test-ch")

        assertIs<CommandResult.Failure>(result)
    }

    @Test
    fun `execute should return Failure when already member`() {
        val deps = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command =
            ChannelJoinCommand(plugin, deps.channelManager, deps.membershipManager, deps.notificationHandler, deps.languageManager)

        every { deps.membershipManager.joinChannel(testUUID, "test-ch") } returns
            Result.failure(
                ChannelMemberAlreadyException(testUUID, "test-ch"),
            )

        val result = command.execute(deps.ctx, "test-ch")

        assertIs<CommandResult.Failure>(result)
    }

    @Test
    fun `execute should return Failure when banned`() {
        val deps = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command =
            ChannelJoinCommand(plugin, deps.channelManager, deps.membershipManager, deps.notificationHandler, deps.languageManager)

        every { deps.membershipManager.joinChannel(testUUID, "test-ch") } returns
            Result.failure(
                ChannelPlayerBannedException(testUUID, "test-ch"),
            )

        val result = command.execute(deps.ctx, "test-ch")

        assertIs<CommandResult.Failure>(result)
    }

    @Test
    fun `execute should return Failure when private without invite`() {
        val deps = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command =
            ChannelJoinCommand(plugin, deps.channelManager, deps.membershipManager, deps.notificationHandler, deps.languageManager)

        every { deps.membershipManager.joinChannel(testUUID, "private-ch") } returns
            Result.failure(
                ChannelPrivateRequiresInvitationException(testUUID, "private-ch"),
            )

        val result = command.execute(deps.ctx, "private-ch")

        assertIs<CommandResult.Failure>(result)
    }
}
