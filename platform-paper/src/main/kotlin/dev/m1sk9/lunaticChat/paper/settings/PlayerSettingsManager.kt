package dev.m1sk9.lunaticChat.paper.settings

import kotlinx.serialization.json.Json
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger
import kotlin.io.path.bufferedReader
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * Manages per-player chat settings persistence.
 * Provides in-memory cache with async disk I/O for player settings.
 *
 * @property settingsDirectory The directory where player settings files are stored
 * @property plugin The plugin instance for scheduling async tasks
 * @property logger The logger for logging operations
 */
class PlayerSettingsManager(
    private val settingsDirectory: Path,
    private val plugin: JavaPlugin,
    private val logger: Logger,
) {
    private val settingsCache = ConcurrentHashMap<UUID, PlayerChatSettings>()
    private val saveQueue = ConcurrentHashMap<UUID, AtomicBoolean>()

    /**
     * Initializes the settings directory if it doesn't exist.
     */
    fun initializeDirectory() {
        if (!settingsDirectory.exists()) {
            settingsDirectory.createDirectories()
            logger.info("Created player settings directory at: $settingsDirectory")
        }
    }

    /**
     * Loads player settings from disk into memory.
     * If the settings file doesn't exist, creates default settings.
     *
     * @param uuid The UUID of the player
     * @return The loaded or newly created player settings
     */
    fun loadPlayerSettings(uuid: UUID): PlayerChatSettings {
        val settingsFile = settingsDirectory.resolve("$uuid.json")

        if (!settingsFile.exists()) {
            logger.fine("Settings file not found for player $uuid, creating default settings.")
            val defaultSettings = PlayerChatSettings(uuid = uuid, japaneseConversionEnabled = false)
            settingsCache[uuid] = defaultSettings
            queueSaveToDisk(uuid)
            return defaultSettings
        }

        try {
            val jsonBuffer = settingsFile.bufferedReader().use { it.readText() }
            val settings = Json.decodeFromString<PlayerChatSettings>(jsonBuffer)
            settingsCache[uuid] = settings
            logger.fine("Loaded settings for player $uuid from disk.")
            return settings
        } catch (e: Exception) {
            logger.warning("Failed to load settings for player $uuid: ${e.message}. Using default settings.")
            val defaultSettings = PlayerChatSettings(uuid = uuid, japaneseConversionEnabled = false)
            settingsCache[uuid] = defaultSettings
            return defaultSettings
        }
    }

    /**
     * Retrieves settings from cache.
     * If settings are not cached, creates and caches default settings.
     *
     * @param uuid The UUID of the player
     * @return The player's settings
     */
    fun getSettings(uuid: UUID): PlayerChatSettings =
        settingsCache.getOrPut(uuid) {
            PlayerChatSettings(uuid = uuid, japaneseConversionEnabled = false)
        }

    /**
     * Updates player settings in cache and queues async save to disk.
     *
     * @param settings The updated settings to save
     */
    fun updateSettings(settings: PlayerChatSettings) {
        settingsCache[settings.uuid] = settings
        queueSaveToDisk(settings.uuid)
        logger.fine("Updated settings for player ${settings.uuid}")
    }

    /**
     * Removes player settings from cache.
     * Called when a player quits. Settings remain persisted on disk.
     *
     * @param uuid The UUID of the player
     */
    fun unloadPlayerSettings(uuid: UUID) {
        settingsCache.remove(uuid)
        saveQueue.remove(uuid)
        logger.fine("Unloaded settings for player $uuid from cache.")
    }

    /**
     * Saves player settings from memory to disk immediately.
     * This operation is synchronous and should only be called from async context.
     *
     * @param uuid The UUID of the player
     */
    private fun savePlayerSettings(uuid: UUID) {
        val settings = settingsCache[uuid] ?: return

        try {
            val settingsFile = settingsDirectory.resolve("$uuid.json")
            val jsonBuffer = Json.encodeToString(PlayerChatSettings.serializer(), settings)
            settingsFile.writeText(jsonBuffer)
            logger.fine("Saved settings for player $uuid to disk.")
        } catch (e: Exception) {
            logger.severe("Failed to save settings for player $uuid: ${e.message}")
        }
    }

    /**
     * Queues an async save operation for a player's settings.
     * Multiple save requests within 5 seconds are batched into a single save operation.
     *
     * @param uuid The UUID of the player
     */
    private fun queueSaveToDisk(uuid: UUID) {
        val queueFlag = saveQueue.getOrPut(uuid) { AtomicBoolean(false) }

        if (queueFlag.compareAndSet(false, true)) {
            Bukkit.getScheduler().runTaskAsynchronously(
                plugin,
                Runnable {
                    Thread.sleep(5000) // 5 seconds delay to batch multiple save requests
                    queueFlag.set(false)
                    savePlayerSettings(uuid)
                },
            )
        }
    }
}
