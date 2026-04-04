package dev.m1sk9.lunaticChat.engine.protocol

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Backward compatibility tests using fixed JSON snapshots.
 *
 * These snapshots represent the wire format of protocol version 1.0.0.
 * DO NOT MODIFY these constants after commit — they are the compatibility contract.
 * If a test fails after modifying a @Serializable class, it means backward compatibility is broken.
 */
class ProtocolBackwardCompatibilityTest {
    companion object {
        // Protocol 1.0.0 snapshots — NEVER MODIFY after commit
        const val HANDSHAKE_V1_0_0 =
            """{"pluginVersion":"0.10.0","protocolMajor":1,"protocolMinor":0,"protocolPatch":0}"""

        const val HANDSHAKE_RESPONSE_V1_0_0 =
            """{"compatible":true,"velocityVersion":"0.10.0"}"""

        const val HANDSHAKE_RESPONSE_INCOMPATIBLE_V1_0_0 =
            """{"compatible":false,"velocityVersion":"0.10.0","error":"Version mismatch"}"""

        const val STATUS_RESPONSE_V1_0_0 =
            """{"velocityVersion":"0.10.0","protocolVersion":"1.0.0","online":true}"""

        const val GLOBAL_CHAT_V1_0_0 =
            """{"messageId":"abc-123","serverName":"lobby","playerId":"00000001-0000-0000-0000-000000000000","playerName":"TestPlayer","message":"Hello, world!","timestamp":1000}"""
    }

    private fun buildRawMessage(
        subChannel: String,
        json: String,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val dataOut = DataOutputStream(out)
        dataOut.writeUTF(subChannel)
        dataOut.writeUTF(json)
        return out.toByteArray()
    }

    @Test
    fun `current codec can decode protocol 1_0_0 Handshake`() {
        val data = buildRawMessage("handshake", HANDSHAKE_V1_0_0)
        val decoded = PluginMessageCodec.decode(data)

        assertIs<PluginMessage.Handshake>(decoded)
        assertEquals("0.10.0", decoded.pluginVersion)
        assertEquals(1, decoded.protocolMajor)
        assertEquals(0, decoded.protocolMinor)
        assertEquals(0, decoded.protocolPatch)
    }

    @Test
    fun `current codec can decode HandshakeResponse without protocol version fields`() {
        val data = buildRawMessage("handshake_response", HANDSHAKE_RESPONSE_V1_0_0)
        val decoded = PluginMessageCodec.decode(data)

        assertIs<PluginMessage.HandshakeResponse>(decoded)
        assertTrue(decoded.compatible)
        assertEquals("0.10.0", decoded.velocityVersion)
        assertNull(decoded.error)
        // Protocol fields should get defaults when missing from old format
        assertEquals(ProtocolVersion.MAJOR, decoded.protocolMajor)
        assertEquals(ProtocolVersion.MINOR, decoded.protocolMinor)
        assertEquals(ProtocolVersion.PATCH, decoded.protocolPatch)
    }

    @Test
    fun `current codec can decode HandshakeResponse incompatible without protocol version fields`() {
        val data = buildRawMessage("handshake_response", HANDSHAKE_RESPONSE_INCOMPATIBLE_V1_0_0)
        val decoded = PluginMessageCodec.decode(data)

        assertIs<PluginMessage.HandshakeResponse>(decoded)
        assertEquals(false, decoded.compatible)
        assertEquals("Version mismatch", decoded.error)
    }

    @Test
    fun `current codec can decode protocol 1_0_0 StatusResponse`() {
        val data = buildRawMessage("status_response", STATUS_RESPONSE_V1_0_0)
        val decoded = PluginMessageCodec.decode(data)

        assertIs<PluginMessage.StatusResponse>(decoded)
        assertEquals("0.10.0", decoded.velocityVersion)
        assertEquals("1.0.0", decoded.protocolVersion)
        assertTrue(decoded.online)
    }

    @Test
    fun `current codec can decode protocol 1_0_0 GlobalChatMessage`() {
        val data = buildRawMessage("global_chat", GLOBAL_CHAT_V1_0_0)
        val decoded = PluginMessageCodec.decode(data)

        assertIs<PluginMessage.GlobalChatMessage>(decoded)
        assertEquals("abc-123", decoded.messageId)
        assertEquals("lobby", decoded.serverName)
        assertEquals("00000001-0000-0000-0000-000000000000", decoded.playerId)
        assertEquals("TestPlayer", decoded.playerName)
        assertEquals("Hello, world!", decoded.message)
        assertEquals(1000L, decoded.timestamp)
    }

    @Test
    fun `current codec ignores unknown fields in Handshake`() {
        val json =
            """{"pluginVersion":"0.10.0","protocolMajor":1,"protocolMinor":0,"protocolPatch":0,"futureField":"value"}"""
        val data = buildRawMessage("handshake", json)
        val decoded = PluginMessageCodec.decode(data)

        assertIs<PluginMessage.Handshake>(decoded)
        assertEquals("0.10.0", decoded.pluginVersion)
    }

    @Test
    fun `current codec ignores unknown fields in GlobalChatMessage`() {
        val json =
            """{"messageId":"abc","serverName":"lobby","playerId":"pid","playerName":"Test","message":"Hello","timestamp":1000,"futureField":"value","anotherField":42}"""
        val data = buildRawMessage("global_chat", json)
        val decoded = PluginMessageCodec.decode(data)

        assertIs<PluginMessage.GlobalChatMessage>(decoded)
        assertEquals("abc", decoded.messageId)
        assertEquals("Hello", decoded.message)
    }

    @Test
    fun `StatusRequest sub-channel decodes correctly with empty JSON`() {
        val data = buildRawMessage("status_request", "{}")
        val decoded = PluginMessageCodec.decode(data)

        assertIs<PluginMessage.StatusRequest>(decoded)
    }
}
