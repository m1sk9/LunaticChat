package dev.m1sk9.lunaticChat.paper.velocity

import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.engine.debug.DebugLogger
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessage
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageChannel
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageCodec
import dev.m1sk9.lunaticChat.paper.chat.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
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
    private val debug: DebugLogger = DebugLogger.Disabled,
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
     *
     * Failures are deliberately not caught here. The delivery queue is the error boundary for
     * queued work and already reports them without stopping the sender's later messages; a second
     * boundary underneath it only obscured where failures on this path are handled - and made an
     * ordinary cancellation at shutdown look like a delivery failure.
     */
    suspend fun sendCrossServerMessage(
        sender: Player,
        targetName: String,
        targetServerName: String,
        message: String,
    ) {
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
        debug.log(DebugCategory.CHAT) {
            "Sent direct message to Velocity: messageId=$messageId, " +
                "target=$targetName@$targetServerName"
        }
    }

    /**
     * Handles a relayed direct message arriving from Velocity for a local recipient.
     */
    fun handleIncomingMessage(message: PluginMessage.DirectMessageRelay) {
        try {
            if (!processedMessages.isNew(message.messageId)) {
                debug.log(DebugCategory.CHAT) { "Deduplication hit, ignoring direct messageId=${message.messageId}" }
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
                            PluginMessage.DirectMessageError.Reason.TARGET_OFFLINE -> "directMessage.remoteTargetOffline"
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
