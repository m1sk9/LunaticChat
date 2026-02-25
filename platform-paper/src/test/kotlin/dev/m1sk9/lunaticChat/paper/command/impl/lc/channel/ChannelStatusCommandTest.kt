package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
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
import org.bukkit.OfflinePlayer
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertIs

class ChannelStatusCommandTest {
    private val testUUID = UUID.fromString("00000001-0000-0000-0000-000000000000")
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

        val command = ChannelStatusCommand(plugin, channelManager, membershipManager, languageManager)
        return TestDeps(command, ctx, channelManager, membershipManager, languageManager, mockPlayer)
    }

    private data class TestDeps(
        val command: ChannelStatusCommand,
        val ctx: CommandContext,
        val channelManager: ChannelManager,
        val membershipManager: ChannelMembershipManager,
        val languageManager: LanguageManager,
        val mockPlayer: org.bukkit.entity.Player,
    )

    private fun mockBukkitOfflinePlayer() {
        val offlinePlayer = mockk<OfflinePlayer>(relaxed = true)
        every { offlinePlayer.name } returns "Player1"
        every { Bukkit.getOfflinePlayer(any<UUID>()) } returns offlinePlayer
    }

    @Test
    fun `execute with active channel should return Success`() {
        val deps = createDependencies()
        val channel = TestUtils.createTestChannel(id = channelId, ownerId = testUUID)
        val members =
            listOf(
                TestUtils.createTestChannelMember(channelId = channelId, playerId = testUUID),
            )

        every { deps.channelManager.getPlayerChannel(testUUID) } returns channelId
        every { deps.channelManager.getChannel(channelId) } returns Result.success(channel)
        every { deps.channelManager.getChannelMembers(channelId) } returns Result.success(members)
        every { deps.membershipManager.getPlayerChannels(testUUID) } returns Result.success(listOf(channelId))

        mockkStatic(Bukkit::class)
        try {
            mockBukkitOfflinePlayer()

            val result = deps.command.execute(deps.ctx)

            assertIs<CommandResult.Success>(result)
        } finally {
            unmockkStatic(Bukkit::class)
        }
    }

    @Test
    fun `execute without active channel should return Success`() {
        val deps = createDependencies()

        every { deps.channelManager.getPlayerChannel(testUUID) } returns null
        every { deps.membershipManager.getPlayerChannels(testUUID) } returns Result.success(emptyList())

        mockkStatic(Bukkit::class)
        try {
            mockBukkitOfflinePlayer()

            val result = deps.command.execute(deps.ctx)

            assertIs<CommandResult.Success>(result)
        } finally {
            unmockkStatic(Bukkit::class)
        }
    }

    @Test
    fun `execute with multiple channels should return Success`() {
        val deps = createDependencies()
        val channelIds = listOf("channel-1", "channel-2", "channel-3")

        every { deps.channelManager.getPlayerChannel(testUUID) } returns "channel-1"
        every { deps.channelManager.getChannel("channel-1") } returns
            Result.success(TestUtils.createTestChannel(id = "channel-1", name = "Channel 1", ownerId = testUUID))
        every { deps.channelManager.getChannel("channel-2") } returns
            Result.success(TestUtils.createTestChannel(id = "channel-2", name = "Channel 2", ownerId = testUUID))
        every { deps.channelManager.getChannel("channel-3") } returns
            Result.success(TestUtils.createTestChannel(id = "channel-3", name = "Channel 3", ownerId = testUUID))
        every { deps.channelManager.getChannelMembers(any()) } returns
            Result.success(
                listOf(TestUtils.createTestChannelMember(playerId = testUUID)),
            )
        every { deps.membershipManager.getPlayerChannels(testUUID) } returns Result.success(channelIds)

        mockkStatic(Bukkit::class)
        try {
            mockBukkitOfflinePlayer()

            val result = deps.command.execute(deps.ctx)

            assertIs<CommandResult.Success>(result)
        } finally {
            unmockkStatic(Bukkit::class)
        }
    }

    @Test
    fun `execute should return Failure on error`() {
        val deps = createDependencies()

        every { deps.channelManager.getPlayerChannel(testUUID) } returns null
        every { deps.membershipManager.getPlayerChannels(testUUID) } returns
            Result.failure(ChannelNotFoundException("error"))

        val result = deps.command.execute(deps.ctx)

        assertIs<CommandResult.Failure>(result)
    }
}
