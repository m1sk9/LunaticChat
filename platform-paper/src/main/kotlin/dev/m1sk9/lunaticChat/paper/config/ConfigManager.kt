package dev.m1sk9.lunaticChat.paper.config

import dev.m1sk9.lunaticChat.paper.config.key.FeaturesConfig
import dev.m1sk9.lunaticChat.paper.config.key.MessageFormatConfig
import dev.m1sk9.lunaticChat.paper.config.key.VelocityConfig
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
                        japaneseConversionEnabled = configFile.getBoolean("features.japaneseConversionEnabled", false),
                        quickRepliesEnabled = configFile.getBoolean("features.quickRepliesEnabled", true),
                    ),
                messageFormat =
                    MessageFormatConfig(
                        directMessageFormat =
                            configFile.getString(
                                "messageFormat.directMessageFormat",
                                "&7[&e{sender} &7>>] &f{message}",
                            )!!,
                        crossServerMessageFormat =
                            configFile.getString(
                                "messageFormat.crossServerMessageFormat",
                                "&7[&e{sender} &7(&b{server}&7)] &f{message}",
                            )!!,
                        crossServerDirectMessageFormat =
                            configFile.getString(
                                "messageFormat.crossServerDirectMessageFormat",
                                "&7[&e{sender}@&7(&b{server}&7) >>] &f{message}",
                            )!!,
                    ),
                velocity =
                    VelocityConfig(
                        enabled = configFile.getBoolean("velocity.enabled", false),
                        serverName = configFile.getString("velocity.serverName", "s1")!!,
                        crossServerChatEnabled = configFile.getBoolean("velocity.crossServerChatEnabled", false),
                        crossServerDirectMessagesEnabled =
                            configFile.getBoolean(
                                "velocity.crossServerDirectMessagesEnabled",
                                false,
                            ),
                    ),
                debug = configFile.getBoolean("debug", false),
            )

        lunaticChatConfiguration = loadedConfig
        return loadedConfig
    }
}
