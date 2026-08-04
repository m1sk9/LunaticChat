package dev.m1sk9.lunaticChat.paper.settings

import com.charleskorn.kaml.Yaml
import dev.m1sk9.lunaticChat.engine.settings.PlayerSettingsData
import dev.m1sk9.lunaticChat.paper.storage.FileStore
import java.util.logging.Logger

/**
 * Handles YAML serialization for player settings.
 *
 * @property store The file settings are read from and written to
 * @property logger The logger for logging operations
 */
class YamlPlayerSettingsStorage(
    private val store: FileStore,
    private val logger: Logger,
) {
    private val yaml = Yaml.default

    /**
     * Loads player settings from the YAML file.
     * If the file doesn't exist or cannot be parsed, returns empty settings.
     *
     * @return The loaded settings or empty settings if file doesn't exist
     */
    fun loadFromDisk(): PlayerSettingsData {
        val yamlContent =
            store.read() ?: run {
                logger.info("Settings file not found, will create on first save")
                return PlayerSettingsData()
            }

        return try {
            yaml.decodeFromString(PlayerSettingsData.serializer(), yamlContent)
        } catch (e: Exception) {
            logger.severe("Failed to load settings file: ${e.message}")
            logger.warning("Using empty settings as fallback")
            PlayerSettingsData()
        }
    }

    /**
     * Saves player settings to the YAML file synchronously.
     * This should only be called from async context or during shutdown.
     *
     * @param data The settings data to save
     */
    fun saveToDisk(data: PlayerSettingsData) {
        try {
            store.write(yaml.encodeToString(PlayerSettingsData.serializer(), data))
            logger.fine("Saved player settings to disk")
        } catch (e: Exception) {
            logger.severe("Failed to save settings: ${e.message}")
        }
    }

    /**
     * Queues a debounced asynchronous save.
     *
     * @param data Supplies the settings to write. It is called when the write runs rather than
     *   when it is queued, so the batched write persists every change made during the delay - not
     *   just the one that started it.
     */
    fun queueAsyncSave(data: () -> PlayerSettingsData) {
        store.queueWrite { yaml.encodeToString(PlayerSettingsData.serializer(), data()) }
    }
}
