package dev.m1sk9.lunaticChat.paper.chat.channel

import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelData
import dev.m1sk9.lunaticChat.engine.exception.ChannelStorageLoadException
import dev.m1sk9.lunaticChat.engine.exception.ChannelStorageSaveException
import dev.m1sk9.lunaticChat.paper.storage.FileStore
import kotlinx.serialization.json.Json
import java.util.logging.Logger

/**
 * Manages the storage of channel data on disk.
 *
 * @property store The file channel data is read from and written to.
 * @property logger The logger for logging messages.
 */
class ChannelStorage(
    private val store: FileStore,
    private val logger: Logger,
) {
    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

    /**
     * Loads channel data from disk.
     *
     * @return The loaded ChannelData.
     * @throws ChannelStorageLoadException if there is an error loading the data.
     */
    fun loadFromDisk(): ChannelData {
        val jsonContent =
            store.read() ?: run {
                logger.warning("Channel storage not found, will create a new one.")
                return ChannelData()
            }

        return try {
            json.decodeFromString(ChannelData.serializer(), jsonContent).also {
                logger.info("Successfully loaded channels from ${store.name}.")
            }
        } catch (e: Exception) {
            throw ChannelStorageLoadException(
                "Failed to load channels from ${store.name}: ${e.message}",
                e,
            )
        }
    }

    /**
     * Saves channel data to disk.
     *
     * @param data The ChannelData to save.
     * @throws ChannelStorageSaveException if there is an error saving the data.
     */
    fun saveToDisk(data: ChannelData) {
        try {
            store.write(json.encodeToString(ChannelData.serializer(), data))
            logger.fine("Successfully saved channels from ${store.name}.")
        } catch (e: Exception) {
            throw ChannelStorageSaveException(
                "Failed to save channels to ${store.name}: ${e.message}",
                e,
            )
        }
    }

    /**
     * Queues a debounced asynchronous save of channel data to disk.
     *
     * @param data Supplies the channel data to write. It is called when the write runs rather
     *   than when it is queued, so a burst of channel changes costs one snapshot and one file
     *   write instead of one of each per change.
     */
    fun queueAsyncSave(data: () -> ChannelData) {
        store.queueWrite { json.encodeToString(ChannelData.serializer(), data()) }
    }
}
