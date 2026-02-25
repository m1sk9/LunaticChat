package dev.m1sk9.lunaticChat.paper.settings

import dev.m1sk9.lunaticChat.engine.settings.PlayerChatSettings
import dev.m1sk9.lunaticChat.engine.settings.PlayerSettingsData
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.TestUtils.createTestUUID
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerSettingsManagerTest {
    private fun createManager(
        initialData: PlayerSettingsData = PlayerSettingsData(),
    ): Triple<PlayerSettingsManager, YamlPlayerSettingsStorage, TestUtils.TestLogger> {
        val logger = TestUtils.TestLogger()
        val storage = mockk<YamlPlayerSettingsStorage>(relaxed = true)

        every { storage.loadFromDisk() } returns initialData

        val manager = PlayerSettingsManager(storage, logger)
        return Triple(manager, storage, logger)
    }

    @Test
    fun `initialize should load settings from storage`() {
        val playerId = createTestUUID(1)
        val data =
            PlayerSettingsData(
                japaneseConversion = mapOf(playerId to false),
                directMessageNotification = mapOf(playerId to false),
                channelMessageNotification = mapOf(playerId to false),
            )

        val (manager, _, logger) = createManager(initialData = data)
        manager.initialize()

        val settings = manager.getSettings(playerId)
        assertFalse(settings.japaneseConversionEnabled)
        assertFalse(settings.directMessageNotificationEnabled)
        assertFalse(settings.channelMessageNotificationEnabled)
        assertTrue(logger.infoMessages.any { it.contains("1 players") })
    }

    @Test
    fun `getSettings should return defaults for unknown player`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val settings = manager.getSettings(createTestUUID(99))

        assertTrue(settings.japaneseConversionEnabled)
        assertTrue(settings.directMessageNotificationEnabled)
        assertTrue(settings.channelMessageNotificationEnabled)
    }

    @Test
    fun `updateSettings should update cache and queue save`() {
        val (manager, storage, _) = createManager()
        manager.initialize()

        val playerId = createTestUUID(1)
        val settings =
            PlayerChatSettings(
                uuid = playerId,
                japaneseConversionEnabled = false,
                directMessageNotificationEnabled = false,
                channelMessageNotificationEnabled = true,
            )

        manager.updateSettings(settings)

        val retrieved = manager.getSettings(playerId)
        assertFalse(retrieved.japaneseConversionEnabled)
        assertFalse(retrieved.directMessageNotificationEnabled)
        assertTrue(retrieved.channelMessageNotificationEnabled)

        verify(exactly = 1) { storage.queueAsyncSave(any()) }
    }

    @Test
    fun `updateSettings should overwrite existing settings`() {
        val playerId = createTestUUID(1)
        val data =
            PlayerSettingsData(
                japaneseConversion = mapOf(playerId to true),
                directMessageNotification = mapOf(playerId to true),
                channelMessageNotification = mapOf(playerId to true),
            )

        val (manager, _, _) = createManager(initialData = data)
        manager.initialize()

        manager.updateSettings(
            PlayerChatSettings(
                uuid = playerId,
                japaneseConversionEnabled = false,
                directMessageNotificationEnabled = false,
                channelMessageNotificationEnabled = false,
            ),
        )

        val settings = manager.getSettings(playerId)
        assertFalse(settings.japaneseConversionEnabled)
        assertFalse(settings.directMessageNotificationEnabled)
        assertFalse(settings.channelMessageNotificationEnabled)
    }

    @Test
    fun `getSettings should return correct UUID`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val playerId = createTestUUID(1)
        val settings = manager.getSettings(playerId)

        assertEquals(playerId, settings.uuid)
    }

    @Test
    fun `saveToDisk should call storage saveToDisk`() {
        val (manager, storage, _) = createManager()
        manager.initialize()

        manager.saveToDisk()

        verify(exactly = 1) { storage.saveToDisk(any()) }
    }

    @Test
    fun `multiple players should have independent settings`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val player1 = createTestUUID(1)
        val player2 = createTestUUID(2)

        manager.updateSettings(
            PlayerChatSettings(uuid = player1, japaneseConversionEnabled = false),
        )
        manager.updateSettings(
            PlayerChatSettings(uuid = player2, japaneseConversionEnabled = true),
        )

        assertFalse(manager.getSettings(player1).japaneseConversionEnabled)
        assertTrue(manager.getSettings(player2).japaneseConversionEnabled)
    }

    @Test
    fun `initialize with empty data should work`() {
        val (manager, _, logger) = createManager(PlayerSettingsData())
        manager.initialize()

        assertTrue(logger.infoMessages.any { it.contains("0 players") })
    }
}
