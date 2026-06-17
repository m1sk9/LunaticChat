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
        val protocolMajor: Int = ProtocolVersion.MAJOR,
        val protocolMinor: Int = ProtocolVersion.MINOR,
        val protocolPatch: Int = ProtocolVersion.PATCH,
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

    /**
     * Cross-server direct message relay (Paper → Velocity → target Paper)
     *
     * Routed to a single target server instead of broadcast. Romaji conversion is
     * applied on the sending side, so [message] carries the already-converted text.
     *
     * @property messageId UUID for deduplication
     * @property sourceServerName Sender's server identifier (used for reply routing)
     * @property senderId Sender UUID as string
     * @property senderName Sender name
     * @property targetServerName Target server identifier (routing key)
     * @property targetName Target player name
     * @property message Message content (romaji-converted if applicable)
     * @property timestamp Message timestamp (milliseconds since epoch)
     */
    @Serializable
    data class DirectMessageRelay(
        val messageId: String,
        val sourceServerName: String,
        val senderId: String,
        val senderName: String,
        val targetServerName: String,
        val targetName: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
    ) : PluginMessage

    /**
     * Cross-server direct message delivery failure (Velocity → source Paper)
     *
     * @property messageId UUID of the failed relay
     * @property senderId Sender UUID as string (used to locate the sender to notify)
     * @property targetName Target player name that was requested
     * @property targetServerName Target server name that was requested
     * @property reason Failure reason: [Reason.TARGET_OFFLINE] or [Reason.SERVER_NOT_FOUND]
     */
    @Serializable
    data class DirectMessageError(
        val messageId: String,
        val senderId: String,
        val targetName: String,
        val targetServerName: String,
        val reason: String,
    ) : PluginMessage {
        object Reason {
            const val TARGET_OFFLINE = "TARGET_OFFLINE"
            const val SERVER_NOT_FOUND = "SERVER_NOT_FOUND"
        }
    }

    /**
     * Proxy-wide player presence snapshot (Velocity → Paper)
     *
     * Sent on join/quit/server-switch and in response to [PresenceRequest].
     * Replaces the receiver's cached roster entirely.
     *
     * @property players All players currently connected to the proxy
     * @property timestamp Snapshot timestamp (milliseconds since epoch)
     */
    @Serializable
    data class PresenceSnapshot(
        val players: List<PresenceEntry>,
        val timestamp: Long = System.currentTimeMillis(),
    ) : PluginMessage

    /**
     * Presence snapshot request (Paper → Velocity)
     *
     * Sent after a successful handshake to obtain the initial roster.
     */
    @Serializable
    data object PresenceRequest : PluginMessage
}

/**
 * A single player presence entry used in [PluginMessage.PresenceSnapshot].
 *
 * @property playerName Player name
 * @property serverName Name of the server the player is currently connected to
 */
@Serializable
data class PresenceEntry(
    val playerName: String,
    val serverName: String,
)
