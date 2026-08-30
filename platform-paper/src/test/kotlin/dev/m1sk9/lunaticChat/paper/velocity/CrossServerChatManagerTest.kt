package dev.m1sk9.lunaticChat.paper.velocity

import dev.m1sk9.lunaticChat.engine.protocol.PluginMessage
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageChannel
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.config.key.MessageFormatConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals

class CrossServerChatManagerTest {
    private class Fixture(
        cacheSize: Int = 100,
    ) {
        val plugin = mockk<Plugin>(relaxed = true)
        val logger = mockk<Logger>(relaxed = true)
        val config = TestUtils.createTestConfiguration()
        val messageFormats = TestUtils.createTestMessageFormats(config)
        val online = TestUtils.createMockPlayer(name = "Bob")
        val manager = CrossServerChatManager(plugin, logger, config, messageFormats, cacheSize)

        init {
            // Run scheduled main-thread tasks inline so assertions can observe their effects.
            every { plugin.server.scheduler.runTask(plugin, any<Runnable>()) } answers {
                secondArg<Runnable>().run()
                mockk(relaxed = true)
            }
            every { plugin.server.onlinePlayers } returns listOf(online)
        }

        fun broadcastTo(online: Player): String {
            val delivered = slot<Component>()
            verify { online.sendMessage(capture(delivered)) }
            return LegacyComponentSerializer.legacySection().serialize(delivered.captured)
        }
    }

    private fun globalChat(messageId: String = "id-1") =
        PluginMessage.GlobalChatMessage(
            messageId = messageId,
            serverName = "lobby",
            playerId = UUID.randomUUID().toString(),
            playerName = "Alice",
            message = "hi",
        )

    @Test
    fun `sendGlobalMessage relays the message through the plugin channel`() {
        val f = Fixture()
        val senderId = UUID.randomUUID()
        val sender = TestUtils.createMockPlayer(uuid = senderId, name = "Alice")
        every { f.plugin.server.getPlayer(senderId) } returns sender

        f.manager.sendGlobalMessage(senderId, "Alice", "hi")

        verify { sender.sendPluginMessage(f.plugin, PluginMessageChannel.ID, any<ByteArray>()) }
    }

    @Test
    fun `sendGlobalMessage warns when the sender is no longer online`() {
        val f = Fixture()
        every { f.plugin.server.getPlayer(any<UUID>()) } returns null

        f.manager.sendGlobalMessage(UUID.randomUUID(), "Alice", "hi")

        verify { f.logger.warning(any<String>()) }
    }

    @Test
    fun `an incoming message is broadcast in the configured cross-server format`() {
        val f = Fixture()

        f.manager.handleIncomingMessage(globalChat())

        assertEquals("§7[§6lobby§7] §eAlice: §fhi", f.broadcastTo(f.online))
    }

    @Test
    fun `a format replaced while the server runs applies to the next incoming message`() {
        val f = Fixture()
        f.messageFormats.replace(MessageFormatConfig(crossServerGlobalChatFormat = "({server}) {sender}: {message}"))

        f.manager.handleIncomingMessage(globalChat())

        assertEquals("(lobby) Alice: hi", f.broadcastTo(f.online))
    }

    @Test
    fun `a message id already seen is broadcast only once`() {
        val f = Fixture()
        val message = globalChat(messageId = "dup")

        f.manager.handleIncomingMessage(message)
        f.manager.handleIncomingMessage(message)

        verify(exactly = 1) { f.online.sendMessage(any<Component>()) }
    }

    @Test
    fun `pruning the dedup cache still lets new messages through`() {
        val f = Fixture(cacheSize = 1)

        repeat(3) { f.manager.handleIncomingMessage(globalChat(messageId = "id-$it")) }

        verify(exactly = 3) { f.online.sendMessage(any<Component>()) }
    }
}
