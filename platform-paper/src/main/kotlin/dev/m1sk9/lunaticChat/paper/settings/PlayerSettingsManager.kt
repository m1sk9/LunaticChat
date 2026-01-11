package dev.m1sk9.lunaticChat.paper.settings

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

/**
 * Manages player chat settings with YAML-based persistence.
 * All settings are loaded into memory at startup and saved asynchronously on changes.
 *
 * @property storage The YAML storage layer
 * @property logger The logger for logging operations
 */
class PlayerSettingsManager(
    private val storage: YamlPlayerSettingsStorage,
    private val logger: Logger,
) {
    private val japaneseConversionCache = ConcurrentHashMap<UUID, Boolean>()
    private lateinit var settingsData: PlayerSettingsData

    /**
     * Initializes the settings manager by loading all settings from disk into memory.
     * This should be called once during plugin startup.
     */
    fun initialize() {
        settingsData = storage.loadFromDisk()
        japaneseConversionCache.putAll(settingsData.japaneseConversion)
        logger.info("Loaded settings for ${japaneseConversionCache.size} players")
    }

    /**
     * Retrieves settings for a player.
     * If settings don't exist, returns default settings.
     *
     * @param uuid The UUID of the player
     * @return The player's settings
     */
    fun getSettings(uuid: UUID): PlayerChatSettings {
        val enabled = japaneseConversionCache.getOrDefault(uuid, false)
        return PlayerChatSettings(uuid = uuid, japaneseConversionEnabled = enabled)
    }

    /**
     * Updates player settings in cache and queues async save to disk.
     *
     * @param settings The updated settings to save
     */
    fun updateSettings(settings: PlayerChatSettings) {
        japaneseConversionCache[settings.uuid] = settings.japaneseConversionEnabled

        settingsData =
            settingsData.copy(
                japaneseConversion = japaneseConversionCache.toMap(),
            )

        storage.queueAsyncSave(settingsData)
        logger.fine("Updated settings for player ${settings.uuid}")
    }

    /**
     * Forces an immediate synchronous save of all settings to disk.
     * This should only be called during plugin shutdown.
     */
    fun saveToDisk() {
        storage.saveToDisk(settingsData)
    }
}
