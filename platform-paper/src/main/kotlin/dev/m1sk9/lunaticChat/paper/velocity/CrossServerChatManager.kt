package dev.m1sk9.lunaticChat.paper.velocity

import dev.m1sk9.lunaticChat.engine.protocol.PluginMessage
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageChannel
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageCodec
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.i18n.withChatPlaceholders
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Manages cross-server global chat messages
 *
 * Handles:
 * - Sending global chat messages to Velocity
 * - Processing incoming messages from Velocity
 * - Message deduplication using LRU cache
 */
class CrossServerChatManager(
    private val plugin: Plugin,
    private val logger: Logger,
    private val configuration: LunaticChatConfiguration,
    private val cacheSize: Int = 100,
) {
    private val processedMessages = MessageDeduplicationCache(cacheSize, logger, "global chat")

    /**
     * Sends a global chat message to Velocity for cross-server broadcast
     *
     * @param playerId Player UUID
     * @param playerName Player name
     * @param message Chat message content
     */
    fun sendGlobalMessage(
        playerId: UUID,
        playerName: String,
        message: String,
    ) {
        try {
            val messageId = UUID.randomUUID().toString()
            val serverName = configuration.features.velocityIntegration.serverName

            // Mark as processed immediately to prevent echo
            processedMessages.markProcessed(messageId)

            val globalChatMessage =
                PluginMessage.GlobalChatMessage(
                    messageId = messageId,
                    serverName = serverName,
                    playerId = playerId.toString(),
                    playerName = playerName,
                    message = message,
                )

            // Schedule Bukkit API calls on the main server thread
            plugin.server.scheduler.runTask(
                plugin,
                Runnable {
                    try {
                        // Send to Velocity
                        val player = plugin.server.getPlayer(playerId)
                        if (player != null) {
                            player.sendPluginMessage(
                                plugin,
                                PluginMessageChannel.ID,
                                PluginMessageCodec.encode(globalChatMessage),
                            )
                            logger.fine { "Sent global chat message to Velocity: messageId=$messageId, player=$playerName" }
                        } else {
                            logger.warning("Cannot send global chat message: player $playerId not found")
                        }
                    } catch (e: Exception) {
                        logger.log(Level.SEVERE, "Failed to send plugin message on main thread", e)
                    }
                },
            )
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to send global chat message", e)
        }
    }

    /**
     * Handles incoming global chat message from Velocity
     *
     * @param message Global chat message
     */
    fun handleIncomingMessage(message: PluginMessage.GlobalChatMessage) {
        try {
            // Check if already processed (deduplication)
            if (!processedMessages.isNew(message.messageId)) {
                logger.fine("Ignoring duplicate message: messageId=${message.messageId}")
                return
            }
            processedMessages.markProcessed(message.messageId)

            // Broadcast to all players on this server
            val formattedMessage = formatCrossServerMessage(message)

            plugin.server.scheduler.runTask(
                plugin,
                Runnable {
                    plugin.server.onlinePlayers.forEach { player ->
                        player.sendMessage(formattedMessage)
                    }
                },
            )

            logger.fine {
                "Broadcasted global chat message from ${message.serverName}: " +
                    "player=${message.playerName}, messageId=${message.messageId}"
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to handle incoming global chat message", e)
        }
    }

    /**
     * Formats a cross-server chat message using the configured format
     *
     * @param message Global chat message
     * @return Formatted Component
     */
    private fun formatCrossServerMessage(message: PluginMessage.GlobalChatMessage): Component {
        val format = configuration.messageFormat.crossServerGlobalChatFormat
        val formattedText =
            format.withChatPlaceholders(
                "server" to message.serverName,
                "sender" to message.playerName,
                "message" to message.message,
            )

        return LegacyComponentSerializer.legacySection().deserialize(formattedText)
    }
}
