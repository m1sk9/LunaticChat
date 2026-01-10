package dev.m1sk9.lunaticChat.paper.config

import dev.m1sk9.lunaticChat.paper.config.key.FeaturesConfig
import dev.m1sk9.lunaticChat.paper.config.key.JapaneseConversionFeatureConfig
import dev.m1sk9.lunaticChat.paper.config.key.MessageFormatConfig
import dev.m1sk9.lunaticChat.paper.config.key.QuickRepliesFeatureConfig
import org.bukkit.configuration.file.FileConfiguration

object ConfigManager {
    private var lunaticChatConfiguration: LunaticChatConfiguration? = null

    fun getConfiguration(): LunaticChatConfiguration =
        lunaticChatConfiguration ?: IllegalStateException("LunaticChat Config not loaded").let { throw it }

    fun loadConfiguration(configFile: FileConfiguration): LunaticChatConfiguration {
        val loadedConfig =
            LunaticChatConfiguration(
                features =
                    FeaturesConfig(
                        quickRepliesEnabled =
                            QuickRepliesFeatureConfig(
                                enabled =
                                    configFile.getBoolean("features.quickReplies.enabled", true),
                            ),
                        japaneseConversion =
                            JapaneseConversionFeatureConfig(
                                enabled = configFile.getBoolean("features.japaneseConversion.enabled", false),
                                cacheMaxEntries = configFile.getInt("features.japaneseConversion.cacheMaxEntries", 500),
                                cacheSaveIntervalSeconds =
                                    configFile.getInt(
                                        "features.japaneseConversion.cacheSaveIntervalSeconds",
                                        300,
                                    ),
                                cacheFilePath =
                                    configFile.getString(
                                        "features.japaneseConversion.cacheFilePath",
                                        "conversion_cache.json",
                                    )!!,
                                apiTimeout =
                                    configFile.getLong(
                                        "features.japaneseConversion.apiTimeout",
                                        3000,
                                    ),
                                apiRetryAttempts = configFile.getInt("features.japaneseConversion.apiRetryAttempts", 2),
                            ),
                    ),
                messageFormat =
                    MessageFormatConfig(
                        directMessageFormat =
                            configFile.getString(
                                "messageFormat.directMessageFormat",
                                "§7[§e{sender} §7>> §e{recipient}§7] §f{message}",
                            )!!,
                    ),
                debug = configFile.getBoolean("debug", false),
            )

        lunaticChatConfiguration = loadedConfig
        return loadedConfig
    }
}
