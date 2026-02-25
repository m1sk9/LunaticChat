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
