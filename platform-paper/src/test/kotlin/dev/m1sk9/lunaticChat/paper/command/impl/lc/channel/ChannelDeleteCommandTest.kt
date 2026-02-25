package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelNoOwnerPermissionException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
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

class ChannelDeleteCommandTest {
    private val testUUID = UUID.fromString("00000001-0000-0000-0000-000000000000")

    private fun createDependencies(hasBypass: Boolean = false): Triple<CommandContext, ChannelManager, LanguageManager> {
        val player = TestUtils.createMockPlayer(uuid = testUUID, name = "Player1")
        every { player.hasPermission(LunaticChatPermissionNode.ChannelBypass.permissionNode) } returns hasBypass

        val ctx = mockk<CommandContext>(relaxed = true)
        every { ctx.requirePlayer() } returns player

        val channelManager = mockk<ChannelManager>(relaxed = true)
        val languageManager = mockk<LanguageManager>(relaxed = true)
        every { languageManager.getMessage(any(), any()) } returns "test message"
        every { languageManager.getMessage(any()) } returns "test message"

        return Triple(ctx, channelManager, languageManager)
    }

    @Test
    fun `execute should return SuccessWithMessage on successful delete`() {
        val (ctx, channelManager, languageManager) = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command = ChannelDeleteCommand(plugin, channelManager, languageManager)

        every { channelManager.deleteChannel("test-ch", testUUID, false) } returns Result.success(Unit)

        val result = command.execute(ctx, "test-ch")

        assertIs<CommandResult.SuccessWithMessage>(result)
    }

    @Test
    fun `execute should return Failure when channel not found`() {
        val (ctx, channelManager, languageManager) = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command = ChannelDeleteCommand(plugin, channelManager, languageManager)

        every { channelManager.deleteChannel("unknown-ch", testUUID, false) } returns
            Result.failure(
                ChannelNotFoundException("unknown-ch"),
            )

        val result = command.execute(ctx, "unknown-ch")

        assertIs<CommandResult.Failure>(result)
    }

    @Test
    fun `execute should return Failure when no owner permission`() {
        val (ctx, channelManager, languageManager) = createDependencies()
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command = ChannelDeleteCommand(plugin, channelManager, languageManager)

        every { channelManager.deleteChannel("other-ch", testUUID, false) } returns
            Result.failure(
                ChannelNoOwnerPermissionException(testUUID),
            )

        val result = command.execute(ctx, "other-ch")

        assertIs<CommandResult.Failure>(result)
    }

    @Test
    fun `execute with bypass permission should pass hasBypass true`() {
        val (ctx, channelManager, languageManager) = createDependencies(hasBypass = true)
        val plugin = mockk<LunaticChat>(relaxed = true)
        val command = ChannelDeleteCommand(plugin, channelManager, languageManager)

        every { channelManager.deleteChannel("test-ch", testUUID, true) } returns Result.success(Unit)

        val result = command.execute(ctx, "test-ch")

        assertIs<CommandResult.SuccessWithMessage>(result)
        verify { channelManager.deleteChannel("test-ch", testUUID, true) }
    }
}
