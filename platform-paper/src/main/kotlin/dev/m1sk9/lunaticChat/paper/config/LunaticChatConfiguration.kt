package dev.m1sk9.lunaticChat.paper.config

import dev.m1sk9.lunaticChat.paper.config.key.FeaturesConfig
import dev.m1sk9.lunaticChat.paper.config.key.MessageFormatConfig
import dev.m1sk9.lunaticChat.paper.config.key.VelocityConfig

data class LunaticChatConfiguration(
    val features: FeaturesConfig,
    val messageFormat: MessageFormatConfig,
    val velocity: VelocityConfig,
    val debug: Boolean = false,
)
