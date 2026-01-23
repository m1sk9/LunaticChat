package dev.m1sk9.lunaticChat.paper

import dev.m1sk9.lunaticChat.paper.command.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager

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
 */
data class ServiceContainer(
    val languageManager: LanguageManager,
    val playerSettingsManager: PlayerSettingsManager,
    val directMessageHandler: DirectMessageHandler,
    val romajiConverter: RomanjiConverter? = null,
)
