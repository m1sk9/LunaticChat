package dev.m1sk9.lunaticChat.engine.protocol

import kotlinx.serialization.Serializable

/**
 * Plugin message definitions for Paper-Velocity communication
 */
sealed interface PluginMessage {
    /**
     * Handshake message (Paper → Velocity)
     *
     * @property pluginVersion Plugin version (e.g., "0.7.0")
     * @property protocolMajor Protocol major version
     * @property protocolMinor Protocol minor version
     * @property protocolPatch Protocol patch version
     */
    @Serializable
    data class Handshake(
        val pluginVersion: String,
        val protocolMajor: Int,
        val protocolMinor: Int,
        val protocolPatch: Int,
    ) : PluginMessage

    /**
     * Handshake response message (Velocity → Paper)
     *
     * @property compatible true if compatible
     * @property velocityVersion Velocity plugin version
     * @property error Error message (when incompatible)
     */
    @Serializable
    data class HandshakeResponse(
        val compatible: Boolean,
        val velocityVersion: String,
        val error: String? = null,
    ) : PluginMessage

    /**
     * Status request message (Paper → Velocity)
     */
    @Serializable
    data object StatusRequest : PluginMessage

    /**
     * Status response message (Velocity → Paper)
     *
     * @property velocityVersion Velocity plugin version
     * @property protocolVersion Protocol version
     * @property online true if connection is active
     */
    @Serializable
    data class StatusResponse(
        val velocityVersion: String,
        val protocolVersion: String,
        val online: Boolean,
    ) : PluginMessage

    /**
     * Global chat message (Paper ↔ Velocity ↔ Paper)
     *
     * @property messageId UUID for deduplication
     * @property serverName Source server identifier
     * @property playerId UUID as string
     * @property playerName Player name
     * @property message Chat message content
     * @property timestamp Message timestamp (milliseconds since epoch)
     */
    @Serializable
    data class GlobalChatMessage(
        val messageId: String,
        val serverName: String,
        val playerId: String,
        val playerName: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
    ) : PluginMessage
}
