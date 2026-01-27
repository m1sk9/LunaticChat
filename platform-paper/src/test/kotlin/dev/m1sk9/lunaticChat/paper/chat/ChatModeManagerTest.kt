package dev.m1sk9.lunaticChat.paper.chat

import dev.m1sk9.lunaticChat.engine.chat.ChatMode
import dev.m1sk9.lunaticChat.engine.chat.ChatModeData
import dev.m1sk9.lunaticChat.paper.TestUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for ChatModeManager.
 * Verifies chat mode management, toggling, and persistence.
 */
class ChatModeManagerTest {
    private fun createChatModeManager(
        initialData: ChatModeData = ChatModeData(),
    ): Triple<ChatModeManager, ChatModeStorage, TestUtils.TestLogger> {
        val logger = TestUtils.TestLogger()
        val storage = mockk<ChatModeStorage>(relaxed = true)

        every { storage.loadFromDisk() } returns initialData

        val manager = ChatModeManager(storage, logger)
        return Triple(manager, storage, logger)
    }

    @Test
    fun `initialize should load data from storage`() {
        val playerId = UUID.randomUUID()
        val initialData = ChatModeData(modes = mapOf(playerId to ChatMode.CHANNEL))
        val (manager, _, logger) = createChatModeManager(initialData)

        manager.initialize()

        assertEquals(ChatMode.CHANNEL, manager.getChatMode(playerId))
        assert(logger.infoMessages.any { it.contains("ChatModeManager initialized with 1 saved modes") })
    }

    @Test
    fun `getChatMode should return DEFAULT for unknown player`() {
        val (manager, _, _) = createChatModeManager()
        manager.initialize()

        val playerId = UUID.randomUUID()
        val mode = manager.getChatMode(playerId)

        assertEquals(ChatMode.DEFAULT, mode)
        assertEquals(ChatMode.GLOBAL, mode) // DEFAULT is GLOBAL
    }

    @Test
    fun `getChatMode should return set mode`() {
        val (manager, _, _) = createChatModeManager()
        manager.initialize()

        val playerId = UUID.randomUUID()
        manager.setChatMode(playerId, ChatMode.CHANNEL)

        assertEquals(ChatMode.CHANNEL, manager.getChatMode(playerId))
    }

    @Test
    fun `setChatMode should save to storage`() {
        val (manager, storage, _) = createChatModeManager()
        manager.initialize()

        val playerId = UUID.randomUUID()
        manager.setChatMode(playerId, ChatMode.CHANNEL)

        verify(exactly = 1) { storage.queueAsyncSave(any()) }
    }

    @Test
    fun `toggleChatMode should switch from GLOBAL to CHANNEL`() {
        val (manager, _, _) = createChatModeManager()
        manager.initialize()

        val playerId = UUID.randomUUID()
        // Initial mode is GLOBAL (default)
        assertEquals(ChatMode.GLOBAL, manager.getChatMode(playerId))

        val newMode = manager.toggleChatMode(playerId)

        assertEquals(ChatMode.CHANNEL, newMode)
        assertEquals(ChatMode.CHANNEL, manager.getChatMode(playerId))
    }

    @Test
    fun `toggleChatMode should switch from CHANNEL to GLOBAL`() {
        val (manager, _, _) = createChatModeManager()
        manager.initialize()

        val playerId = UUID.randomUUID()
        manager.setChatMode(playerId, ChatMode.CHANNEL)

        val newMode = manager.toggleChatMode(playerId)

        assertEquals(ChatMode.GLOBAL, newMode)
        assertEquals(ChatMode.GLOBAL, manager.getChatMode(playerId))
    }

    @Test
    fun `toggleChatMode should persist changes`() {
        val (manager, storage, _) = createChatModeManager()
        manager.initialize()

        val playerId = UUID.randomUUID()
        manager.toggleChatMode(playerId)

        verify(atLeast = 1) { storage.queueAsyncSave(any()) }
    }

    @Test
    fun `removeChatMode should revert to default`() {
        val (manager, _, _) = createChatModeManager()
        manager.initialize()

        val playerId = UUID.randomUUID()
        manager.setChatMode(playerId, ChatMode.CHANNEL)
        assertEquals(ChatMode.CHANNEL, manager.getChatMode(playerId))

        manager.removeChatMode(playerId)

        assertEquals(ChatMode.GLOBAL, manager.getChatMode(playerId))
    }

    @Test
    fun `removeChatMode should save to storage`() {
        val (manager, storage, _) = createChatModeManager()
        manager.initialize()

        val playerId = UUID.randomUUID()
        manager.setChatMode(playerId, ChatMode.CHANNEL)
        manager.removeChatMode(playerId)

        verify(atLeast = 2) { storage.queueAsyncSave(any()) } // Once for set, once for remove
    }

    @Test
    fun `saveToDisk should synchronously save`() {
        val (manager, storage, _) = createChatModeManager()
        manager.initialize()

        val playerId = UUID.randomUUID()
        manager.setChatMode(playerId, ChatMode.CHANNEL)
        manager.saveToDisk()

        verify { storage.saveToDisk(any()) }
    }

    @Test
    fun `shutdown should save and shutdown storage`() {
        val (manager, storage, _) = createChatModeManager()
        manager.initialize()

        manager.shutdown()

        verify { storage.saveToDisk(any()) }
        verify { storage.shutdown() }
    }

    @Test
    fun `manager should handle multiple players independently`() {
        val (manager, _, _) = createChatModeManager()
        manager.initialize()

        val player1 = UUID.randomUUID()
        val player2 = UUID.randomUUID()

        manager.setChatMode(player1, ChatMode.CHANNEL)
        manager.setChatMode(player2, ChatMode.GLOBAL)

        assertEquals(ChatMode.CHANNEL, manager.getChatMode(player1))
        assertEquals(ChatMode.GLOBAL, manager.getChatMode(player2))
    }

    @Test
    fun `manager should support rapid mode changes`() {
        val (manager, storage, _) = createChatModeManager()
        manager.initialize()

        val playerId = UUID.randomUUID()

        // Toggle multiple times
        manager.toggleChatMode(playerId) // GLOBAL -> CHANNEL
        manager.toggleChatMode(playerId) // CHANNEL -> GLOBAL
        manager.toggleChatMode(playerId) // GLOBAL -> CHANNEL

        assertEquals(ChatMode.CHANNEL, manager.getChatMode(playerId))
        verify(atLeast = 3) { storage.queueAsyncSave(any()) }
    }

    @Test
    fun `manager should handle empty initial data`() {
        val (manager, _, logger) = createChatModeManager(ChatModeData())
        manager.initialize()

        assert(logger.infoMessages.any { it.contains("ChatModeManager initialized with 0 saved modes") })
    }

    @Test
    fun `manager should restore modes after initialization`() {
        val player1 = TestUtils.createTestUUID(1)
        val player2 = TestUtils.createTestUUID(2)
        val initialData =
            ChatModeData(
                modes =
                    mapOf(
                        player1 to ChatMode.CHANNEL,
                        player2 to ChatMode.GLOBAL,
                    ),
            )

        val (manager, _, _) = createChatModeManager(initialData)
        manager.initialize()

        assertEquals(ChatMode.CHANNEL, manager.getChatMode(player1))
        assertEquals(ChatMode.GLOBAL, manager.getChatMode(player2))
    }
}
