package dev.m1sk9.lunaticChat.paper.settings

import com.charleskorn.kaml.Yaml
import dev.m1sk9.lunaticChat.engine.settings.PlayerSettingsData
import dev.m1sk9.lunaticChat.paper.DebouncedSaver
import dev.m1sk9.lunaticChat.paper.writeTextAtomically
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists

/**
 * Handles YAML file I/O operations for player settings.
 * Provides async save with debouncing.
 *
 * @property settingsFile The path to the YAML settings file
 * @property saver Coalesces bursts of save requests into one asynchronous write
 * @property logger The logger for logging operations
 */
class YamlPlayerSettingsStorage(
    private val settingsFile: Path,
    private val saver: DebouncedSaver,
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
        if (!settingsFile.exists()) {
            logger.info("Settings file not found, will create on first save")
            return PlayerSettingsData()
        }

        return try {
            val yamlContent = settingsFile.bufferedReader().use { it.readText() }
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
     * A failed write leaves the previous file untouched: loading falls back to empty settings when
     * the YAML does not parse, so a torn file would silently discard every player's settings.
     *
     * @param data The settings data to save
     */
    fun saveToDisk(data: PlayerSettingsData) {
        try {
            val yamlContent = yaml.encodeToString(PlayerSettingsData.serializer(), data)
            settingsFile.writeTextAtomically(yamlContent)
            logger.fine("Saved player settings to disk")
        } catch (e: Exception) {
            logger.severe("Failed to save settings: ${e.message}")
        }
    }

    /**
     * Queues an async save operation with 5-second debouncing.
     * Multiple save requests within 5 seconds are batched into a single save.
     *
     * @param data Supplies the settings to write. It is called when the write runs rather than
     *   when it is queued, so the batched write persists every change made during the delay - not
     *   just the one that started it.
     */
    fun queueAsyncSave(data: () -> PlayerSettingsData) {
        saver.request { saveToDisk(data()) }
    }
}
