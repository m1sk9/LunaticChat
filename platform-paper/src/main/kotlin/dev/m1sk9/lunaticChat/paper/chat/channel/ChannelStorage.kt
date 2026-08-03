package dev.m1sk9.lunaticChat.paper.chat.channel

import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelData
import dev.m1sk9.lunaticChat.engine.exception.ChannelStorageLoadException
import dev.m1sk9.lunaticChat.engine.exception.ChannelStorageSaveException
import dev.m1sk9.lunaticChat.paper.DebouncedSaver
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.logging.Logger
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * Manages the storage of channel data on disk.
 *
 * @property channelsFile The path to the file where channel data is stored.
 * @property saver Coalesces bursts of save requests into one asynchronous write.
 * @property logger The logger for logging messages.
 */
class ChannelStorage(
    private val channelsFile: Path,
    private val saver: DebouncedSaver,
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
        if (!channelsFile.exists()) {
            logger.warning("Channel storage not found, will create a new one.")
            return ChannelData()
        }

        return try {
            val jsonContent =
                channelsFile.bufferedReader().use {
                    it.readText()
                }
            json.decodeFromString(ChannelData.serializer(), jsonContent).also {
                logger.info("Successfully loaded channels from ${channelsFile.fileName}.")
            }
        } catch (e: Exception) {
            throw ChannelStorageLoadException(
                "Failed to load channels from ${channelsFile.fileName}: ${e.message}",
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
            val jsonContent = json.encodeToString(ChannelData.serializer(), data)

            // Written to a sibling and moved into place. Bukkit runs onDisable before cancelling
            // scheduler tasks, so the shutdown save and a still-pending debounced save can reach
            // this at the same time; two truncating writes to the same path would interleave and
            // leave channels.json unparseable.
            val temporaryFile = channelsFile.resolveSibling("${channelsFile.fileName}.tmp")
            temporaryFile.writeText(jsonContent)
            Files.move(temporaryFile, channelsFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            logger.fine("Successfully saved channels from ${channelsFile.fileName}.")
        } catch (e: Exception) {
            throw ChannelStorageSaveException(
                "Failed to save channels to ${channelsFile.fileName}: ${e.message}",
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
        saver.request {
            try {
                saveToDisk(data())
            } catch (e: ChannelStorageSaveException) {
                logger.severe("Error saving channel data asynchronously: ${e.message}")
            }
        }
    }
}
