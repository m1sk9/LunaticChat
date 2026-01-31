package dev.m1sk9.lunaticChat.paper

import dev.m1sk9.lunaticChat.paper.chat.ChatModeManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelMessageHandler
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelNotificationHandler
import dev.m1sk9.lunaticChat.paper.chat.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import dev.m1sk9.lunaticChat.paper.velocity.VelocityConnectionManager

/**
 * Container for initialized services.
 *
 * This data class holds all services that have been initialized during plugin startup,
 * eliminating the need for null-assertion operators throughout the codebase.
 *
 * @property languageManager Always available (initialized first)
 * @property playerSettingsManager Always available (required for DM notifications)
 * @property directMessageHandler Always available (core feature)
 * @property romajiConverter Optional (only when Japanese conversion feature is enabled)
 * @property channelManager Optional (only when channel chat feature is enabled)
 * @property channelMembershipManager Optional (only when channel chat feature is enabled)
 * @property chatModeManager Optional (only when channel chat feature is enabled)
 * @property channelMessageHandler Optional (only when channel chat feature is enabled)
 * @property channelNotificationHandler Optional (only when channel chat feature is enabled)
 * @property velocityConnectionManager Optional (only when Velocity integration is enabled)
 */
data class ServiceContainer(
    val languageManager: LanguageManager,
    val playerSettingsManager: PlayerSettingsManager,
    val directMessageHandler: DirectMessageHandler,
    val romajiConverter: RomanjiConverter? = null,
    val channelManager: ChannelManager? = null,
    val channelMembershipManager: ChannelMembershipManager? = null,
    val chatModeManager: ChatModeManager? = null,
    val channelMessageHandler: ChannelMessageHandler? = null,
    val channelNotificationHandler: ChannelNotificationHandler? = null,
    val velocityConnectionManager: VelocityConnectionManager? = null,
)
