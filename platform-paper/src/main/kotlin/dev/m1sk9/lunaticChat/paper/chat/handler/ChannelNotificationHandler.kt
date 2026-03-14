package dev.m1sk9.lunaticChat.paper.chat.handler

import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import org.bukkit.Bukkit

/**
 * Handles broadcasting notifications to channel members.
 */
class ChannelNotificationHandler(
    private val channelManager: ChannelManager,
    private val languageManager: LanguageManager,
) {
    fun broadcastJoin(
        channelId: String,
        playerName: String,
    ) {
        broadcastToMembers(
            channelId,
            "channel.notification.playerJoined",
            mapOf("player" to playerName),
        )
    }

    fun broadcastLeave(
        channelId: String,
        playerName: String,
    ) {
        broadcastToMembers(
            channelId,
            "channel.notification.playerLeft",
            mapOf("player" to playerName),
        )
    }

    fun broadcastKick(
        channelId: String,
        kickedPlayerName: String,
        kickerName: String,
    ) {
        broadcastToMembers(
            channelId,
            "channel.notification.playerKicked",
            mapOf("player" to kickedPlayerName, "kicker" to kickerName),
        )
    }

    fun broadcastBan(
        channelId: String,
        bannedPlayerName: String,
        bannerName: String,
    ) {
        broadcastToMembers(
            channelId,
            "channel.notification.playerBanned",
            mapOf("player" to bannedPlayerName, "banner" to bannerName),
        )
    }

    private fun broadcastToMembers(
        channelId: String,
        messageKey: String,
        placeholders: Map<String, String>,
    ) {
        val channel = channelManager.getChannel(channelId).getOrNull() ?: return
        val members = channelManager.getChannelMembers(channelId).getOrNull() ?: return

        val message = languageManager.getMessage(messageKey, placeholders + ("channel" to channel.name))
        val formattedMessage = MessageFormatter.format(message)

        members.forEach { member ->
            Bukkit.getPlayer(member.playerId)?.let { player ->
                if (player.isOnline) {
                    player.sendMessage(formattedMessage)
                }
            }
        }
    }
}
