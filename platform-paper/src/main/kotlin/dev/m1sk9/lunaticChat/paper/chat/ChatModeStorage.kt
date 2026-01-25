package dev.m1sk9.lunaticChat.paper.chat

import dev.m1sk9.lunaticChat.engine.chat.ChatModeData
import dev.m1sk9.lunaticChat.engine.exception.ChatModeStorageException
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Handles JSON storage for chat mode data.
 * Provides async save queue and synchronous save for shutdown.
 *
 * @property dataFile Path to chatmodes.json
 * @property logger Logger for operations
 */
class ChatModeStorage(
    private val dataFile: Path,
    private val logger: Logger,
) {
    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

    private val saveExecutor = Executors.newSingleThreadExecutor()

    /**
     * Loads chat mode data from disk.
     *
     * @return ChatModeData loaded from file, or empty data if file doesn't exist
     * @throws ChatModeStorageException if loading fails
     */
    fun loadFromDisk(): ChatModeData {
        if (!dataFile.exists()) {
            logger.info("Chat mode data file not found, starting with empty data")
            return ChatModeData()
        }

        return try {
            val jsonContent = dataFile.readText()
            json.decodeFromString<ChatModeData>(jsonContent)
        } catch (e: Exception) {
            throw ChatModeStorageException("Failed to load chat mode data from ${dataFile.fileName}", e)
        }
    }

    /**
     * Queues an asynchronous save operation.
     *
     * @param data ChatModeData to save
     */
    fun queueAsyncSave(data: ChatModeData) {
        saveExecutor.submit {
            try {
                saveToDisk(data)
            } catch (e: Exception) {
                logger.severe("Failed to save chat mode data: ${e.message}")
            }
        }
    }

    /**
     * Saves chat mode data to disk synchronously.
     *
     * @param data ChatModeData to save
     * @throws ChatModeStorageException if saving fails
     */
    fun saveToDisk(data: ChatModeData) {
        try {
            val jsonContent = json.encodeToString(data)
            dataFile.writeText(jsonContent)
        } catch (e: Exception) {
            throw ChatModeStorageException("Failed to save chat mode data to ${dataFile.fileName}", e)
        }
    }

    /**
     * Shuts down the async save executor.
     * Should be called during plugin disable.
     */
    fun shutdown() {
        saveExecutor.shutdown()
        try {
            if (!saveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                saveExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            saveExecutor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}
