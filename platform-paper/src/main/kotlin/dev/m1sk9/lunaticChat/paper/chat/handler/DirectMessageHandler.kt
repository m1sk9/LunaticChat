package dev.m1sk9.lunaticChat.paper.chat.handler

import dev.m1sk9.lunaticChat.paper.common.SpyPermissionManager
import dev.m1sk9.lunaticChat.paper.common.playDirectMessageNotification
import dev.m1sk9.lunaticChat.paper.common.playMessageSendNotification
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import dev.m1sk9.lunaticChat.paper.converter.convertWithRomaji
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import dev.m1sk9.lunaticChat.paper.velocity.RemotePlayerRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * A reply destination for the /reply command.
 *
 * Generalized to support both same-server players ([Local]) and players on
 * another server behind the Velocity proxy ([Remote]).
 */
sealed interface ReplyTarget {
    data class Local(
        val uuid: UUID,
    ) : ReplyTarget

    data class Remote(
        val playerName: String,
        val serverName: String,
    ) : ReplyTarget
}

/**
 * Manages direct message state including reply targets.
 * Tracks the last player who messaged each player for /reply functionality.
 */
class DirectMessageHandler(
    private val configuration: LunaticChatConfiguration,
    private val settingsManager: PlayerSettingsManager?,
    private val romanjiConverter: RomanjiConverter?,
    private val languageManager: LanguageManager,
) {
    private val lastMessager: ConcurrentHashMap<UUID, ReplyTarget> = ConcurrentHashMap()
    private val lastRecipient: ConcurrentHashMap<UUID, ReplyTarget> = ConcurrentHashMap()

    /**
     * Registry of proxy-wide player presence. Set when cross-server direct
     * messages are enabled; used to validate [ReplyTarget.Remote] reply targets.
     */
    var remotePlayerRegistry: RemotePlayerRegistry? = null

    /**
     * Records a local direct message between two players.
     * Updates both the sender's last recipient and the receiver's last messager.
     */
    fun recordMessage(
        sender: Player,
        recipient: Player,
    ) {
        lastRecipient[sender.uniqueId] = ReplyTarget.Local(recipient.uniqueId)
        lastMessager[recipient.uniqueId] = ReplyTarget.Local(sender.uniqueId)
    }

    /**
     * Gets the target to reply to.
     * First checks if someone has messaged this player, otherwise falls back
     * to the last person they messaged. Targets that are no longer reachable
     * (offline locally, or absent from the proxy roster) are skipped.
     */
    fun getReplyTarget(player: Player): ReplyTarget? {
        resolveValidTarget(lastMessager[player.uniqueId])?.let { return it }
        resolveValidTarget(lastRecipient[player.uniqueId])?.let { return it }
        return null
    }

    private fun resolveValidTarget(target: ReplyTarget?): ReplyTarget? =
        when (target) {
            is ReplyTarget.Local -> target.takeIf { Bukkit.getPlayer(it.uuid)?.isOnline == true }
            is ReplyTarget.Remote -> target.takeIf { remotePlayerRegistry?.serverOf(it.playerName) == it.serverName }
            null -> null
        }

    /**
     * Clears message history for a player (called on disconnect).
     * Removes entries where this player is either the sender or a local target.
     */
    fun clearPlayer(player: Player) {
        val playerId = player.uniqueId

        // Remove entries where this player is the sender
        lastMessager.remove(playerId)
        lastRecipient.remove(playerId)

        // Remove entries where this player is the local target
        lastMessager.entries.removeIf { (it.value as? ReplyTarget.Local)?.uuid == playerId }
        lastRecipient.entries.removeIf { (it.value as? ReplyTarget.Local)?.uuid == playerId }
    }

    /**
     * Sends a direct message from one player to another on the same server.
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

        val displayMessage = convertIfEnabled(message, senderSettings?.japaneseConversionEnabled == true)

        val format = configuration.messageFormat.directMessageFormat

        notifySpies(format, sender.name, recipient.name, message)

        val userMessage = formatMessage(format, sender.name, recipient.name, displayMessage, replyTo = sender.name)
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

    /**
     * Handles the sender-side display of an outgoing cross-server direct message.
     * Applies romaji conversion, shows the message to the sender, notifies spies,
     * and records the remote reply target.
     *
     * @return the message body to relay (romaji-converted if applicable), since the
     *   receiving server has no access to the sender's settings.
     */
    fun handleOutgoingCrossServerMessage(
        sender: Player,
        targetName: String,
        targetServerName: String,
        message: String,
    ): String {
        val senderSettings = settingsManager?.getSettings(sender.uniqueId)
        val displayMessage = convertIfEnabled(message, senderSettings?.japaneseConversionEnabled == true)

        val format = configuration.messageFormat.directMessageFormat
        val recipientDisplay = "$targetName@$targetServerName"

        notifySpies(format, sender.name, recipientDisplay, message)

        val userMessage =
            formatMessage(format, sender.name, recipientDisplay, displayMessage, replyTo = recipientDisplay)
        sender.apply {
            sendMessage(userMessage)
            takeIf { senderSettings?.directMessageNotificationEnabled == true }
                ?.playMessageSendNotification()
        }

        lastRecipient[sender.uniqueId] = ReplyTarget.Remote(targetName, targetServerName)
        return displayMessage
    }

    /**
     * Handles the receiver-side display of an incoming cross-server direct message.
     * The message body is already romaji-converted by the sending server.
     */
    fun handleIncomingCrossServerMessage(
        recipient: Player,
        senderName: String,
        sourceServerName: String,
        message: String,
    ) {
        val recipientSettings = settingsManager?.getSettings(recipient.uniqueId)
        val format = configuration.messageFormat.directMessageFormat
        val senderDisplay = "$senderName@$sourceServerName"

        val userMessage =
            formatMessage(format, senderDisplay, recipient.name, message, replyTo = senderDisplay)
        recipient.apply {
            sendMessage(userMessage)
            takeIf { recipientSettings?.directMessageNotificationEnabled == true }
                ?.playDirectMessageNotification()
        }

        lastMessager[recipient.uniqueId] = ReplyTarget.Remote(senderName, sourceServerName)
    }

    private fun convertIfEnabled(
        message: String,
        enabled: Boolean,
    ): String =
        if (enabled && romanjiConverter != null) {
            convertWithRomaji(message, romanjiConverter)
        } else {
            message
        }

    private fun notifySpies(
        format: String,
        senderName: String,
        recipientName: String,
        rawMessage: String,
    ) {
        val spyMessage = formatMessage(format, senderName, recipientName, rawMessage, replyTo = senderName)
        SpyPermissionManager
            .getDirectMessageSpyPlayers()
            .values
            .filter { it.isOnline && it.name !in setOf(senderName, recipientName) }
            .forEach {
                it.sendMessage(
                    spyMessage.hoverEvent(
                        HoverEvent.showText(
                            Component.text(languageManager.getMessage("general.spyMessage")),
                        ),
                    ),
                )
            }
    }

    private fun formatMessage(
        format: String,
        senderName: String,
        recipientName: String,
        message: String,
        replyTo: String,
    ): Component {
        val text =
            format
                .replace("{sender}", senderName)
                .replace("{recipient}", recipientName)
                .replace("{message}", message)

        return Component
            .text(text)
            .clickEvent(ClickEvent.suggestCommand("/tell $replyTo "))
    }
}
