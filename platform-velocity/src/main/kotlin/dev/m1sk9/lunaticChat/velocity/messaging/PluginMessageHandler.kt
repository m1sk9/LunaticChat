package dev.m1sk9.lunaticChat.velocity.messaging

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.ServerConnection
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessage
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageCodec
import dev.m1sk9.lunaticChat.engine.protocol.ProtocolVersion
import org.slf4j.Logger

/**
 * Handles plugin messages from Paper servers
 */
class PluginMessageHandler(
    private val plugin: Any,
    private val server: ProxyServer,
    private val logger: Logger,
    private val pluginVersion: String,
) {
    companion object {
        private val CHANNEL = MinecraftChannelIdentifier.create("lunaticchat", "main")
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
            val message = PluginMessageCodec.decode(event.data)

            when (message) {
                is PluginMessage.Handshake -> {
                    handleHandshake(source, message)
                }
                is PluginMessage.StatusRequest -> {
                    handleStatusRequest(source)
                }
                else -> {
                    logger.warn("Unexpected message type from Paper: ${message::class.simpleName}")
                }
            }
        } catch (e: Exception) {
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
        logger.info(
            "Received handshake from ${connection.serverInfo.name}: " +
                "Plugin=${handshake.pluginVersion}, Protocol=${handshake.protocolMajor}.${handshake.protocolMinor}.${handshake.protocolPatch}",
        )

        // Plugin version check (exact match required)
        val versionMatch = handshake.pluginVersion == pluginVersion
        if (!versionMatch) {
            val error = "Plugin version mismatch: Paper=${handshake.pluginVersion}, Velocity=$pluginVersion"
            logger.error(error)
            sendHandshakeResponse(connection, false, error)
            return
        }

        // Protocol version check (MAJOR.MINOR must match)
        val protocolCompatible =
            ProtocolVersion.isCompatible(
                handshake.protocolMajor,
                handshake.protocolMinor,
            )
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
            )

        val data = PluginMessageCodec.encode(response)

        // Send
        connection.sendPluginMessage(CHANNEL, data)

        if (compatible) {
            logger.info("Sent successful handshake response to ${connection.serverInfo.name}")
        } else {
            logger.warn("Sent failed handshake response to ${connection.serverInfo.name}: $error")
        }
    }

    /**
     * Handles status request
     */
    private fun handleStatusRequest(connection: ServerConnection) {
        logger.info("Received status request from ${connection.serverInfo.name}")

        val response =
            PluginMessage.StatusResponse(
                velocityVersion = pluginVersion,
                protocolVersion = ProtocolVersion.version,
                online = true,
            )

        val data = PluginMessageCodec.encode(response)
        connection.sendPluginMessage(CHANNEL, data)

        logger.info("Sent status response to ${connection.serverInfo.name}")
    }

    /**
     * Shutdown
     */
    fun shutdown() {
        server.eventManager.unregisterListener(plugin, this)
        logger.info("Plugin message handler unregistered")
    }
}
