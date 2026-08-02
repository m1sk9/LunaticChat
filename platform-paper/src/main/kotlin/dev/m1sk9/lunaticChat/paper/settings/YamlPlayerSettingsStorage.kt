package dev.m1sk9.lunaticChat.paper.settings

import com.charleskorn.kaml.Yaml
import dev.m1sk9.lunaticChat.engine.settings.PlayerSettingsData
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * Handles YAML file I/O operations for player settings.
 * Provides async save with debouncing.
 *
 * @property settingsFile The path to the YAML settings file
 * @property plugin The plugin instance for scheduling async tasks
 * @property logger The logger for logging operations
 */
class YamlPlayerSettingsStorage(
    private val settingsFile: Path,
    private val plugin: JavaPlugin,
    private val logger: Logger,
) {
    private val yaml = Yaml.default
    private val saveFlag = AtomicBoolean(false)

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
     * @param data The settings data to save
     */
    fun saveToDisk(data: PlayerSettingsData) {
        try {
            val yamlContent = yaml.encodeToString(PlayerSettingsData.serializer(), data)
            settingsFile.writeText(yamlContent)
            logger.fine("Saved player settings to disk")
        } catch (e: Exception) {
            logger.severe("Failed to save settings: ${e.message}")
        }
    }

    /**
     * Queues an async save operation with 5-second debouncing.
     * Multiple save requests within 5 seconds are batched into a single save.
     *
     * @param data The settings data to save
     */
    fun queueAsyncSave(data: PlayerSettingsData) {
        if (saveFlag.compareAndSet(false, true)) {
            plugin.server.asyncScheduler.runDelayed(
                plugin,
                {
                    saveFlag.set(false)
                    saveToDisk(data)
                },
                5,
                TimeUnit.SECONDS,
            )
        }
    }
}
