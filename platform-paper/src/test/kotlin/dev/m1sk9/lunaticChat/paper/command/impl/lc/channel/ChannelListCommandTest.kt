package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertIs

class ChannelListCommandTest {
    private val testUUID = UUID.fromString("00000001-0000-0000-0000-000000000000")

    private fun createDependencies(): TestDeps {
        val plugin = mockk<LunaticChat>(relaxed = true)
        val channelManager = mockk<ChannelManager>(relaxed = true)
        val languageManager = mockk<LanguageManager>(relaxed = true)
        every { languageManager.getMessage(any(), any()) } returns "test message"

        val mockPlayer = TestUtils.createMockPlayer(uuid = testUUID, name = "Player1")
        val ctx = mockk<CommandContext>(relaxed = true)
        every { ctx.requirePlayer() } returns mockPlayer

        val command = ChannelListCommand(plugin, channelManager, languageManager)
        return TestDeps(command, ctx, channelManager, languageManager, mockPlayer)
    }

    private data class TestDeps(
        val command: ChannelListCommand,
        val ctx: CommandContext,
        val channelManager: ChannelManager,
        val languageManager: LanguageManager,
        val mockPlayer: org.bukkit.entity.Player,
    )

    @Test
    fun `execute should return Success with public channels`() {
        val deps = createDependencies()
        val channels =
            listOf(
                TestUtils.createTestChannel(id = "channel-1", name = "Channel 1", ownerId = testUUID),
                TestUtils.createTestChannel(id = "channel-2", name = "Channel 2", ownerId = testUUID),
            )
        val members =
            listOf(
                TestUtils.createTestChannelMember(channelId = "channel-1", playerId = testUUID),
            )

        every { deps.channelManager.getPublicChannels() } returns Result.success(channels)
        every { deps.channelManager.getChannelMembers(any()) } returns Result.success(members)

        val result = deps.command.execute(deps.ctx, 1)

        assertIs<CommandResult.Success>(result)
    }

    @Test
    fun `execute should return Success with empty list message`() {
        val deps = createDependencies()

        every { deps.channelManager.getPublicChannels() } returns Result.success(emptyList())

        val result = deps.command.execute(deps.ctx, 1)

        assertIs<CommandResult.Success>(result)
    }

    @Test
    fun `execute should return Success on page 1 of multiple`() {
        val deps = createDependencies()
        val channels =
            (1..15).map { i ->
                TestUtils.createTestChannel(
                    id = "channel-$i",
                    name = "Channel $i",
                    ownerId = testUUID,
                )
            }
        val members =
            listOf(
                TestUtils.createTestChannelMember(channelId = "channel-1", playerId = testUUID),
            )

        every { deps.channelManager.getPublicChannels() } returns Result.success(channels)
        every { deps.channelManager.getChannelMembers(any()) } returns Result.success(members)

        val result = deps.command.execute(deps.ctx, 1)

        assertIs<CommandResult.Success>(result)
    }

    @Test
    fun `execute should return Failure on error`() {
        val deps = createDependencies()

        every { deps.channelManager.getPublicChannels() } returns Result.failure(ChannelNotFoundException("error"))

        val result = deps.command.execute(deps.ctx, 1)

        assertIs<CommandResult.Failure>(result)
    }
}
