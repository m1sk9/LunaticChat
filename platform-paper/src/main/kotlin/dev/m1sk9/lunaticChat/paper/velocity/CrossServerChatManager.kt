package dev.m1sk9.lunaticChat.paper.velocity

import dev.m1sk9.lunaticChat.engine.protocol.PluginMessage
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
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
    companion object {
        private const val CLEANUP_THRESHOLD_MILLIS = 60_000L
    }

    /**
     * Cache of recently processed message IDs (messageId -> timestamp)
     * Used for deduplication
     */
    private val processedMessages = ConcurrentHashMap<String, Long>()

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
            processedMessages[messageId] = System.currentTimeMillis()

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
                                "lunaticchat:main",
                                dev.m1sk9.lunaticChat.engine.protocol.PluginMessageCodec
                                    .encode(globalChatMessage),
                            )
                            logger.info("Sent global chat message to Velocity: messageId=$messageId, player=$playerName")
                        } else {
                            logger.warning("Cannot send global chat message: player $playerId not found")
                        }
                    } catch (e: Exception) {
                        logger.log(Level.SEVERE, "Failed to send plugin message on main thread", e)
                    }
                },
            )

            // Cleanup old messages if cache is too large
            if (processedMessages.size > cacheSize) {
                cleanupOldMessages()
            }
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
            if (!shouldProcessMessage(message.messageId)) {
                logger.fine("Ignoring duplicate message: messageId=${message.messageId}")
                return
            }

            // Mark as processed
            processedMessages[message.messageId] = System.currentTimeMillis()

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

            logger.info(
                "Broadcasted global chat message from ${message.serverName}: " +
                    "player=${message.playerName}, messageId=${message.messageId}",
            )

            // Cleanup if needed
            if (processedMessages.size > cacheSize) {
                cleanupOldMessages()
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
            format
                .replace("{server}", message.serverName)
                .replace("{sender}", message.playerName)
                .replace("{message}", message.message)

        return LegacyComponentSerializer.legacySection().deserialize(formattedText)
    }

    /**
     * Checks if a message should be processed (not a duplicate)
     *
     * @param messageId Message ID to check
     * @return true if message should be processed, false if it's a duplicate
     */
    private fun shouldProcessMessage(messageId: String): Boolean = !processedMessages.containsKey(messageId)

    /**
     * Removes old messages from the cache (LRU cleanup)
     * Keeps only the most recent messages
     */
    private fun cleanupOldMessages() {
        try {
            val currentTime = System.currentTimeMillis()
            val cutoffTime = currentTime - CLEANUP_THRESHOLD_MILLIS

            // Collect keys to remove (ConcurrentHashMap iterator doesn't support remove())
            val keysToRemove = mutableListOf<String>()
            processedMessages.entries.forEach { entry ->
                if (entry.value < cutoffTime) {
                    keysToRemove.add(entry.key)
                }
            }

            // Remove expired entries
            keysToRemove.forEach { key ->
                processedMessages.remove(key)
            }
            var removedCount = keysToRemove.size

            // If still over cache size, remove oldest entries
            if (processedMessages.size > cacheSize) {
                val sortedEntries = processedMessages.entries.sortedBy { it.value }
                val toRemove = processedMessages.size - cacheSize

                sortedEntries.take(toRemove).forEach { entry ->
                    processedMessages.remove(entry.key)
                    removedCount++
                }
            }

            if (removedCount > 0) {
                logger.fine("Cleaned up $removedCount old messages from deduplication cache")
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to cleanup old messages", e)
        }
    }
}
