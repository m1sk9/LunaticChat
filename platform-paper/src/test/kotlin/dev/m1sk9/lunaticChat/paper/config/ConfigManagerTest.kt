package dev.m1sk9.lunaticChat.paper.config

import dev.m1sk9.lunaticChat.paper.i18n.Language
import io.mockk.every
import io.mockk.mockk
import org.bukkit.configuration.file.FileConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for ConfigManager to ensure proper configuration loading and parsing.
 */
class ConfigManagerTest {
    private fun createMockConfig(values: Map<String, Any> = emptyMap()): FileConfiguration {
        val config = mockk<FileConfiguration>(relaxed = true)

        // Set up default values
        every { config.getBoolean(any(), any()) } answers {
            val key = firstArg<String>()
            val default = secondArg<Boolean>()
            (values[key] as? Boolean) ?: default
        }

        every { config.getInt(any(), any()) } answers {
            val key = firstArg<String>()
            val default = secondArg<Int>()
            (values[key] as? Int) ?: default
        }

        every { config.getLong(any(), any()) } answers {
            val key = firstArg<String>()
            val default = secondArg<Long>()
            (values[key] as? Long) ?: default
        }

        every { config.getString(any(), any()) } answers {
            val key = firstArg<String>()
            val default = secondArg<String>()
            (values[key] as? String) ?: default
        }

        every { config.getString(any()) } answers {
            val key = firstArg<String>()
            values[key] as? String
        }

        return config
    }

    @Test
    fun `loadConfiguration should load all default values`() {
        val configManager = ConfigManager()
        val mockConfig = createMockConfig()

        val configuration = configManager.loadConfiguration(mockConfig)

        assertNotNull(configuration)
        assertTrue(configuration.features.quickReplies.enabled)
        assertFalse(configuration.features.japaneseConversion.enabled)
        assertFalse(configuration.features.channelChat.enabled)
        assertFalse(configuration.debug)
        assertFalse(configuration.checkForUpdates)
        assertEquals(Language.EN, configuration.language)
    }

    @Test
    fun `loadConfiguration should load quick replies configuration`() {
        val configManager = ConfigManager()
        val mockConfig =
            createMockConfig(
                mapOf(
                    "features.quickReplies.enabled" to false,
                ),
            )

        val configuration = configManager.loadConfiguration(mockConfig)

        assertFalse(configuration.features.quickReplies.enabled)
    }

    @Test
    fun `loadConfiguration should load Japanese conversion configuration`() {
        val configManager = ConfigManager()
        val mockConfig =
            createMockConfig(
                mapOf(
                    "features.japaneseConversion.enabled" to true,
                    "features.japaneseConversion.cache.maxEntries" to 1000,
                    "features.japaneseConversion.cache.saveIntervalSeconds" to 600,
                    "features.japaneseConversion.cache.filePath" to "custom_cache.json",
                    "features.japaneseConversion.api.timeout" to 5000L,
                    "features.japaneseConversion.api.retryAttempts" to 3,
                ),
            )

        val configuration = configManager.loadConfiguration(mockConfig)

        assertTrue(configuration.features.japaneseConversion.enabled)
        assertEquals(1000, configuration.features.japaneseConversion.cacheMaxEntries)
        assertEquals(600, configuration.features.japaneseConversion.cacheSaveIntervalSeconds)
        assertEquals("custom_cache.json", configuration.features.japaneseConversion.cacheFilePath)
        assertEquals(5000L, configuration.features.japaneseConversion.apiTimeout)
        assertEquals(3, configuration.features.japaneseConversion.apiRetryAttempts)
    }

    @Test
    fun `loadConfiguration should load channel chat configuration`() {
        val configManager = ConfigManager()
        val mockConfig =
            createMockConfig(
                mapOf(
                    "features.channelChat.enabled" to true,
                    "features.channelChat.maxChannelsPerServer" to 20,
                    "features.channelChat.maxMembersPerChannel" to 100,
                    "features.channelChat.maxMembershipPerPlayer" to 10,
                ),
            )

        val configuration = configManager.loadConfiguration(mockConfig)

        assertTrue(configuration.features.channelChat.enabled)
        assertEquals(20, configuration.features.channelChat.maxChannelsPerServer)
        assertEquals(100, configuration.features.channelChat.maxMembersPerChannel)
        assertEquals(10, configuration.features.channelChat.maxMembershipPerPlayer)
    }

    @Test
    fun `loadConfiguration should load message format configuration`() {
        val configManager = ConfigManager()
        val customDMFormat = "DM: {sender} -> {recipient}: {message}"
        val customChannelFormat = "[{channel}] {sender}: {message}"
        val mockConfig =
            createMockConfig(
                mapOf(
                    "messageFormat.directMessageFormat" to customDMFormat,
                    "messageFormat.channelMessageFormat" to customChannelFormat,
                ),
            )

        val configuration = configManager.loadConfiguration(mockConfig)

        assertEquals(customDMFormat, configuration.messageFormat.directMessageFormat)
        assertEquals(customChannelFormat, configuration.messageFormat.channelMessageFormat)
    }

    @Test
    fun `loadConfiguration should load debug and update check settings`() {
        val configManager = ConfigManager()
        val mockConfig =
            createMockConfig(
                mapOf(
                    "debug" to true,
                    "checkForUpdates" to true,
                ),
            )

        val configuration = configManager.loadConfiguration(mockConfig)

        assertTrue(configuration.debug)
        assertTrue(configuration.checkForUpdates)
    }

    @Test
    fun `loadConfiguration should load custom settings file path`() {
        val configManager = ConfigManager()
        val customPath = "custom-player-settings.yaml"
        val mockConfig =
            createMockConfig(
                mapOf(
                    "userSettingsFilePath" to customPath,
                ),
            )

        val configuration = configManager.loadConfiguration(mockConfig)

        assertEquals(customPath, configuration.userSettingsFilePath)
    }

    @Test
    fun `loadConfiguration should load Japanese language`() {
        val configManager = ConfigManager()
        val mockConfig =
            createMockConfig(
                mapOf(
                    "language" to "ja",
                ),
            )

        val configuration = configManager.loadConfiguration(mockConfig)

        assertEquals(Language.JA, configuration.language)
    }

    @Test
    fun `loadConfiguration should handle unknown language code`() {
        val configManager = ConfigManager()
        val mockConfig =
            createMockConfig(
                mapOf(
                    "language" to "fr", // French not supported
                ),
            )

        val configuration = configManager.loadConfiguration(mockConfig)

        // Should fall back to English
        assertEquals(Language.EN, configuration.language)
    }

    @Test
    fun `loadConfiguration should use default values when keys are missing`() {
        val configManager = ConfigManager()
        val mockConfig = createMockConfig(emptyMap())

        val configuration = configManager.loadConfiguration(mockConfig)

        // All features should have their defaults
        assertTrue(configuration.features.quickReplies.enabled)
        assertEquals(500, configuration.features.japaneseConversion.cacheMaxEntries)
        assertEquals(300, configuration.features.japaneseConversion.cacheSaveIntervalSeconds)
        assertEquals("conversion_cache.json", configuration.features.japaneseConversion.cacheFilePath)
        assertEquals(3000L, configuration.features.japaneseConversion.apiTimeout)
        assertEquals(2, configuration.features.japaneseConversion.apiRetryAttempts)
        assertEquals(0, configuration.features.channelChat.maxChannelsPerServer)
        assertEquals(0, configuration.features.channelChat.maxMembersPerChannel)
        assertEquals(0, configuration.features.channelChat.maxMembershipPerPlayer)
    }

    @Test
    fun `loadConfiguration can be called multiple times`() {
        val configManager = ConfigManager()
        val mockConfig1 =
            createMockConfig(
                mapOf("debug" to true),
            )
        val mockConfig2 =
            createMockConfig(
                mapOf("debug" to false),
            )

        val config1 = configManager.loadConfiguration(mockConfig1)
        val config2 = configManager.loadConfiguration(mockConfig2)

        assertTrue(config1.debug)
        assertFalse(config2.debug)
    }

    @Test
    fun `loadConfiguration should handle all features enabled`() {
        val configManager = ConfigManager()
        val mockConfig =
            createMockConfig(
                mapOf(
                    "features.quickReplies.enabled" to true,
                    "features.japaneseConversion.enabled" to true,
                    "features.channelChat.enabled" to true,
                ),
            )

        val configuration = configManager.loadConfiguration(mockConfig)

        assertTrue(configuration.features.quickReplies.enabled)
        assertTrue(configuration.features.japaneseConversion.enabled)
        assertTrue(configuration.features.channelChat.enabled)
    }

    @Test
    fun `loadConfiguration should handle all features disabled`() {
        val configManager = ConfigManager()
        val mockConfig =
            createMockConfig(
                mapOf(
                    "features.quickReplies.enabled" to false,
                    "features.japaneseConversion.enabled" to false,
                    "features.channelChat.enabled" to false,
                ),
            )

        val configuration = configManager.loadConfiguration(mockConfig)

        assertFalse(configuration.features.quickReplies.enabled)
        assertFalse(configuration.features.japaneseConversion.enabled)
        assertFalse(configuration.features.channelChat.enabled)
    }
}
