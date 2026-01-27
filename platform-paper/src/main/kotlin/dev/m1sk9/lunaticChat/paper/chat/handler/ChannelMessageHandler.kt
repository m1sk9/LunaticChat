package dev.m1sk9.lunaticChat.paper.chat.handler

import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.common.SpyPermissionManager
import dev.m1sk9.lunaticChat.paper.common.playChannelReceiveNotification
import dev.m1sk9.lunaticChat.paper.common.playMessageSendNotification
import dev.m1sk9.lunaticChat.paper.config.ConfigManager
import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import io.ktor.util.logging.Logger
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class ChannelMessageHandler(
    private val settingsManager: PlayerSettingsManager?,
    private val channelManager: ChannelManager,
    private val romanjiConverter: RomanjiConverter?,
    private val logger: Logger,
) {
    private var lunaticChatConfiguration = ConfigManager.getConfiguration()

    fun sendChannelMessage(
        player: Player,
        message: String,
    ): Boolean {
        val playerId = player.uniqueId
        val context =
            channelManager.getPlayerChannelContext(playerId)
                ?: return false

        val senderSettings = settingsManager?.getSettings(playerId)

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
            .forEach { it.sendMessage(spyMessage) }
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
        return true
    }

    private fun formatChannelMessage(
        senderName: String,
        channelName: String,
        message: String,
    ): Component {
        val format = lunaticChatConfiguration.messageFormat.channelMessageFormat
        val text =
            format
                .replace("{sender}", senderName)
                .replace("{channel}", channelName)
                .replace("{message}", message)

        return Component.text(text)
    }
}
