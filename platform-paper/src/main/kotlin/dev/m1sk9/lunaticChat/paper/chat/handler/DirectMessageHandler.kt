package dev.m1sk9.lunaticChat.paper.chat.handler

import dev.m1sk9.lunaticChat.paper.common.SpyPermissionManager
import dev.m1sk9.lunaticChat.paper.common.playDirectMessageNotification
import dev.m1sk9.lunaticChat.paper.common.playMessageSendNotification
import dev.m1sk9.lunaticChat.paper.config.ConfigManager
import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages direct message state including reply targets.
 * Tracks the last player who messaged each player for /reply functionality.
 */
class DirectMessageHandler(
    private val settingsManager: PlayerSettingsManager?,
    private val romanjiConverter: RomanjiConverter?,
) {
    private var lunaticChatConfiguration = ConfigManager.getConfiguration()

    private val lastMessager: ConcurrentHashMap<UUID, UUID> = ConcurrentHashMap()
    private val lastRecipient: ConcurrentHashMap<UUID, UUID> = ConcurrentHashMap()

    /**
     * Records a direct message between two players.
     * Updates both the sender's last recipient and the receiver's last messager.
     */
    fun recordMessage(
        sender: Player,
        recipient: Player,
    ) {
        lastRecipient[sender.uniqueId] = recipient.uniqueId
        lastMessager[recipient.uniqueId] = sender.uniqueId
    }

    /**
     * Gets the player to reply to.
     * First checks if someone has messaged this player, otherwise falls back
     * to the last person they messaged.
     */
    fun getReplyTarget(player: Player): Player? {
        val messager = lastMessager[player.uniqueId]?.let { Bukkit.getPlayer(it) }
        if (messager != null && messager.isOnline) {
            return messager
        }

        val recipient = lastRecipient[player.uniqueId]?.let { Bukkit.getPlayer(it) }
        if (recipient != null && recipient.isOnline) {
            return recipient
        }

        return null
    }

    /**
     * Clears message history for a player (called on disconnect).
     * Removes entries where this player is either the sender or recipient.
     */
    fun clearPlayer(player: Player) {
        val playerId = player.uniqueId

        // Remove entries where this player is the sender
        lastMessager.remove(playerId)
        lastRecipient.remove(playerId)

        // Remove entries where this player is the recipient
        lastMessager.entries.removeIf { it.value == playerId }
        lastRecipient.entries.removeIf { it.value == playerId }
    }

    /**
     * Sends a direct message from one player to another.
     * Handles formatting and recording the conversation.
     * Applies romaji-to-Japanese conversion if sender has it enabled.
     *
     * @return true if message was sent successfully
     */
    fun sendDirectMessage(
        sender: Player,
        recipient: Player,
        message: String,
    ): Boolean {
        recordMessage(sender, recipient)

        val senderSettings = settingsManager?.getSettings(sender.uniqueId)
        val recipientSettings = settingsManager?.getSettings(recipient.uniqueId)

        // Handle romaji conversion if enabled (requires blocking for HTTP call)
        val displayMessage =
            if (senderSettings?.japaneseConversionEnabled == true && romanjiConverter != null) {
                runCatching {
                    kotlinx.coroutines.runBlocking {
                        romanjiConverter
                            ?.convert(message)
                            ?.let { "$message §e($it)" }
                            ?: message
                    }
                }.getOrNull() ?: message
            } else {
                message
            }

        val format = lunaticChatConfiguration.messageFormat.directMessageFormat

        val spyMessage = formatMessage(format, sender.name, recipient.name, message)
        SpyPermissionManager
            .getDirectMessageSpyPlayers()
            .values
            .filter { it.isOnline && it.uniqueId !in setOf(sender.uniqueId, recipient.uniqueId) }
            .forEach { it.sendMessage(spyMessage) }

        val userMessage = formatMessage(format, sender.name, recipient.name, displayMessage)
        sender.apply {
            sendMessage(userMessage)
            takeIf { senderSettings?.directMessageNotificationEnabled == true }
                ?.playMessageSendNotification()
        }
        recipient.apply {
            sendMessage(userMessage)
            takeIf { recipientSettings?.directMessageNotificationEnabled == true }
                ?.playDirectMessageNotification()
        }
        return true
    }

    private fun formatMessage(
        format: String,
        senderName: String,
        recipientName: String,
        message: String,
    ): Component {
        val text =
            format
                .replace("{sender}", senderName)
                .replace("{recipient}", recipientName)
                .replace("{message}", message)

        return Component
            .text(text)
            .clickEvent(ClickEvent.suggestCommand("/tell $senderName "))
    }
}
