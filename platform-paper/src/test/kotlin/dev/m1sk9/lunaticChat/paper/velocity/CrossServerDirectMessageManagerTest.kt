package dev.m1sk9.lunaticChat.paper.velocity

import dev.m1sk9.lunaticChat.engine.protocol.PluginMessage
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.chat.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.kyori.adventure.text.Component
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.Test

class CrossServerDirectMessageManagerTest {
    private class Fixture(
        cacheSize: Int = 100,
    ) {
        val plugin = mockk<Plugin>(relaxed = true)
        val logger = mockk<Logger>(relaxed = true)
        val config = TestUtils.createTestConfiguration()
        val dmHandler = mockk<DirectMessageHandler>(relaxed = true)
        val languageManager = mockk<LanguageManager>(relaxed = true)
        val manager =
            CrossServerDirectMessageManager(plugin, logger, config, dmHandler, languageManager, cacheSize)

        init {
            // Run scheduled main-thread tasks inline so assertions can observe their effects.
            every { plugin.server.scheduler.runTask(plugin, any<Runnable>()) } answers {
                secondArg<Runnable>().run()
                mockk(relaxed = true)
            }
            every { languageManager.getMessage(any(), any()) } returns "msg"
        }
    }

    private fun relay(
        messageId: String = "id-1",
        targetName: String = "Bob",
        sourceServer: String = "lobby",
    ) = PluginMessage.DirectMessageRelay(
        messageId = messageId,
        sourceServerName = sourceServer,
        senderId = UUID.randomUUID().toString(),
        senderName = "Alice",
        targetServerName = "survival",
        targetName = targetName,
        message = "hi",
    )

    @Test
    fun `sendCrossServerMessage relays via plugin channel and delegates display`() {
        val f = Fixture()
        val sender = TestUtils.createMockPlayer(name = "Alice")
        every { f.dmHandler.handleOutgoingCrossServerMessage(sender, "Bob", "survival", "hi") } returns "hi"

        f.manager.sendCrossServerMessage(sender, "Bob", "survival", "hi")

        verify { f.dmHandler.handleOutgoingCrossServerMessage(sender, "Bob", "survival", "hi") }
        verify { sender.sendPluginMessage(f.plugin, "lunaticchat:main", any<ByteArray>()) }
    }

    @Test
    fun `handleIncomingMessage delivers to the local recipient`() {
        val f = Fixture()
        val recipient = TestUtils.createMockPlayer(name = "Bob")
        every { f.plugin.server.getPlayer("Bob") } returns recipient

        f.manager.handleIncomingMessage(relay())

        verify { f.dmHandler.handleIncomingCrossServerMessage(recipient, "Alice", "lobby", "hi") }
    }

    @Test
    fun `handleIncomingMessage ignores duplicate message ids`() {
        val f = Fixture()
        val recipient = TestUtils.createMockPlayer(name = "Bob")
        every { f.plugin.server.getPlayer("Bob") } returns recipient
        val message = relay(messageId = "dup")

        f.manager.handleIncomingMessage(message)
        f.manager.handleIncomingMessage(message)

        verify(exactly = 1) {
            f.dmHandler.handleIncomingCrossServerMessage(any(), any(), any(), any())
        }
    }

    @Test
    fun `handleIncomingMessage does not deliver when recipient is offline`() {
        val f = Fixture()
        every { f.plugin.server.getPlayer("Ghost") } returns null

        f.manager.handleIncomingMessage(relay(targetName = "Ghost"))

        verify(exactly = 0) {
            f.dmHandler.handleIncomingCrossServerMessage(any(), any(), any(), any())
        }
    }

    @Test
    fun `handleError notifies the original sender`() {
        val f = Fixture()
        val senderId = UUID.randomUUID()
        val sender = TestUtils.createMockPlayer(uuid = senderId, name = "Alice")
        every { f.plugin.server.getPlayer(senderId) } returns sender

        val error =
            PluginMessage.DirectMessageError(
                messageId = "id",
                senderId = senderId.toString(),
                targetName = "Bob",
                targetServerName = "survival",
                reason = PluginMessage.DirectMessageError.Reason.TARGET_OFFLINE,
            )
        f.manager.handleError(error)

        verify { sender.sendMessage(any<Component>()) }
    }

    @Test
    fun `handleError with malformed sender id is ignored`() {
        val f = Fixture()

        val error =
            PluginMessage.DirectMessageError(
                messageId = "id",
                senderId = "not-a-uuid",
                targetName = "Bob",
                targetServerName = "survival",
                reason = PluginMessage.DirectMessageError.Reason.SERVER_NOT_FOUND,
            )

        // Should not throw.
        f.manager.handleError(error)
    }

    @Test
    fun `sendCrossServerMessage prunes the dedup cache when over capacity`() {
        val f = Fixture(cacheSize = 1)
        val sender = TestUtils.createMockPlayer(name = "Alice")
        every { f.dmHandler.handleOutgoingCrossServerMessage(any(), any(), any(), any()) } returns "hi"

        repeat(3) { f.manager.sendCrossServerMessage(sender, "Bob$it", "survival", "hi") }

        verify(atLeast = 1) { sender.sendPluginMessage(f.plugin, "lunaticchat:main", any<ByteArray>()) }
    }
}
