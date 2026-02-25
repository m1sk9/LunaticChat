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
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertIs

class ChannelInfoCommandTest {
    private val testUUID = UUID.fromString("00000001-0000-0000-0000-000000000000")
    private val channelId = "test-channel-1"

    private fun createDependencies(): TestDeps {
        val plugin = mockk<LunaticChat>(relaxed = true)
        val channelManager = mockk<ChannelManager>(relaxed = true)
        val languageManager = mockk<LanguageManager>(relaxed = true)
        every { languageManager.getMessage(any(), any()) } returns "test message"

        val mockPlayer = TestUtils.createMockPlayer(uuid = testUUID, name = "Player1")
        val ctx = mockk<CommandContext>(relaxed = true)
        every { ctx.requirePlayer() } returns mockPlayer

        val command = ChannelInfoCommand(plugin, channelManager, languageManager)
        return TestDeps(command, ctx, channelManager, languageManager, mockPlayer)
    }

    private data class TestDeps(
        val command: ChannelInfoCommand,
        val ctx: CommandContext,
        val channelManager: ChannelManager,
        val languageManager: LanguageManager,
        val mockPlayer: org.bukkit.entity.Player,
    )

    private fun mockBukkitOfflinePlayer() {
        val offlinePlayer = mockk<OfflinePlayer>(relaxed = true)
        every { offlinePlayer.name } returns "Player1"
        every { Bukkit.getOfflinePlayer(any<UUID>()) } returns offlinePlayer
    }

    @Test
    fun `execute with channelId should return Success`() {
        val deps = createDependencies()
        val channel = TestUtils.createTestChannel(id = channelId, ownerId = testUUID)
        val members =
            listOf(
                TestUtils.createTestChannelMember(channelId = channelId, playerId = testUUID),
            )

        every { deps.channelManager.getChannel(channelId) } returns Result.success(channel)
        every { deps.channelManager.getChannelMembers(channelId) } returns Result.success(members)

        mockkStatic(Bukkit::class)
        try {
            mockBukkitOfflinePlayer()

            val result = deps.command.execute(deps.ctx, channelId)

            assertIs<CommandResult.Success>(result)
        } finally {
            unmockkStatic(Bukkit::class)
        }
    }

    @Test
    fun `execute with null channelId should use active channel`() {
        val deps = createDependencies()
        val channel = TestUtils.createTestChannel(id = channelId, ownerId = testUUID)
        val members =
            listOf(
                TestUtils.createTestChannelMember(channelId = channelId, playerId = testUUID),
            )

        every { deps.channelManager.getPlayerChannel(testUUID) } returns channelId
        every { deps.channelManager.getChannel(channelId) } returns Result.success(channel)
        every { deps.channelManager.getChannelMembers(channelId) } returns Result.success(members)

        mockkStatic(Bukkit::class)
        try {
            mockBukkitOfflinePlayer()

            val result = deps.command.execute(deps.ctx, null)

            assertIs<CommandResult.Success>(result)
        } finally {
            unmockkStatic(Bukkit::class)
        }
    }

    @Test
    fun `execute should return Failure when no channel specified and no active channel`() {
        val deps = createDependencies()

        every { deps.channelManager.getPlayerChannel(testUUID) } returns null

        val result = deps.command.execute(deps.ctx, null)

        assertIs<CommandResult.Failure>(result)
    }

    @Test
    fun `execute should return Failure when channel not found`() {
        val deps = createDependencies()

        every { deps.channelManager.getChannel("nonexistent") } returns
            Result.failure(ChannelNotFoundException("nonexistent"))

        val result = deps.command.execute(deps.ctx, "nonexistent")

        assertIs<CommandResult.Failure>(result)
    }
}
