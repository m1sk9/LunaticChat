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
    /**
     * Broadcasts a join notification to all members of a channel.
     *
     * @param channelId The ID of the channel.
     * @param playerName The name of the player who joined.
     */
    fun broadcastJoin(
        channelId: String,
        playerName: String,
    ) {
        val channel = channelManager.getChannel(channelId).getOrNull() ?: return
        val members = channelManager.getChannelMembers(channelId).getOrNull() ?: return

        val message =
            languageManager.getMessage(
                "channel.notification.playerJoined",
                mapOf("player" to playerName, "channel" to channel.name),
            )
        val formattedMessage = MessageFormatter.format(message)

        members.forEach { member ->
            Bukkit.getPlayer(member.playerId)?.let { player ->
                if (player.isOnline) {
                    player.sendMessage(formattedMessage)
                }
            }
        }
    }

    /**
     * Broadcasts a leave notification to all members of a channel.
     *
     * @param channelId The ID of the channel.
     * @param playerName The name of the player who left.
     */
    fun broadcastLeave(
        channelId: String,
        playerName: String,
    ) {
        val channel = channelManager.getChannel(channelId).getOrNull() ?: return
        val members = channelManager.getChannelMembers(channelId).getOrNull() ?: return

        val message =
            languageManager.getMessage(
                "channel.notification.playerLeft",
                mapOf("player" to playerName, "channel" to channel.name),
            )
        val formattedMessage = MessageFormatter.format(message)

        members.forEach { member ->
            Bukkit.getPlayer(member.playerId)?.let { player ->
                if (player.isOnline) {
                    player.sendMessage(formattedMessage)
                }
            }
        }
    }

    /**
     * Broadcasts a kick notification to all members of a channel.
     *
     * @param channelId The ID of the channel.
     * @param kickedPlayerName The name of the player who was kicked.
     * @param kickerName The name of the player who performed the kick.
     */
    fun broadcastKick(
        channelId: String,
        kickedPlayerName: String,
        kickerName: String,
    ) {
        val channel = channelManager.getChannel(channelId).getOrNull() ?: return
        val members = channelManager.getChannelMembers(channelId).getOrNull() ?: return

        val message =
            languageManager.getMessage(
                "channel.notification.playerKicked",
                mapOf(
                    "player" to kickedPlayerName,
                    "channel" to channel.name,
                    "kicker" to kickerName,
                ),
            )
        val formattedMessage = MessageFormatter.format(message)

        members.forEach { member ->
            Bukkit.getPlayer(member.playerId)?.let { player ->
                if (player.isOnline) {
                    player.sendMessage(formattedMessage)
                }
            }
        }
    }

    /**
     * Broadcasts a ban notification to all members of a channel.
     *
     * @param channelId The ID of the channel.
     * @param bannedPlayerName The name of the player who was banned.
     * @param bannerName The name of the player who performed the ban.
     */
    fun broadcastBan(
        channelId: String,
        bannedPlayerName: String,
        bannerName: String,
    ) {
        val channel = channelManager.getChannel(channelId).getOrNull() ?: return
        val members = channelManager.getChannelMembers(channelId).getOrNull() ?: return

        val message =
            languageManager.getMessage(
                "channel.notification.playerBanned",
                mapOf(
                    "player" to bannedPlayerName,
                    "channel" to channel.name,
                    "banner" to bannerName,
                ),
            )
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
