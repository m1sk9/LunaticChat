package dev.m1sk9.lunaticChat.paper.settings

import dev.m1sk9.lunaticChat.engine.settings.PlayerChatSettings
import dev.m1sk9.lunaticChat.engine.settings.PlayerSettingsData
import dev.m1sk9.lunaticChat.paper.StoppableService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
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
) : StoppableService {
    private val settings = ConcurrentHashMap<UUID, PlayerChatSettings>()
    private val dirty = AtomicBoolean(false)

    // Written back unchanged: nothing migrates on it yet, but rewriting the file must not
    // silently relabel a schema this build does not understand.
    private var schemaVersion = PlayerSettingsData().version

    /**
     * Initializes the settings manager by loading all settings from disk into memory.
     * This should be called once during plugin startup.
     */
    fun initialize() {
        val data = storage.loadFromDisk()
        schemaVersion = data.version
        val knownPlayers =
            data.japaneseConversion.keys + data.directMessageNotification.keys + data.channelMessageNotification.keys

        knownPlayers.forEach { uuid ->
            settings[uuid] =
                PlayerChatSettings(
                    uuid = uuid,
                    japaneseConversionEnabled = data.japaneseConversion.getOrDefault(uuid, true),
                    directMessageNotificationEnabled = data.directMessageNotification.getOrDefault(uuid, true),
                    channelMessageNotificationEnabled = data.channelMessageNotification.getOrDefault(uuid, true),
                )
        }
        logger.info("Loaded settings for ${settings.size} players")
    }

    /**
     * Retrieves settings for a player.
     * If settings don't exist, returns default settings.
     *
     * @param uuid The UUID of the player
     * @return The player's settings
     */
    fun getSettings(uuid: UUID): PlayerChatSettings = settings[uuid] ?: PlayerChatSettings(uuid = uuid)

    /**
     * Updates player settings in cache and queues async save to disk.
     *
     * @param settings The updated settings to save
     */
    fun updateSettings(settings: PlayerChatSettings) {
        this.settings[settings.uuid] = settings
        dirty.set(true)
        storage.queueAsyncSave(::snapshot)
        logger.fine("Updated settings for player ${settings.uuid}")
    }

    /**
     * Queues a debounced asynchronous save, unless nothing has changed since the last write.
     *
     * Used where the caller wants what is already in memory flushed soon - a player leaving, say -
     * rather than paying for a write it does not need. [updateSettings] is the only thing that
     * changes a setting, and it queues its own save, so a quit almost always has nothing to persist:
     * without the guard every quit re-serialized every player ever stored in the file to write bytes
     * identical to the ones already there.
     */
    fun queueSave() {
        if (!dirty.get()) return
        storage.queueAsyncSave(::snapshot)
    }

    /**
     * Forces an immediate synchronous save of all settings to disk.
     *
     * Serializes every stored player and writes the whole file inline, so this belongs on the
     * shutdown path only; everywhere else should use [queueSave] or [updateSettings].
     */
    fun saveToDisk() {
        storage.saveToDisk(snapshot())
    }

    override fun stop() = saveToDisk()

    private fun snapshot(): PlayerSettingsData {
        // Cleared where the snapshot is taken rather than after the write: a change made while the
        // write is in flight must leave the flag set so the next queueSave still fires.
        dirty.set(false)
        return PlayerSettingsData(
            version = schemaVersion,
            japaneseConversion = settings.mapValues { it.value.japaneseConversionEnabled },
            directMessageNotification = settings.mapValues { it.value.directMessageNotificationEnabled },
            channelMessageNotification = settings.mapValues { it.value.channelMessageNotificationEnabled },
        )
    }
}
