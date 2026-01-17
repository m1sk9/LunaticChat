package dev.m1sk9.lunaticChat.paper.config

import dev.m1sk9.lunaticChat.paper.config.key.FeaturesConfig
import dev.m1sk9.lunaticChat.paper.config.key.MessageFormatConfig

data class LunaticChatConfiguration(
    val features: FeaturesConfig,
    val messageFormat: MessageFormatConfig,
    val debug: Boolean = false,
    val userSettingsFilePath: String = "player-settings.yaml",
    val checkForUpdates: Boolean = true,
)
