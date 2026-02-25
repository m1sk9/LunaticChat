package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelLimitExceededException
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertIs

class ChannelCreateCommandTest {
    private val testUUID = UUID.fromString("00000001-0000-0000-0000-000000000000")

    private fun createDependencies(): Triple<CommandContext, ChannelManager, LanguageManager> {
        val player = TestUtils.createMockPlayer(uuid = testUUID, name = "Player1")
        val ctx = mockk<CommandContext>(relaxed = true)
        every { ctx.requirePlayer() } returns player

        val channelManager = mockk<ChannelManager>(relaxed = true)
        val languageManager = mockk<LanguageManager>(relaxed = true)
        every { languageManager.getMessage(any(), any()) } returns "test message"
        every { languageManager.getMessage(any()) } returns "test message"

        return Triple(ctx, channelManager, languageManager)
    }

    @Test
    fun `execute with valid ID should return SuccessWithMessage`() {
        val (ctx, channelManager, languageManager) = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command = ChannelCreateCommand(plugin, channelManager, languageManager)

        every { channelManager.createChannel(any()) } returns
            Result.success(
                TestUtils.createTestChannel(id = "test-ch", name = "Test Channel", ownerId = testUUID),
            )

        val result = command.execute(ctx, "test-ch", "Test Channel", null, false)

        assertIs<CommandResult.SuccessWithMessage>(result)
    }

    @Test
    fun `execute with private channel should return SuccessWithMessage`() {
        val (ctx, channelManager, languageManager) = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command = ChannelCreateCommand(plugin, channelManager, languageManager)

        every { channelManager.createChannel(any()) } returns
            Result.success(
                TestUtils.createTestChannel(id = "private-ch", name = "Private Channel", ownerId = testUUID, isPrivate = true),
            )

        val result = command.execute(ctx, "private-ch", "Private Channel", null, true)

        assertIs<CommandResult.SuccessWithMessage>(result)
    }

    @Test
    fun `execute with invalid ID pattern should return Failure`() {
        val (ctx, channelManager, languageManager) = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command = ChannelCreateCommand(plugin, channelManager, languageManager)

        val result = command.execute(ctx, "ab", "Short ID", null, false)

        assertIs<CommandResult.Failure>(result)
    }

    @Test
    fun `execute when channel already exists should return Failure`() {
        val (ctx, channelManager, languageManager) = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command = ChannelCreateCommand(plugin, channelManager, languageManager)

        every { channelManager.createChannel(any()) } returns
            Result.failure(
                IllegalArgumentException("Channel already exists"),
            )

        val result = command.execute(ctx, "existing-ch", "Existing Channel", null, false)

        assertIs<CommandResult.Failure>(result)
    }

    @Test
    fun `execute when limit exceeded should return Failure`() {
        val (ctx, channelManager, languageManager) = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command = ChannelCreateCommand(plugin, channelManager, languageManager)

        every { channelManager.createChannel(any()) } returns
            Result.failure(
                ChannelLimitExceededException(10),
            )

        val result = command.execute(ctx, "new-channel", "New Channel", null, false)

        assertIs<CommandResult.Failure>(result)
    }

    @Test
    fun `execute with description should pass it to channel`() {
        val (ctx, channelManager, languageManager) = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command = ChannelCreateCommand(plugin, channelManager, languageManager)

        every { channelManager.createChannel(any()) } returns
            Result.success(
                TestUtils.createTestChannel(id = "desc-ch", name = "Desc Channel", description = "test desc", ownerId = testUUID),
            )

        val result = command.execute(ctx, "desc-ch", "Desc Channel", "test desc", false)

        assertIs<CommandResult.SuccessWithMessage>(result)
        verify {
            channelManager.createChannel(
                match { it.description == "test desc" },
            )
        }
    }
}
