package dev.m1sk9.lunaticChat.velocity.messaging

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.messages.ChannelIdentifier
import com.velocitypowered.api.proxy.server.RegisteredServer
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.slf4j.Logger
import kotlin.test.Test

class CrossServerChatRelayTest {
    private fun createRelay(): Triple<CrossServerChatRelay, ProxyServer, Logger> {
        val server = mockk<ProxyServer>(relaxed = true)
        val logger = mockk<Logger>(relaxed = true)
        val relay = CrossServerChatRelay(server, logger)
        return Triple(relay, server, logger)
    }

    private fun createRegisteredServer(name: String): RegisteredServer {
        val server = mockk<RegisteredServer>(relaxed = true)
        val serverInfo = mockk<com.velocitypowered.api.proxy.server.ServerInfo>(relaxed = true)
        every { serverInfo.name } returns name
        every { server.serverInfo } returns serverInfo
        return server
    }

    private fun createTestMessage(
        messageId: String = "msg-1",
        serverName: String = "lobby",
    ): PluginMessage.GlobalChatMessage =
        PluginMessage.GlobalChatMessage(
            messageId = messageId,
            serverName = serverName,
            playerId = "00000001-0000-0000-0000-000000000000",
            playerName = "TestPlayer",
            message = "Hello!",
            timestamp = 1000L,
        )

    @Test
    fun `relayGlobalMessage should exclude source server`() {
        val (relay, proxyServer, _) = createRelay()
        val sourceServer = createRegisteredServer("lobby")
        val targetServer = createRegisteredServer("survival")

        every { proxyServer.allServers } returns listOf(sourceServer, targetServer)

        relay.relayGlobalMessage(createTestMessage(), sourceServer)

        verify(exactly = 1) { targetServer.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
        verify(exactly = 0) { sourceServer.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
    }

    @Test
    fun `relayGlobalMessage should relay to multiple servers`() {
        val (relay, proxyServer, _) = createRelay()
        val sourceServer = createRegisteredServer("lobby")
        val target1 = createRegisteredServer("survival")
        val target2 = createRegisteredServer("creative")

        every { proxyServer.allServers } returns listOf(sourceServer, target1, target2)

        relay.relayGlobalMessage(createTestMessage(), sourceServer)

        verify(exactly = 1) { target1.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
        verify(exactly = 1) { target2.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
    }

    @Test
    fun `relayGlobalMessage with only source server should relay to zero`() {
        val (relay, proxyServer, logger) = createRelay()
        val sourceServer = createRegisteredServer("lobby")

        every { proxyServer.allServers } returns listOf(sourceServer)

        relay.relayGlobalMessage(createTestMessage(), sourceServer)

        verify(exactly = 0) { sourceServer.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
        verify { logger.info(match { it.contains("0 servers") }) }
    }

    @Test
    fun `relayGlobalMessage should log with messageId and playerName`() {
        val (relay, proxyServer, logger) = createRelay()
        val sourceServer = createRegisteredServer("lobby")
        val targetServer = createRegisteredServer("survival")

        every { proxyServer.allServers } returns listOf(sourceServer, targetServer)

        val message = createTestMessage(messageId = "test-msg-123")
        relay.relayGlobalMessage(message, sourceServer)

        verify {
            logger.info(
                match { msg ->
                    msg.contains("test-msg-123") && msg.contains("TestPlayer")
                },
            )
        }
    }

    @Test
    fun `relayGlobalMessage should log error on exception`() {
        val (relay, proxyServer, logger) = createRelay()
        val sourceServer = createRegisteredServer("lobby")
        val targetServer = createRegisteredServer("survival")

        every { proxyServer.allServers } returns listOf(sourceServer, targetServer)
        every { targetServer.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) } throws
            RuntimeException("connection failed")

        relay.relayGlobalMessage(createTestMessage(), sourceServer)

        verify { logger.error(match { it.contains("Failed to relay") }, any<Exception>()) }
    }

    @Test
    fun `relayGlobalMessage should send encoded message data`() {
        val (relay, proxyServer, _) = createRelay()
        val sourceServer = createRegisteredServer("lobby")
        val targetServer = createRegisteredServer("survival")

        every { proxyServer.allServers } returns listOf(sourceServer, targetServer)

        relay.relayGlobalMessage(createTestMessage(), sourceServer)

        verify {
            targetServer.sendPluginMessage(
                any<ChannelIdentifier>(),
                match<ByteArray> { it.isNotEmpty() },
            )
        }
    }
}
