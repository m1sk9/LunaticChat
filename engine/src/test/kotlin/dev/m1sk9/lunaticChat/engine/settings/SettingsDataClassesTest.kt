package dev.m1sk9.lunaticChat.engine.settings

import dev.m1sk9.lunaticChat.engine.chat.ChatMode
import dev.m1sk9.lunaticChat.engine.chat.ChatModeData
import dev.m1sk9.lunaticChat.engine.converter.CacheData
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsDataClassesTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val testUUID = UUID.fromString("00000001-0000-0000-0000-000000000000")
    private val testUUID2 = UUID.fromString("00000002-0000-0000-0000-000000000000")

    // --- PlayerChatSettings ---

    @Test
    fun `PlayerChatSettings default values should all be true`() {
        val settings = PlayerChatSettings(uuid = testUUID)
        assertTrue(settings.japaneseConversionEnabled)
        assertTrue(settings.directMessageNotificationEnabled)
        assertTrue(settings.channelMessageNotificationEnabled)
    }

    @Test
    fun `PlayerChatSettings serialization round-trip should preserve all fields`() {
        val original =
            PlayerChatSettings(
                uuid = testUUID,
                japaneseConversionEnabled = false,
                directMessageNotificationEnabled = true,
                channelMessageNotificationEnabled = false,
            )

        val serialized = json.encodeToString(PlayerChatSettings.serializer(), original)
        val deserialized = json.decodeFromString(PlayerChatSettings.serializer(), serialized)

        assertEquals(original.uuid, deserialized.uuid)
        assertEquals(original.japaneseConversionEnabled, deserialized.japaneseConversionEnabled)
        assertEquals(original.directMessageNotificationEnabled, deserialized.directMessageNotificationEnabled)
        assertEquals(original.channelMessageNotificationEnabled, deserialized.channelMessageNotificationEnabled)
    }

    @Test
    fun `PlayerChatSettings copy should allow flag toggle`() {
        val original = PlayerChatSettings(uuid = testUUID)
        val modified = original.copy(japaneseConversionEnabled = false)

        assertFalse(modified.japaneseConversionEnabled)
        assertTrue(modified.directMessageNotificationEnabled)
        assertTrue(modified.channelMessageNotificationEnabled)
        assertEquals(original.uuid, modified.uuid)
    }

    @Test
    fun `PlayerChatSettings copy should allow toggling all flags`() {
        val original = PlayerChatSettings(uuid = testUUID)
        val modified =
            original.copy(
                japaneseConversionEnabled = false,
                directMessageNotificationEnabled = false,
                channelMessageNotificationEnabled = false,
            )

        assertFalse(modified.japaneseConversionEnabled)
        assertFalse(modified.directMessageNotificationEnabled)
        assertFalse(modified.channelMessageNotificationEnabled)
    }

    // --- PlayerSettingsData ---

    @Test
    fun `PlayerSettingsData default values should be correct`() {
        val data = PlayerSettingsData()
        assertEquals(1, data.version)
        assertTrue(data.japaneseConversion.isEmpty())
        assertTrue(data.directMessageNotification.isEmpty())
        assertTrue(data.channelMessageNotification.isEmpty())
    }

    @Test
    fun `PlayerSettingsData serialization round-trip should preserve UUID keys`() {
        val original =
            PlayerSettingsData(
                version = 1,
                japaneseConversion = mapOf(testUUID to true, testUUID2 to false),
                directMessageNotification = mapOf(testUUID to false),
                channelMessageNotification = mapOf(testUUID2 to true),
            )

        val serialized = json.encodeToString(PlayerSettingsData.serializer(), original)
        val deserialized = json.decodeFromString(PlayerSettingsData.serializer(), serialized)

        assertEquals(original.version, deserialized.version)
        assertEquals(original.japaneseConversion, deserialized.japaneseConversion)
        assertEquals(original.directMessageNotification, deserialized.directMessageNotification)
        assertEquals(original.channelMessageNotification, deserialized.channelMessageNotification)
    }

    @Test
    fun `PlayerSettingsData serialized UUID keys should be string format`() {
        val data = PlayerSettingsData(japaneseConversion = mapOf(testUUID to true))
        val serialized = json.encodeToString(PlayerSettingsData.serializer(), data)
        assertTrue(serialized.contains(testUUID.toString()))
    }

    // --- CacheData ---

    @Test
    fun `CacheData should store version and entries`() {
        val data = CacheData(version = "1.0", entries = mapOf("hello" to "こんにちは"))
        assertEquals("1.0", data.version)
        assertEquals("こんにちは", data.entries["hello"])
    }

    @Test
    fun `CacheData serialization round-trip should preserve all fields`() {
        val original =
            CacheData(
                version = "2.0",
                entries = mapOf("hello" to "こんにちは", "world" to "世界"),
            )

        val serialized = json.encodeToString(CacheData.serializer(), original)
        val deserialized = json.decodeFromString(CacheData.serializer(), serialized)

        assertEquals(original.version, deserialized.version)
        assertEquals(original.entries, deserialized.entries)
    }

    // --- ChatModeData ---

    @Test
    fun `ChatModeData default should have empty modes map`() {
        val data = ChatModeData()
        assertTrue(data.modes.isEmpty())
    }

    @Test
    fun `ChatModeData serialization round-trip should preserve UUID-keyed map`() {
        val original =
            ChatModeData(
                modes =
                    mapOf(
                        testUUID to ChatMode.CHANNEL,
                        testUUID2 to ChatMode.GLOBAL,
                    ),
            )

        val serialized = json.encodeToString(ChatModeData.serializer(), original)
        val deserialized = json.decodeFromString(ChatModeData.serializer(), serialized)

        assertEquals(original.modes.size, deserialized.modes.size)
        assertEquals(ChatMode.CHANNEL, deserialized.modes[testUUID])
        assertEquals(ChatMode.GLOBAL, deserialized.modes[testUUID2])
    }

    @Test
    fun `ChatModeData serialized should contain UUID strings`() {
        val data = ChatModeData(modes = mapOf(testUUID to ChatMode.GLOBAL))
        val serialized = json.encodeToString(ChatModeData.serializer(), data)
        assertTrue(serialized.contains(testUUID.toString()))
    }
}
