package dev.m1sk9.lunaticChat.paper.command.handler

import dev.m1sk9.lunaticChat.paper.config.ConfigManager
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages direct message state including reply targets.
 * Tracks the last player who messaged each player for /reply functionality.
 */
class DirectMessageHandler {
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
     */
    fun clearPlayer(player: Player) {
        lastMessager.remove(player.uniqueId)
        lastRecipient.remove(player.uniqueId)
    }

    /**
     * Sends a direct message from one player to another.
     * Handles formatting and recording the conversation.
     *
     * @return true if message was sent successfully
     */
    fun sendDirectMessage(
        sender: Player,
        recipient: Player,
        message: String,
    ): Boolean {
        recordMessage(sender, recipient)
        val format = lunaticChatConfiguration.messageFormat.directMessageFormat
        val formattedMessage = formatMessage(format, sender.name, recipient.name, message)

        sender.sendMessage(formattedMessage)
        recipient.sendMessage(formattedMessage)
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

        return Component.text(text)
    }
}
