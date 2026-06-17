package dev.m1sk9.lunaticChat.engine.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class PluginMessageCodecTest {
    @Test
    fun `encode and decode Handshake round-trip`() {
        val original =
            PluginMessage.Handshake(
                pluginVersion = "0.10.0",
                protocolMajor = 1,
                protocolMinor = 0,
                protocolPatch = 0,
            )

        val encoded = PluginMessageCodec.encode(original)
        val decoded = PluginMessageCodec.decode(encoded)

        assertIs<PluginMessage.Handshake>(decoded)
        assertEquals(original.pluginVersion, decoded.pluginVersion)
        assertEquals(original.protocolMajor, decoded.protocolMajor)
        assertEquals(original.protocolMinor, decoded.protocolMinor)
        assertEquals(original.protocolPatch, decoded.protocolPatch)
    }

    @Test
    fun `encode and decode HandshakeResponse compatible round-trip`() {
        val original =
            PluginMessage.HandshakeResponse(
                compatible = true,
                velocityVersion = "0.10.0",
                error = null,
            )

        val encoded = PluginMessageCodec.encode(original)
        val decoded = PluginMessageCodec.decode(encoded)

        assertIs<PluginMessage.HandshakeResponse>(decoded)
        assertEquals(original.compatible, decoded.compatible)
        assertEquals(original.velocityVersion, decoded.velocityVersion)
        assertEquals(original.error, decoded.error)
    }

    @Test
    fun `encode and decode HandshakeResponse with protocol version fields`() {
        val original =
            PluginMessage.HandshakeResponse(
                compatible = true,
                velocityVersion = "0.11.0",
                protocolMajor = 1,
                protocolMinor = 1,
                protocolPatch = 0,
            )

        val encoded = PluginMessageCodec.encode(original)
        val decoded = PluginMessageCodec.decode(encoded)

        assertIs<PluginMessage.HandshakeResponse>(decoded)
        assertEquals(original.compatible, decoded.compatible)
        assertEquals(original.velocityVersion, decoded.velocityVersion)
        assertEquals(1, decoded.protocolMajor)
        assertEquals(1, decoded.protocolMinor)
        assertEquals(0, decoded.protocolPatch)
    }

    @Test
    fun `encode and decode HandshakeResponse incompatible round-trip`() {
        val original =
            PluginMessage.HandshakeResponse(
                compatible = false,
                velocityVersion = "0.10.0",
                error = "Version mismatch",
            )

        val encoded = PluginMessageCodec.encode(original)
        val decoded = PluginMessageCodec.decode(encoded)

        assertIs<PluginMessage.HandshakeResponse>(decoded)
        assertEquals(false, decoded.compatible)
        assertEquals("Version mismatch", decoded.error)
    }

    @Test
    fun `encode and decode StatusRequest round-trip`() {
        val original = PluginMessage.StatusRequest

        val encoded = PluginMessageCodec.encode(original)
        val decoded = PluginMessageCodec.decode(encoded)

        assertIs<PluginMessage.StatusRequest>(decoded)
    }

    @Test
    fun `encode and decode StatusResponse round-trip`() {
        val original =
            PluginMessage.StatusResponse(
                velocityVersion = "0.10.0",
                protocolVersion = "1.0.0",
                online = true,
            )

        val encoded = PluginMessageCodec.encode(original)
        val decoded = PluginMessageCodec.decode(encoded)

        assertIs<PluginMessage.StatusResponse>(decoded)
        assertEquals(original.velocityVersion, decoded.velocityVersion)
        assertEquals(original.protocolVersion, decoded.protocolVersion)
        assertEquals(original.online, decoded.online)
    }

    @Test
    fun `encode and decode GlobalChatMessage round-trip`() {
        val original =
            PluginMessage.GlobalChatMessage(
                messageId = "test-id-123",
                serverName = "lobby",
                playerId = "00000001-0000-0000-0000-000000000000",
                playerName = "TestPlayer",
                message = "Hello, world!",
                timestamp = 1000L,
            )

        val encoded = PluginMessageCodec.encode(original)
        val decoded = PluginMessageCodec.decode(encoded)

        assertIs<PluginMessage.GlobalChatMessage>(decoded)
        assertEquals(original.messageId, decoded.messageId)
        assertEquals(original.serverName, decoded.serverName)
        assertEquals(original.playerId, decoded.playerId)
        assertEquals(original.playerName, decoded.playerName)
        assertEquals(original.message, decoded.message)
        assertEquals(original.timestamp, decoded.timestamp)
    }

    @Test
    fun `encode and decode DirectMessageRelay round-trip`() {
        val original =
            PluginMessage.DirectMessageRelay(
                messageId = "dm-id-123",
                sourceServerName = "survival",
                senderId = "00000004-0000-0000-0000-000000000000",
                senderName = "Sender",
                targetServerName = "lobby",
                targetName = "Recipient",
                message = "Hello across servers!",
                timestamp = 4000L,
            )

        val encoded = PluginMessageCodec.encode(original)
        val decoded = PluginMessageCodec.decode(encoded)

        assertIs<PluginMessage.DirectMessageRelay>(decoded)
        assertEquals(original.messageId, decoded.messageId)
        assertEquals(original.sourceServerName, decoded.sourceServerName)
        assertEquals(original.senderId, decoded.senderId)
        assertEquals(original.senderName, decoded.senderName)
        assertEquals(original.targetServerName, decoded.targetServerName)
        assertEquals(original.targetName, decoded.targetName)
        assertEquals(original.message, decoded.message)
        assertEquals(original.timestamp, decoded.timestamp)
    }

    @Test
    fun `encode and decode DirectMessageError round-trip`() {
        val original =
            PluginMessage.DirectMessageError(
                messageId = "dm-id-456",
                senderId = "00000005-0000-0000-0000-000000000000",
                targetName = "Ghost",
                targetServerName = "lobby",
                reason = PluginMessage.DirectMessageError.Reason.TARGET_OFFLINE,
            )

        val encoded = PluginMessageCodec.encode(original)
        val decoded = PluginMessageCodec.decode(encoded)

        assertIs<PluginMessage.DirectMessageError>(decoded)
        assertEquals(original.messageId, decoded.messageId)
        assertEquals(original.senderId, decoded.senderId)
        assertEquals(original.targetName, decoded.targetName)
        assertEquals(original.targetServerName, decoded.targetServerName)
        assertEquals(PluginMessage.DirectMessageError.Reason.TARGET_OFFLINE, decoded.reason)
    }

    @Test
    fun `encode and decode PresenceSnapshot round-trip`() {
        val original =
            PluginMessage.PresenceSnapshot(
                players =
                    listOf(
                        PresenceEntry("Alice", "lobby"),
                        PresenceEntry("Bob", "survival"),
                    ),
                timestamp = 5000L,
            )

        val encoded = PluginMessageCodec.encode(original)
        val decoded = PluginMessageCodec.decode(encoded)

        assertIs<PluginMessage.PresenceSnapshot>(decoded)
        assertEquals(2, decoded.players.size)
        assertEquals("Alice", decoded.players[0].playerName)
        assertEquals("lobby", decoded.players[0].serverName)
        assertEquals("Bob", decoded.players[1].playerName)
        assertEquals("survival", decoded.players[1].serverName)
        assertEquals(original.timestamp, decoded.timestamp)
    }

    @Test
    fun `encode and decode PresenceSnapshot with empty list round-trip`() {
        val original = PluginMessage.PresenceSnapshot(players = emptyList(), timestamp = 6000L)

        val encoded = PluginMessageCodec.encode(original)
        val decoded = PluginMessageCodec.decode(encoded)

        assertIs<PluginMessage.PresenceSnapshot>(decoded)
        assertEquals(0, decoded.players.size)
    }

    @Test
    fun `encode and decode PresenceRequest round-trip`() {
        val original = PluginMessage.PresenceRequest

        val encoded = PluginMessageCodec.encode(original)
        val decoded = PluginMessageCodec.decode(encoded)

        assertIs<PluginMessage.PresenceRequest>(decoded)
    }

    @Test
    fun `decode should throw on unknown sub-channel`() {
        val out = java.io.ByteArrayOutputStream()
        val dataOut = java.io.DataOutputStream(out)
        dataOut.writeUTF("unknown_channel")
        dataOut.writeUTF("{}")

        assertFailsWith<IllegalArgumentException> {
            PluginMessageCodec.decode(out.toByteArray())
        }
    }

    @Test
    fun `decode should throw on empty data`() {
        assertFailsWith<Exception> {
            PluginMessageCodec.decode(byteArrayOf())
        }
    }

    @Test
    fun `encode and decode GlobalChatMessage with special characters`() {
        val original =
            PluginMessage.GlobalChatMessage(
                messageId = "msg-special",
                serverName = "survival",
                playerId = "00000002-0000-0000-0000-000000000000",
                playerName = "Player_With-Dash",
                message = "Hello! こんにちは 🎉 \"quotes\" & <tags>",
                timestamp = 2000L,
            )

        val encoded = PluginMessageCodec.encode(original)
        val decoded = PluginMessageCodec.decode(encoded)

        assertIs<PluginMessage.GlobalChatMessage>(decoded)
        assertEquals(original.message, decoded.message)
    }

    @Test
    fun `SubChannel constants should have correct values`() {
        assertEquals("handshake", PluginMessageCodec.SubChannel.HANDSHAKE)
        assertEquals("handshake_response", PluginMessageCodec.SubChannel.HANDSHAKE_RESPONSE)
        assertEquals("status_request", PluginMessageCodec.SubChannel.STATUS_REQUEST)
        assertEquals("status_response", PluginMessageCodec.SubChannel.STATUS_RESPONSE)
        assertEquals("global_chat", PluginMessageCodec.SubChannel.GLOBAL_CHAT)
        assertEquals("direct_message", PluginMessageCodec.SubChannel.DIRECT_MESSAGE)
        assertEquals("direct_message_error", PluginMessageCodec.SubChannel.DIRECT_MESSAGE_ERROR)
        assertEquals("presence_snapshot", PluginMessageCodec.SubChannel.PRESENCE_SNAPSHOT)
        assertEquals("presence_request", PluginMessageCodec.SubChannel.PRESENCE_REQUEST)
    }

    @Test
    fun `encode should produce non-empty byte array for all message types`() {
        val messages =
            listOf(
                PluginMessage.Handshake("1.0.0", 1, 0, 0),
                PluginMessage.HandshakeResponse(true, "1.0.0"),
                PluginMessage.StatusRequest,
                PluginMessage.StatusResponse("1.0.0", "1.0.0", true),
                PluginMessage.GlobalChatMessage("id", "srv", "pid", "name", "msg", 0L),
                PluginMessage.DirectMessageRelay("id", "src", "sid", "sname", "tsrv", "tname", "msg", 0L),
                PluginMessage.DirectMessageError("id", "sid", "tname", "tsrv", "TARGET_OFFLINE"),
                PluginMessage.PresenceSnapshot(listOf(PresenceEntry("p", "s")), 0L),
                PluginMessage.PresenceRequest,
            )

        messages.forEach { message ->
            val encoded = PluginMessageCodec.encode(message)
            assert(encoded.isNotEmpty()) { "Encoded ${message::class.simpleName} should not be empty" }
        }
    }

    @Test
    fun `StatusResponse with online false round-trip`() {
        val original =
            PluginMessage.StatusResponse(
                velocityVersion = "0.10.0",
                protocolVersion = "1.0.0",
                online = false,
            )

        val encoded = PluginMessageCodec.encode(original)
        val decoded = PluginMessageCodec.decode(encoded)

        assertIs<PluginMessage.StatusResponse>(decoded)
        assertEquals(false, decoded.online)
    }

    @Test
    fun `Handshake with various protocol versions round-trip`() {
        val original =
            PluginMessage.Handshake(
                pluginVersion = "2.5.3",
                protocolMajor = 99,
                protocolMinor = 42,
                protocolPatch = 7,
            )

        val encoded = PluginMessageCodec.encode(original)
        val decoded = PluginMessageCodec.decode(encoded)

        assertIs<PluginMessage.Handshake>(decoded)
        assertEquals(99, decoded.protocolMajor)
        assertEquals(42, decoded.protocolMinor)
        assertEquals(7, decoded.protocolPatch)
    }

    @Test
    fun `GlobalChatMessage with empty message round-trip`() {
        val original =
            PluginMessage.GlobalChatMessage(
                messageId = "msg-empty",
                serverName = "lobby",
                playerId = "00000003-0000-0000-0000-000000000000",
                playerName = "Player",
                message = "",
                timestamp = 3000L,
            )

        val encoded = PluginMessageCodec.encode(original)
        val decoded = PluginMessageCodec.decode(encoded)

        assertIs<PluginMessage.GlobalChatMessage>(decoded)
        assertEquals("", decoded.message)
    }
}
