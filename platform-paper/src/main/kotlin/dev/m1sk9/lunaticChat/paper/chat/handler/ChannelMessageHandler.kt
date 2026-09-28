package dev.m1sk9.lunaticChat.paper.chat.handler

import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelContext
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelMessageLogEntry
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.engine.debug.DebugLogger
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMessageLogger
import dev.m1sk9.lunaticChat.paper.common.SpyPermissionManager
import dev.m1sk9.lunaticChat.paper.common.playChannelReceiveNotification
import dev.m1sk9.lunaticChat.paper.common.playMessageSendNotification
import dev.m1sk9.lunaticChat.paper.config.MessageFormatHolder
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.channelMessage
import dev.m1sk9.lunaticChat.paper.i18n.channelRoleLabel
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class ChannelMessageHandler(
    private val messageFormats: MessageFormatHolder,
    private val settingsManager: PlayerSettingsManager?,
    private val channelManager: ChannelManager,
    private val languageManager: LanguageManager,
    private val messageLogger: ChannelMessageLogger?,
    private val debug: DebugLogger,
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

        val formattedMessage = formatChannelMessage(player, context, message)

        // Play notification sound to sender if enabled
        if (senderSettings?.channelMessageNotificationEnabled == true) {
            player.playMessageSendNotification()
        }

        // Send to spy players (exclude sender and channel members). The member set is built lazily
        // because notifySpies only consults exclude when a spy is actually online.
        val memberIds by lazy { context.members.mapTo(HashSet()) { it.playerId } }
        SpyPermissionManager.notifySpies(
            noticeText = { languageManager.getMessage("general.spyMessage") },
            exclude = { it.uniqueId == playerId || it.uniqueId in memberIds },
        ) { formattedMessage }
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

        debug.log(DebugCategory.CHANNEL) {
            "Delivered a message from ${player.name} to ${context.members.size} members of ${context.channel.name}"
        }

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
        sender: Player,
        context: ChannelContext,
        message: String,
    ): Component =
        messageFormats.current.channelMessage(
            sender = sender.name,
            channel = context.channel.name,
            channelId = context.channel.id,
            displayName = sender.displayName(),
            message = message,
            world = sender.world.name,
            role = {
                val role = context.members.firstOrNull { it.playerId == sender.uniqueId }?.role ?: ChannelRole.MEMBER
                languageManager.channelRoleLabel(role)
            },
        )
}
