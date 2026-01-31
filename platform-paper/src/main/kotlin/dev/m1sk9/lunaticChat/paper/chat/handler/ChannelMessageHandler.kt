package dev.m1sk9.lunaticChat.paper.chat.handler

import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelMessageLogEntry
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMessageLogger
import dev.m1sk9.lunaticChat.paper.common.SpyPermissionManager
import dev.m1sk9.lunaticChat.paper.common.playChannelReceiveNotification
import dev.m1sk9.lunaticChat.paper.common.playMessageSendNotification
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import io.ktor.util.logging.Logger
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class ChannelMessageHandler(
    private val configuration: LunaticChatConfiguration,
    private val settingsManager: PlayerSettingsManager?,
    private val channelManager: ChannelManager,
    private val romanjiConverter: RomanjiConverter?,
    private val languageManager: LanguageManager,
    private val messageLogger: ChannelMessageLogger?,
    private val logger: Logger,
) {
    fun sendChannelMessage(
        player: Player,
        message: String,
    ): Boolean {
        val playerId = player.uniqueId
        val context =
            channelManager.getPlayerChannelContext(playerId)
                ?: return false

        val senderSettings = settingsManager?.getSettings(playerId)

        // Handle romaji conversion if enabled
        // Uses explicit timeout to prevent long blocking (1s max instead of 3s)
        val displayMessage =
            if (senderSettings?.japaneseConversionEnabled == true && romanjiConverter != null) {
                runCatching {
                    kotlinx.coroutines.runBlocking {
                        kotlinx.coroutines
                            .withTimeoutOrNull(1000) {
                                romanjiConverter!!
                                    .convert(message)
                            }?.let { "$message §e($it)" } ?: message
                    }
                }.getOrElse { message }
            } else {
                message
            }

        val formattedMessage = formatChannelMessage(player.name, context.channel.name, displayMessage)
        val spyMessage = formatChannelMessage(player.name, context.channel.name, message)

        // Play notification sound to sender if enabled
        if (senderSettings?.channelMessageNotificationEnabled == true) {
            player.playMessageSendNotification()
        }

        // Send to spy players (exclude sender and channel members)
        val memberIds = context.members.map { it.playerId }.toSet()
        SpyPermissionManager
            .getDirectMessageSpyPlayers()
            .values
            .filter { it.isOnline && it.uniqueId != playerId && it.uniqueId !in memberIds }
            .forEach {
                it.sendMessage(
                    spyMessage.hoverEvent(
                        HoverEvent.showText(
                            Component.text(languageManager.getMessage("general.spyMessage")),
                        ),
                    ),
                )
            }
        context.members.forEach { member ->
            Bukkit.getPlayer(member.playerId)?.let { memberPlayer ->
                if (memberPlayer.isOnline) {
                    memberPlayer.sendMessage(formattedMessage)

                    // Play notification sound to receiver if enabled and not the sender
                    if (memberPlayer.uniqueId != playerId) {
                        settingsManager?.let { manager ->
                            val receiverSettings = manager.getSettings(memberPlayer.uniqueId)
                            if (receiverSettings.channelMessageNotificationEnabled) {
                                memberPlayer.playChannelReceiveNotification()
                            }
                        }
                    }
                }
            }
        }

        logger.info("Channel Message from ${player.name} in ${context.channel.name}: $message")

        // Log message to file if logging is enabled
        messageLogger?.let {
            val logEntry =
                ChannelMessageLogEntry.create(
                    playerId = player.uniqueId,
                    playerName = player.name,
                    channelId = context.channelId,
                    message = message,
                )
            it.logMessage(logEntry)
        }

        return true
    }

    private fun formatChannelMessage(
        senderName: String,
        channelName: String,
        message: String,
    ): Component {
        val format = configuration.messageFormat.channelMessageFormat
        val text =
            format
                .replace("{sender}", senderName)
                .replace("{channel}", channelName)
                .replace("{message}", message)

        return Component.text(text)
    }
}
