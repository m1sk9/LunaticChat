package dev.m1sk9.lunaticChat.paper.config

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import java.util.logging.Logger

/**
 * Reads config.yml into [LunaticChatConfiguration].
 *
 * The file is deserialized directly rather than copied key by key, so a default lives only on the
 * data class. The hand-written mapper it replaced repeated every default in a second place, and
 * they had already drifted - checkForUpdates disagreed with both config.yml and the data class,
 * and the whole messageLogging block was documented but never read.
 */
class ConfigManager(
    private val logger: Logger,
) {
    private val yaml =
        Yaml(
            configuration =
                YamlConfiguration(
                    // A config.yml from a newer build, or one carrying a key we have retired,
                    // should not stop the plugin from starting.
                    strictMode = false,
                ),
        )

    /**
     * Parses [contents] as config.yml, falling back to defaults if it cannot be read.
     */
    fun loadConfiguration(contents: String): LunaticChatConfiguration =
        try {
            yaml.decodeFromString(LunaticChatConfiguration.serializer(), contents)
        } catch (e: Exception) {
            logger.severe("Failed to read config.yml, falling back to defaults: ${e.message}")
            LunaticChatConfiguration()
        }
}
