package dev.m1sk9.lunaticChat.velocity.messaging

import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.ServerConnection
import com.velocitypowered.api.proxy.messages.ChannelIdentifier
import com.velocitypowered.api.proxy.messages.ChannelMessageSink
import com.velocitypowered.api.proxy.messages.ChannelMessageSource
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessage
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageCodec
import dev.m1sk9.lunaticChat.engine.protocol.ProtocolVersion
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.slf4j.Logger
import kotlin.test.Test

class PluginMessageHandlerTest {
    private val channel = MinecraftChannelIdentifier.create("lunaticchat", "main")

    private fun createHandler(pluginVersion: String = "0.10.0"): Triple<PluginMessageHandler, ProxyServer, CrossServerChatRelay> {
        val plugin = Any()
        val server = mockk<ProxyServer>(relaxed = true)
        val logger = mockk<Logger>(relaxed = true)
        val relay = mockk<CrossServerChatRelay>(relaxed = true)

        val handler = PluginMessageHandler(plugin, server, logger, pluginVersion, relay)
        return Triple(handler, server, relay)
    }

    private fun createServerConnection(serverName: String = "lobby"): ServerConnection {
        val connection = mockk<ServerConnection>(relaxed = true)
        val serverInfo = mockk<ServerInfo>(relaxed = true)
        val registeredServer = mockk<RegisteredServer>(relaxed = true)
        every { serverInfo.name } returns serverName
        every { connection.serverInfo } returns serverInfo
        every { connection.server } returns registeredServer
        return connection
    }

    private fun createPluginMessageEvent(
        source: ChannelMessageSource,
        target: ChannelMessageSink,
        identifier: ChannelIdentifier,
        data: ByteArray,
    ): PluginMessageEvent = PluginMessageEvent(source, target, identifier, data)

    @Test
    fun `onPluginMessage should handle successful handshake`() {
        val (handler, _, _) = createHandler(pluginVersion = "0.10.0")
        val connection = createServerConnection()

        val handshake =
            PluginMessage.Handshake(
                pluginVersion = "0.10.0",
                protocolMajor = ProtocolVersion.MAJOR,
                protocolMinor = ProtocolVersion.MINOR,
                protocolPatch = ProtocolVersion.PATCH,
            )
        val data = PluginMessageCodec.encode(handshake)
        val event = createPluginMessageEvent(connection, mockk(relaxed = true), channel, data)

        handler.onPluginMessage(event)

        verify { connection.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
    }

    @Test
    fun `onPluginMessage should reject version mismatch handshake`() {
        val (handler, _, _) = createHandler(pluginVersion = "0.10.0")
        val connection = createServerConnection()

        val handshake =
            PluginMessage.Handshake(
                pluginVersion = "0.9.0",
                protocolMajor = ProtocolVersion.MAJOR,
                protocolMinor = ProtocolVersion.MINOR,
                protocolPatch = ProtocolVersion.PATCH,
            )
        val data = PluginMessageCodec.encode(handshake)
        val event = createPluginMessageEvent(connection, mockk(relaxed = true), channel, data)

        handler.onPluginMessage(event)

        verify { connection.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
    }

    @Test
    fun `onPluginMessage should reject protocol mismatch handshake`() {
        val (handler, _, _) = createHandler(pluginVersion = "0.10.0")
        val connection = createServerConnection()

        val handshake =
            PluginMessage.Handshake(
                pluginVersion = "0.10.0",
                protocolMajor = ProtocolVersion.MAJOR + 1,
                protocolMinor = ProtocolVersion.MINOR,
                protocolPatch = ProtocolVersion.PATCH,
            )
        val data = PluginMessageCodec.encode(handshake)
        val event = createPluginMessageEvent(connection, mockk(relaxed = true), channel, data)

        handler.onPluginMessage(event)

        verify { connection.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
    }

    @Test
    fun `onPluginMessage should handle status request`() {
        val (handler, _, _) = createHandler()
        val connection = createServerConnection()

        val data = PluginMessageCodec.encode(PluginMessage.StatusRequest)
        val event = createPluginMessageEvent(connection, mockk(relaxed = true), channel, data)

        handler.onPluginMessage(event)

        verify { connection.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
    }

    @Test
    fun `onPluginMessage should relay global chat message`() {
        val (handler, _, relay) = createHandler()
        val connection = createServerConnection()

        val chatMessage =
            PluginMessage.GlobalChatMessage(
                messageId = "msg-1",
                serverName = "lobby",
                playerId = "00000001-0000-0000-0000-000000000000",
                playerName = "TestPlayer",
                message = "Hello!",
                timestamp = 1000L,
            )
        val data = PluginMessageCodec.encode(chatMessage)
        val event = createPluginMessageEvent(connection, mockk(relaxed = true), channel, data)

        handler.onPluginMessage(event)

        verify { relay.relayGlobalMessage(any<PluginMessage.GlobalChatMessage>(), any<RegisteredServer>()) }
    }

    @Test
    fun `onPluginMessage should ignore messages from wrong channel`() {
        val (handler, _, relay) = createHandler()
        val connection = createServerConnection()

        val wrongChannel = MinecraftChannelIdentifier.create("other", "channel")
        val data = PluginMessageCodec.encode(PluginMessage.StatusRequest)
        val event = createPluginMessageEvent(connection, mockk(relaxed = true), wrongChannel, data)

        handler.onPluginMessage(event)

        verify(exactly = 0) { relay.relayGlobalMessage(any<PluginMessage.GlobalChatMessage>(), any<RegisteredServer>()) }
    }

    @Test
    fun `onPluginMessage should ignore messages from non-server source`() {
        val (handler, _, relay) = createHandler()
        val source = mockk<ChannelMessageSource>(relaxed = true)

        val data = PluginMessageCodec.encode(PluginMessage.StatusRequest)
        val event = createPluginMessageEvent(source, mockk(relaxed = true), channel, data)

        handler.onPluginMessage(event)

        verify(exactly = 0) { relay.relayGlobalMessage(any<PluginMessage.GlobalChatMessage>(), any<RegisteredServer>()) }
    }

    @Test
    fun `initialize should register channel and listener`() {
        val (handler, server, _) = createHandler()

        handler.initialize()

        verify { server.channelRegistrar.register(any<ChannelIdentifier>()) }
        verify { server.eventManager.register(any(), handler) }
    }

    @Test
    fun `shutdown should unregister listener`() {
        val (handler, server, _) = createHandler()

        handler.shutdown()

        verify { server.eventManager.unregisterListener(any(), handler) }
    }

    @Test
    fun `handshake with matching patch difference should succeed`() {
        val (handler, _, _) = createHandler(pluginVersion = "0.10.0")
        val connection = createServerConnection()

        val handshake =
            PluginMessage.Handshake(
                pluginVersion = "0.10.0",
                protocolMajor = ProtocolVersion.MAJOR,
                protocolMinor = ProtocolVersion.MINOR,
                protocolPatch = ProtocolVersion.PATCH + 5,
            )
        val data = PluginMessageCodec.encode(handshake)
        val event = createPluginMessageEvent(connection, mockk(relaxed = true), channel, data)

        handler.onPluginMessage(event)

        verify { connection.sendPluginMessage(any<ChannelIdentifier>(), any<ByteArray>()) }
    }
}
