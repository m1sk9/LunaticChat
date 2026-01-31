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
}
