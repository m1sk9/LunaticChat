package dev.m1sk9.lunaticChat.velocity.messaging

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import com.velocitypowered.api.proxy.server.RegisteredServer
import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.engine.debug.DebugLogger
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessage
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageChannel
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageCodec
import org.slf4j.Logger

/**
 * Relays global chat messages between servers
 *
 * When a Paper server sends a global chat message to Velocity,
 * this class broadcasts it to all other connected servers.
 */
class CrossServerChatRelay(
    private val server: ProxyServer,
    private val logger: Logger,
    private val debug: DebugLogger = DebugLogger.Disabled,
) {
    companion object {
        private val CHANNEL = MinecraftChannelIdentifier.create(PluginMessageChannel.NAMESPACE, PluginMessageChannel.NAME)
    }

    /**
     * Relays a global chat message to all servers except the source
     *
     * @param message Global chat message to relay
     * @param sourceServer The server that sent the message
     */
    fun relayGlobalMessage(
        message: PluginMessage.GlobalChatMessage,
        sourceServer: RegisteredServer,
    ) {
        try {
            val encodedMessage = PluginMessageCodec.encode(message)
            var relayCount = 0

            server.allServers.forEach { targetServer ->
                if (targetServer == sourceServer) {
                    // Named rather than silently skipped: "the message never arrived" and "the
                    // message was never sent" look identical from the receiving server's log.
                    debug.log(DebugCategory.VELOCITY) {
                        "Skipped ${targetServer.serverInfo.name} for messageId=${message.messageId}: it is the source"
                    }
                    return@forEach
                }
                targetServer.sendPluginMessage(CHANNEL, encodedMessage)
                relayCount++
            }

            debug.log(DebugCategory.VELOCITY) {
                "Relayed global chat message from ${message.serverName} to $relayCount servers (messageId=${message.messageId})"
            }
        } catch (e: Exception) {
            logger.error("Failed to relay global chat message: ${e.message}", e)
        }
    }
}
