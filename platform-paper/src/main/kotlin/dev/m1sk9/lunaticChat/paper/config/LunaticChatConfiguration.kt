package dev.m1sk9.lunaticChat.paper.config

import dev.m1sk9.lunaticChat.paper.config.key.DebugConfig
import dev.m1sk9.lunaticChat.paper.config.key.FeaturesConfig
import dev.m1sk9.lunaticChat.paper.config.key.MessageFormatConfig
import dev.m1sk9.lunaticChat.paper.i18n.Language
import dev.m1sk9.lunaticChat.paper.i18n.LanguageSerializer
import kotlinx.serialization.Serializable

/**
 * The shape of config.yml.
 *
 * Every property carries the default that config.yml documents, and it is the only place that
 * default is written: the file is deserialized straight into this tree, so a key the user has not
 * set simply falls back here.
 */
@Serializable
data class LunaticChatConfiguration(
    val features: FeaturesConfig = FeaturesConfig(),
    val messageFormat: MessageFormatConfig = MessageFormatConfig(),
    val debug: DebugConfig = DebugConfig(),
    val userSettingsFilePath: String = "player-settings.yaml",
    val checkForUpdates: LenientBoolean = true,
    @Serializable(with = LanguageSerializer::class)
    val language: Language = Language.EN,
)
