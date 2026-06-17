package dev.m1sk9.lunaticChat.velocity.messaging

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.ServerConnection
import com.velocitypowered.api.proxy.messages.ChannelIdentifier
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.slf4j.Logger
import java.util.Optional
import kotlin.test.Test

class CrossServerDirectMessageRelayTest {
    private fun createRelay(): Pair<CrossServerDirectMessageRelay, ProxyServer> {
        val server = mockk<ProxyServer>(relaxed = true)
        val logger = mockk<Logger>(relaxed = true)
        val relay = CrossServerDirectMessageRelay(server, logger)
        return relay to server
    }

    private fun createRegisteredServer(name: String): RegisteredServer {
        val server = mockk<RegisteredServer>(relaxed = true)
        val serverInfo = mockk<ServerInfo>(relaxed = true)
        every { serverInfo.name } returns name
        every { server.serverInfo } returns serverInfo
        return server
    }

    private fun createPlayer(currentServerName: String?): Player {
        val player = mockk<Player>(relaxed = true)
        if (currentServerName == null) {
            every { player.currentServer } returns Optional.empty()
        } else {
            val connection = mockk<ServerConnection>(relaxed = true)
            val info = mockk<ServerInfo>(relaxed = true)
            every { info.name } returns currentServerName
            every { connection.serverInfo } returns info
            every { player.currentServer } returns Optional.of(connection)
        }
        return player
    }

    private fun createMessage(
        targetName: String = "Recipient",
        targetServerName: String = "survival",
    ): PluginMessage.DirectMessageRelay =
        PluginMessage.DirectMessageRelay(
            messageId = "dm-1",
            sourceServerName = "lobby",
            senderId = "00000001-0000-0000-0000-000000000000",
            senderName = "Sender",
            targetServerName = targetServerName,
            targetName = targetName,
            message = "Hello!",
            timestamp = 1000L,
        )

    @Test
    fun `relay should forward to target server only`() {
        val (relay, proxyServer) = createRelay()
        val sourceServer = createRegisteredServer("lobby")
        val targetServer = createRegisteredServer("survival")
        every { proxyServer.allServers } returns listOf(sourceServer, targetServer)
        every { proxyServer.getPlayer("Recipient") } returns Optional.of(createPlayer("survival"))

        relay.relay(createMessage(), sourceServer)

        verify(exactly = 1) { targetServer.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
        verify(exactly = 0) { sourceServer.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
    }

    @Test
    fun `relay should return SERVER_NOT_FOUND error when target server missing`() {
        val (relay, proxyServer) = createRelay()
        val sourceServer = createRegisteredServer("lobby")
        every { proxyServer.allServers } returns listOf(sourceServer)

        relay.relay(createMessage(targetServerName = "ghost"), sourceServer)

        // Error returned to source; nothing relayed to a target
        verify(exactly = 1) { sourceServer.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
    }

    @Test
    fun `relay should return TARGET_OFFLINE error when player not online`() {
        val (relay, proxyServer) = createRelay()
        val sourceServer = createRegisteredServer("lobby")
        val targetServer = createRegisteredServer("survival")
        every { proxyServer.allServers } returns listOf(sourceServer, targetServer)
        every { proxyServer.getPlayer("Recipient") } returns Optional.empty()

        relay.relay(createMessage(), sourceServer)

        verify(exactly = 1) { sourceServer.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
        verify(exactly = 0) { targetServer.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
    }

    @Test
    fun `relay should return TARGET_OFFLINE error when player on a different server`() {
        val (relay, proxyServer) = createRelay()
        val sourceServer = createRegisteredServer("lobby")
        val targetServer = createRegisteredServer("survival")
        every { proxyServer.allServers } returns listOf(sourceServer, targetServer)
        // Player exists but is connected to "creative", not the requested "survival"
        every { proxyServer.getPlayer("Recipient") } returns Optional.of(createPlayer("creative"))

        relay.relay(createMessage(), sourceServer)

        verify(exactly = 1) { sourceServer.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
        verify(exactly = 0) { targetServer.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
    }
}
