package dev.m1sk9.lunaticChat.paper.velocity

import dev.m1sk9.lunaticChat.engine.protocol.PluginMessage
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageChannel
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageCodec
import dev.m1sk9.lunaticChat.engine.protocol.ProtocolVersion
import dev.m1sk9.lunaticChat.paper.TestUtils
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.plugin.Plugin
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals

private fun handshakeResponse(compatible: Boolean) =
    PluginMessage.HandshakeResponse(
        compatible = compatible,
        velocityVersion = "1.4.0",
        error = if (compatible) null else "forged",
        protocolMajor = ProtocolVersion.MAJOR,
        protocolMinor = ProtocolVersion.MINOR,
        protocolPatch = ProtocolVersion.PATCH,
    )

class VelocityConnectionManagerTest {
    private class Fixture {
        val plugin = mockk<Plugin>(relaxed = true)
        val chatManager = mockk<CrossServerChatManager>(relaxed = true)
        val dmManager = mockk<CrossServerDirectMessageManager>(relaxed = true)
        val registry = mockk<RemotePlayerRegistry>(relaxed = true)
        val player = TestUtils.createMockPlayer(name = "Carrier")
        val manager =
            VelocityConnectionManager(
                plugin = plugin,
                pluginVersion = "1.6.1",
                logger = mockk<Logger>(relaxed = true),
                crossServerChatManager = chatManager,
                crossServerDirectMessageManager = dmManager,
                remotePlayerRegistry = registry,
            )

        fun receive(message: PluginMessage) =
            manager.onPluginMessageReceived(PluginMessageChannel.ID, player, PluginMessageCodec.encode(message))

        fun connect() {
            manager.performHandshake(player)
            receive(handshakeResponse(compatible = true))
        }
    }

    private val globalChat =
        PluginMessage.GlobalChatMessage(messageId = "id", serverName = "lobby", playerId = "x", playerName = "Admin", message = "hi")

    private val directMessage =
        PluginMessage.DirectMessageRelay(
            messageId = "dm",
            sourceServerName = "lobby",
            senderId = "x",
            senderName = "Admin",
            targetServerName = "survival",
            targetName = "Bob",
            message = "hi",
        )

    @Test
    fun `relays received before a handshake are dropped`() {
        val f = Fixture()

        f.receive(globalChat)
        f.receive(directMessage)
        f.receive(PluginMessage.PresenceSnapshot(players = emptyList()))

        verify(exactly = 0) { f.chatManager.handleIncomingMessage(any()) }
        verify(exactly = 0) { f.dmManager.handleIncomingMessage(any()) }
        verify(exactly = 0) { f.registry.replaceAll(any()) }
    }

    @Test
    fun `relays received while the handshake is pending are dropped`() {
        val f = Fixture()
        f.manager.performHandshake(f.player)

        f.receive(globalChat)

        verify(exactly = 0) { f.chatManager.handleIncomingMessage(any()) }
    }

    @Test
    fun `relays are delivered once the handshake has succeeded`() {
        val f = Fixture()
        f.connect()

        f.receive(globalChat)
        f.receive(directMessage)

        verify { f.chatManager.handleIncomingMessage(globalChat) }
        verify { f.dmManager.handleIncomingMessage(directMessage) }
    }

    @Test
    fun `a handshake response nobody asked for does not change the connection state`() {
        val f = Fixture()

        f.receive(handshakeResponse(compatible = true))

        assertEquals(VelocityConnectionManager.ConnectionState.DISCONNECTED, f.manager.getState())
    }

    @Test
    fun `a handshake response after the connection is up does not tear it down`() {
        val f = Fixture()
        f.connect()

        f.receive(handshakeResponse(compatible = false))

        assertEquals(VelocityConnectionManager.ConnectionState.CONNECTED, f.manager.getState())
    }
}
