package dev.m1sk9.lunaticChat.paper.chat

import dev.m1sk9.lunaticChat.engine.chat.ChatMode
import dev.m1sk9.lunaticChat.engine.chat.ChatModeData
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

/**
 * Manages player chat modes with persistence.
 *
 * Chat modes determine where player messages are sent by default.
 * Modes are persisted across server restarts.
 *
 * @property storage Chat mode storage layer
 * @property logger Logger for operations
 */
class ChatModeManager(
    private val storage: ChatModeStorage,
    private val logger: Logger,
) {
    private val chatModes = ConcurrentHashMap<UUID, ChatMode>()

    /**
     * Initializes the manager by loading data from storage.
     */
    fun initialize() {
        val data = storage.loadFromDisk()
        chatModes.putAll(data.modes)
        logger.info("ChatModeManager initialized with ${chatModes.size} saved modes")
    }

    /**
     * Gets a player's current chat mode.
     *
     * @param playerId The player's UUID
     * @return The player's chat mode, or DEFAULT if not set
     */
    fun getChatMode(playerId: UUID): ChatMode = chatModes.getOrDefault(playerId, ChatMode.Companion.DEFAULT)

    /**
     * Sets a player's chat mode.
     *
     * @param playerId The player's UUID
     * @param mode The chat mode to set
     */
    fun setChatMode(
        playerId: UUID,
        mode: ChatMode,
    ) {
        chatModes[playerId] = mode
        saveToStorage()
    }

    /**
     * Toggles a player's chat mode between GLOBAL and CHANNEL.
     *
     * @param playerId The player's UUID
     * @return The new chat mode after toggling
     */
    fun toggleChatMode(playerId: UUID): ChatMode {
        val currentMode = getChatMode(playerId)
        val newMode = currentMode.toggle()
        setChatMode(playerId, newMode)
        return newMode
    }

    /**
     * Removes a player's chat mode setting (reverts to default).
     *
     * @param playerId The player's UUID
     */
    fun removeChatMode(playerId: UUID) {
        chatModes.remove(playerId)
        saveToStorage()
    }

    /**
     * Saves current state to storage asynchronously.
     */
    private fun saveToStorage() {
        val data = ChatModeData(modes = chatModes.toMap())
        storage.queueAsyncSave(data)
    }

    /**
     * Forces a synchronous save to storage.
     * Should only be called during plugin shutdown.
     */
    fun saveToDisk() {
        val data = ChatModeData(modes = chatModes.toMap())
        storage.saveToDisk(data)
    }
}
