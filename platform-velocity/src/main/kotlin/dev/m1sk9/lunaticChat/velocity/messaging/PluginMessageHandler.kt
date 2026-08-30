package dev.m1sk9.lunaticChat.velocity.messaging

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.ServerConnection
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.engine.debug.DebugLogger
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessage
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageChannel
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageCodec
import dev.m1sk9.lunaticChat.engine.protocol.ProtocolVersion
import dev.m1sk9.lunaticChat.velocity.presence.PresenceTracker
import org.slf4j.Logger

/**
 * Handles plugin messages from Paper servers
 */
class PluginMessageHandler(
    /**
     * Plugin instance used for event registration.
     * Type is [Any] because Velocity's EventManager.register() accepts Object.
     * Could be made generic, but provides little practical benefit since the API itself is not type-safe.
     * Typically the main plugin instance is passed here.
     */
    private val plugin: Any,
    private val server: ProxyServer,
    private val logger: Logger,
    private val debug: DebugLogger = DebugLogger.Disabled,
    private val pluginVersion: String,
    private val crossServerChatRelay: CrossServerChatRelay,
    private val crossServerDirectMessageRelay: CrossServerDirectMessageRelay,
    private val presenceTracker: PresenceTracker,
) {
    companion object {
        private val CHANNEL = MinecraftChannelIdentifier.create(PluginMessageChannel.NAMESPACE, PluginMessageChannel.NAME)
    }

    /**
     * Initialize
     */
    fun initialize() {
        server.channelRegistrar.register(CHANNEL)
        server.eventManager.register(plugin, this)
        logger.info("Plugin message handler registered for channel: $CHANNEL")
    }

    /**
     * Plugin message event
     */
    @Subscribe
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.identifier != CHANNEL) return

        val source = event.source
        if (source !is ServerConnection) {
            logger.warn("Received plugin message from non-server source: ${source::class.simpleName}")
            return
        }

        try {
            debug.log(DebugCategory.PROTOCOL) {
                "Received ${event.data.size} bytes from ${source.serverInfo.name}"
            }
            when (val message = PluginMessageCodec.decode(event.data)) {
                is PluginMessage.Handshake -> {
                    handleHandshake(source, message)
                }
                is PluginMessage.StatusRequest -> {
                    handleStatusRequest(source)
                }
                is PluginMessage.GlobalChatMessage -> {
                    handleGlobalChatMessage(source, message)
                }
                is PluginMessage.DirectMessageRelay -> {
                    crossServerDirectMessageRelay.relay(message, source.server)
                }
                is PluginMessage.PresenceRequest -> {
                    presenceTracker.sendSnapshotTo(source.server)
                }
                else -> {
                    logger.warn("Unexpected message type from Paper: ${message::class.simpleName}")
                }
            }
        } catch (e: Exception) {
            debug.log(DebugCategory.PROTOCOL) {
                "Undecodable message from ${source.serverInfo.name}, first bytes: ${event.data.take(16).toHex()}"
            }
            logger.error("Failed to decode plugin message: ${e.message}", e)
        }
    }

    /**
     * Handles handshake message
     */
    private fun handleHandshake(
        connection: ServerConnection,
        handshake: PluginMessage.Handshake,
    ) {
        debug.log(DebugCategory.VELOCITY) {
            "Received handshake from ${connection.serverInfo.name}: " +
                "Plugin=${handshake.pluginVersion}, Protocol=${handshake.protocolMajor}.${handshake.protocolMinor}.${handshake.protocolPatch}"
        }

        // Protocol version check (MAJOR must match, MINOR within supported range)
        val protocolCompatible =
            ProtocolVersion.isCompatible(
                handshake.protocolMajor,
                handshake.protocolMinor,
            )
        // Spelled out rather than left to the caller to infer from the two version strings: which
        // half of the rule failed is the whole answer when a rolling upgrade stops relaying.
        debug.log(DebugCategory.VELOCITY) {
            "Compatibility check for ${connection.serverInfo.name}: " +
                "major ${handshake.protocolMajor} == ${ProtocolVersion.MAJOR} is " +
                "${handshake.protocolMajor == ProtocolVersion.MAJOR}, " +
                "minor ${handshake.protocolMinor} in " +
                "${ProtocolVersion.MIN_SUPPORTED_MINOR}..${ProtocolVersion.MINOR} is " +
                "${handshake.protocolMinor in ProtocolVersion.MIN_SUPPORTED_MINOR..ProtocolVersion.MINOR}"
        }
        if (!protocolCompatible) {
            val error =
                "Protocol version incompatible: Paper=${handshake.protocolMajor}.${handshake.protocolMinor}.${handshake.protocolPatch}, " +
                    "Velocity=${ProtocolVersion.version}"
            logger.error(error)
            sendHandshakeResponse(connection, false, error)
            return
        }

        // Success
        logger.info("Handshake successful with ${connection.serverInfo.name}")
        sendHandshakeResponse(connection, true, null)
    }

    /**
     * Sends handshake response
     */
    private fun sendHandshakeResponse(
        connection: ServerConnection,
        compatible: Boolean,
        error: String?,
    ) {
        val response =
            PluginMessage.HandshakeResponse(
                compatible = compatible,
                velocityVersion = pluginVersion,
                error = error,
                protocolMajor = ProtocolVersion.MAJOR,
                protocolMinor = ProtocolVersion.MINOR,
                protocolPatch = ProtocolVersion.PATCH,
            )

        val data = PluginMessageCodec.encode(response)

        // Send
        connection.sendPluginMessage(CHANNEL, data)
        debug.log(DebugCategory.PROTOCOL) { "Sent handshake response (${data.size} bytes) to ${connection.serverInfo.name}" }

        // Only the failure stays at INFO or above. Whether a backend connected is one line an
        // operator needs; the outcome is already reported by handleHandshake, and a proxy with a
        // dozen backends should not print the same fact twice per server.
        if (!compatible) {
            logger.warn("Sent failed handshake response to ${connection.serverInfo.name}: $error")
        }
    }

    /**
     * Handles status request
     */
    private fun handleStatusRequest(connection: ServerConnection) {
        debug.log(DebugCategory.VELOCITY) { "Received status request from ${connection.serverInfo.name}" }

        val response =
            PluginMessage.StatusResponse(
                velocityVersion = pluginVersion,
                protocolVersion = ProtocolVersion.version,
                online = true,
            )

        val data = PluginMessageCodec.encode(response)
        connection.sendPluginMessage(CHANNEL, data)

        debug.log(DebugCategory.VELOCITY) { "Sent status response to ${connection.serverInfo.name}" }
    }

    /**
     * Handles global chat message and relays to other servers
     */
    private fun handleGlobalChatMessage(
        connection: ServerConnection,
        message: PluginMessage.GlobalChatMessage,
    ) {
        debug.log(DebugCategory.VELOCITY) {
            "Received global chat message from ${connection.serverInfo.name}: messageId=${message.messageId}"
        }

        crossServerChatRelay.relayGlobalMessage(message, connection.server)
    }

    /**
     * Shutdown
     */
    fun shutdown() {
        server.eventManager.unregisterListener(plugin, this)
        logger.info("Plugin message handler unregistered")
    }
}

/** The leading bytes of an undecodable message, so a wire mismatch can be recognised on sight. */
private fun List<Byte>.toHex(): String = joinToString(" ") { "%02x".format(it) }
