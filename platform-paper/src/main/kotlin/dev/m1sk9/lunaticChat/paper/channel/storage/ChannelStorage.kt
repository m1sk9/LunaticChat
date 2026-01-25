package dev.m1sk9.lunaticChat.paper.channel.storage

import dev.m1sk9.lunaticChat.engine.channel.modal.ChannelData
import dev.m1sk9.lunaticChat.engine.exception.ChannelStorageLoadException
import dev.m1sk9.lunaticChat.engine.exception.ChannelStorageSaveException
import kotlinx.serialization.json.Json
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * Manages the storage of channel data on disk.
 *
 * @property channelsFile The path to the file where channel data is stored.
 * @property plugin The JavaPlugin instance for accessing plugin resources.
 * @property logger The logger for logging messages.
 */
class ChannelStorage(
    private val channelsFile: Path,
    private val plugin: JavaPlugin,
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
            channelsFile.writeText(jsonContent).also {
                logger.fine("Successfully saved channels from ${channelsFile.fileName}.")
            }
        } catch (e: Exception) {
            throw ChannelStorageSaveException(
                "Failed to save channels to ${channelsFile.fileName}: ${e.message}",
                e,
            )
        }
    }

    /**
     * Queues an asynchronous save of channel data to disk.
     *
     * @param data The ChannelData to save.
     * @throws ChannelStorageSaveException if there is an error saving the data.
     */
    fun queueAsyncSave(data: ChannelData) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                try {
                    saveToDisk(data)
                } catch (e: ChannelStorageSaveException) {
                    logger.severe("Error saving channel data asynchronously: ${e.message}")
                    e.printStackTrace()
                }
            },
        )
    }
}
