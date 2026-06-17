package dev.m1sk9.lunaticChat.velocity.messaging

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import com.velocitypowered.api.proxy.server.RegisteredServer
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessage
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageCodec
import org.slf4j.Logger

/**
 * Routes cross-server direct messages to a single target server.
 *
 * Unlike [CrossServerChatRelay] which broadcasts, this resolves the requested
 * target server and player, then forwards the message only to that server.
 * On failure it sends a [PluginMessage.DirectMessageError] back to the source.
 */
class CrossServerDirectMessageRelay(
    private val server: ProxyServer,
    private val logger: Logger,
) {
    companion object {
        private val CHANNEL = MinecraftChannelIdentifier.create("lunaticchat", "main")
    }

    /**
     * Relays a direct message to the target server, or returns an error to the source.
     *
     * @param message Direct message relay to route
     * @param sourceServer The server that sent the message
     */
    fun relay(
        message: PluginMessage.DirectMessageRelay,
        sourceServer: RegisteredServer,
    ) {
        try {
            val targetServer =
                server.allServers.firstOrNull { it.serverInfo.name == message.targetServerName }
            if (targetServer == null) {
                logger.info(
                    "Direct message target server not found: ${message.targetServerName} " +
                        "(messageId=${message.messageId})",
                )
                sendError(sourceServer, message, PluginMessage.DirectMessageError.Reason.SERVER_NOT_FOUND)
                return
            }

            val targetPlayer = server.getPlayer(message.targetName).orElse(null)
            val onTargetServer =
                targetPlayer
                    ?.currentServer
                    ?.orElse(null)
                    ?.serverInfo
                    ?.name == message.targetServerName
            if (targetPlayer == null || !onTargetServer) {
                logger.info(
                    "Direct message target offline or on different server: ${message.targetName}@${message.targetServerName} " +
                        "(messageId=${message.messageId})",
                )
                sendError(sourceServer, message, PluginMessage.DirectMessageError.Reason.TARGET_OFFLINE)
                return
            }

            targetServer.sendPluginMessage(CHANNEL, PluginMessageCodec.encode(message))
            logger.info(
                "Relayed direct message from ${message.senderName}@${message.sourceServerName} " +
                    "to ${message.targetName}@${message.targetServerName} (messageId=${message.messageId})",
            )
        } catch (e: Exception) {
            logger.error("Failed to relay direct message: ${e.message}", e)
        }
    }

    private fun sendError(
        sourceServer: RegisteredServer,
        message: PluginMessage.DirectMessageRelay,
        reason: String,
    ) {
        val error =
            PluginMessage.DirectMessageError(
                messageId = message.messageId,
                senderId = message.senderId,
                targetName = message.targetName,
                targetServerName = message.targetServerName,
                reason = reason,
            )
        sourceServer.sendPluginMessage(CHANNEL, PluginMessageCodec.encode(error))
    }
}
