package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelAlreadyActiveException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotMemberException
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertIs

class ChannelSwitchCommandTest {
    private val testUUID = UUID.fromString("00000001-0000-0000-0000-000000000000")

    private data class Dependencies(
        val ctx: CommandContext,
        val channelManager: ChannelManager,
        val membershipManager: ChannelMembershipManager,
        val languageManager: LanguageManager,
    )

    private fun createDependencies(): Dependencies {
        val player = TestUtils.createMockPlayer(uuid = testUUID, name = "Player1")
        val ctx = mockk<CommandContext>(relaxed = true)
        every { ctx.requirePlayer() } returns player

        val channelManager = mockk<ChannelManager>(relaxed = true)
        val membershipManager = mockk<ChannelMembershipManager>(relaxed = true)
        val languageManager = mockk<LanguageManager>(relaxed = true)
        every { languageManager.getMessage(any(), any()) } returns "test message"
        every { languageManager.getMessage(any()) } returns "test message"

        every { channelManager.getChannel(any()) } returns
            Result.success(
                TestUtils.createTestChannel(id = "test-ch", name = "Test Channel"),
            )

        return Dependencies(ctx, channelManager, membershipManager, languageManager)
    }

    @Test
    fun `execute should return SuccessWithMessage on switch`() {
        val deps = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command = ChannelSwitchCommand(plugin, deps.channelManager, deps.membershipManager, deps.languageManager)

        every { deps.membershipManager.switchChannel(testUUID, "test-ch") } returns Result.success(Unit)

        val result = command.execute(deps.ctx, "test-ch")

        assertIs<CommandResult.SuccessWithMessage>(result)
    }

    @Test
    fun `execute should return Failure when channel not found`() {
        val deps = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command = ChannelSwitchCommand(plugin, deps.channelManager, deps.membershipManager, deps.languageManager)

        every { deps.membershipManager.switchChannel(testUUID, "unknown-ch") } returns
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
        val command = ChannelSwitchCommand(plugin, deps.channelManager, deps.membershipManager, deps.languageManager)

        every { deps.membershipManager.switchChannel(testUUID, "test-ch") } returns
            Result.failure(
                ChannelAlreadyActiveException(testUUID, "test-ch"),
            )

        val result = command.execute(deps.ctx, "test-ch")

        assertIs<CommandResult.Failure>(result)
    }

    @Test
    fun `execute should return Failure when not member`() {
        val deps = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command = ChannelSwitchCommand(plugin, deps.channelManager, deps.membershipManager, deps.languageManager)

        every { deps.membershipManager.switchChannel(testUUID, "other-ch") } returns
            Result.failure(
                ChannelNotMemberException(testUUID, "other-ch"),
            )

        val result = command.execute(deps.ctx, "other-ch")

        assertIs<CommandResult.Failure>(result)
    }
}
