package dev.m1sk9.lunaticChat.paper.velocity

import dev.m1sk9.lunaticChat.engine.protocol.PluginMessage
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageChannel
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageCodec
import dev.m1sk9.lunaticChat.paper.chat.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import kotlinx.coroutines.CancellationException
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Manages cross-server direct messages.
 *
 * Handles:
 * - Sending direct messages to a player on another server via Velocity
 * - Processing incoming relayed direct messages from Velocity
 * - Surfacing delivery errors returned by Velocity
 * - Message deduplication using an LRU-style cache
 */
class CrossServerDirectMessageManager(
    private val plugin: Plugin,
    private val logger: Logger,
    private val configuration: LunaticChatConfiguration,
    private val directMessageHandler: DirectMessageHandler,
    private val languageManager: LanguageManager,
    private val cacheSize: Int = 100,
) {
    private val processedMessages = MessageDeduplicationCache(cacheSize, logger, "direct message")

    /**
     * Sends a direct message to a player on another server through Velocity.
     *
     * Runs on the sender's delivery queue, off the tick thread, because the romaji conversion it
     * goes through may wait on the Google IME API. The reply target is recorded by the command
     * before the work is queued; the sender-side display and the spy notification are handled by
     * [DirectMessageHandler], and the (possibly romaji-converted) body is what gets relayed.
     */
    suspend fun sendCrossServerMessage(
        sender: Player,
        targetName: String,
        targetServerName: String,
        message: String,
    ) {
        try {
            val messageId = UUID.randomUUID().toString()
            processedMessages.markProcessed(messageId)

            val relayedMessage =
                directMessageHandler.handleOutgoingCrossServerMessage(
                    sender = sender,
                    targetName = targetName,
                    targetServerName = targetServerName,
                    message = message,
                )

            val relay =
                PluginMessage.DirectMessageRelay(
                    messageId = messageId,
                    sourceServerName = configuration.features.velocityIntegration.serverName,
                    senderId = sender.uniqueId.toString(),
                    senderName = sender.name,
                    targetServerName = targetServerName,
                    targetName = targetName,
                    message = relayedMessage,
                )

            sender.sendPluginMessage(plugin, PluginMessageChannel.ID, PluginMessageCodec.encode(relay))
            logger.fine {
                "Sent direct message to Velocity: messageId=$messageId, " +
                    "target=$targetName@$targetServerName"
            }
        } catch (e: CancellationException) {
            // Shutdown cancelling the delivery queue is not a delivery failure, and reporting it as
            // SEVERE while carrying on past the cancellation would be wrong twice over.
            throw e
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to send cross-server direct message", e)
        }
    }

    /**
     * Handles a relayed direct message arriving from Velocity for a local recipient.
     */
    fun handleIncomingMessage(message: PluginMessage.DirectMessageRelay) {
        try {
            if (!processedMessages.isNew(message.messageId)) {
                logger.fine("Ignoring duplicate direct message: messageId=${message.messageId}")
                return
            }
            processedMessages.markProcessed(message.messageId)

            plugin.server.scheduler.runTask(
                plugin,
                Runnable {
                    val recipient = plugin.server.getPlayer(message.targetName)
                    if (recipient == null) {
                        logger.warning(
                            "Received direct message for offline player: ${message.targetName} " +
                                "(messageId=${message.messageId})",
                        )
                        return@Runnable
                    }
                    directMessageHandler.handleIncomingCrossServerMessage(
                        recipient = recipient,
                        senderName = message.senderName,
                        sourceServerName = message.sourceServerName,
                        message = message.message,
                    )
                },
            )
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to handle incoming direct message", e)
        }
    }

    /**
     * Handles a delivery error returned by Velocity and notifies the sender.
     */
    fun handleError(error: PluginMessage.DirectMessageError) {
        try {
            val senderId = runCatching { UUID.fromString(error.senderId) }.getOrNull() ?: return
            plugin.server.scheduler.runTask(
                plugin,
                Runnable {
                    val sender = plugin.server.getPlayer(senderId) ?: return@Runnable
                    val messageKey =
                        when (error.reason) {
                            PluginMessage.DirectMessageError.Reason.SERVER_NOT_FOUND -> "directMessage.remoteServerNotFound"
                            else -> "directMessage.remoteTargetOffline"
                        }
                    val text =
                        languageManager.getMessage(
                            messageKey,
                            mapOf("target" to error.targetName, "server" to error.targetServerName),
                        )
                    sender.sendMessage(MessageFormatter.formatError(text))
                },
            )
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to handle direct message error", e)
        }
    }
}
