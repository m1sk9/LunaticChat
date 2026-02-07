package dev.m1sk9.lunaticChat.engine.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Plugin message encoding/decoding
 *
 * Format: [subChannel: UTF][messageJson: UTF]
 */
object PluginMessageCodec {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Sub-channel names
     */
    object SubChannel {
        const val HANDSHAKE = "handshake"
        const val HANDSHAKE_RESPONSE = "handshake_response"
        const val STATUS_REQUEST = "status_request"
        const val STATUS_RESPONSE = "status_response"
        const val GLOBAL_CHAT = "global_chat"
    }

    /**
     * Encodes message
     *
     * @param message Message to encode
     * @return Encoded byte array
     */
    fun encode(message: PluginMessage): ByteArray {
        val out = ByteArrayOutputStream()
        val dataOut = DataOutputStream(out)

        val (subChannel, messageJson) =
            when (message) {
                is PluginMessage.Handshake -> {
                    SubChannel.HANDSHAKE to json.encodeToString(message)
                }
                is PluginMessage.HandshakeResponse -> {
                    SubChannel.HANDSHAKE_RESPONSE to json.encodeToString(message)
                }
                is PluginMessage.StatusRequest -> {
                    SubChannel.STATUS_REQUEST to "{}"
                }
                is PluginMessage.StatusResponse -> {
                    SubChannel.STATUS_RESPONSE to json.encodeToString(message)
                }
                is PluginMessage.GlobalChatMessage -> {
                    SubChannel.GLOBAL_CHAT to json.encodeToString(message)
                }
            }

        dataOut.writeUTF(subChannel)
        dataOut.writeUTF(messageJson)

        return out.toByteArray()
    }

    /**
     * Decodes message
     *
     * @param data Byte array to decode
     * @return Decoded message
     * @throws IllegalArgumentException Invalid message format
     */
    fun decode(data: ByteArray): PluginMessage {
        val input = ByteArrayInputStream(data)
        val dataIn = DataInputStream(input)

        val subChannel = dataIn.readUTF()
        val messageJson = dataIn.readUTF()

        return when (subChannel) {
            SubChannel.HANDSHAKE -> {
                json.decodeFromString<PluginMessage.Handshake>(messageJson)
            }
            SubChannel.HANDSHAKE_RESPONSE -> {
                json.decodeFromString<PluginMessage.HandshakeResponse>(messageJson)
            }
            SubChannel.STATUS_REQUEST -> {
                PluginMessage.StatusRequest
            }
            SubChannel.STATUS_RESPONSE -> {
                json.decodeFromString<PluginMessage.StatusResponse>(messageJson)
            }
            SubChannel.GLOBAL_CHAT -> {
                json.decodeFromString<PluginMessage.GlobalChatMessage>(messageJson)
            }
            else -> throw IllegalArgumentException("Unknown sub-channel: $subChannel")
        }
    }
}
