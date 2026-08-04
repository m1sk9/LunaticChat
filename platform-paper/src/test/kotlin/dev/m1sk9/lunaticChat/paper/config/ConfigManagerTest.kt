package dev.m1sk9.lunaticChat.paper.config

import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.i18n.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for ConfigManager.
 *
 * These parse real YAML rather than a mocked Bukkit FileConfiguration, so they exercise the same
 * path the plugin uses at startup - including what happens to a file that is incomplete, stale or
 * malformed.
 */
class ConfigManagerTest {
    private fun load(yaml: String) = ConfigManager(TestUtils.TestLogger()).loadConfiguration(yaml)

    /** The file shipped in resources, which is what a fresh install actually reads. */
    private val bundledConfig: String =
        checkNotNull(javaClass.classLoader.getResourceAsStream("config.yml")) {
            "config.yml missing from resources"
        }.bufferedReader().use { it.readText() }

    @Test
    fun `the bundled config parses`() {
        val config = load(bundledConfig)

        assertFalse(config.debug)
        assertEquals("player-settings.yaml", config.userSettingsFilePath)
        assertEquals(Language.EN, config.language)
    }

    @Test
    fun `the bundled config agrees with the declared defaults`() {
        // The data class is meant to be the single source of every default. If config.yml ships a
        // different value for a key, one of the two is lying to the operator.
        assertEquals(LunaticChatConfiguration(), load(bundledConfig))
    }

    @Test
    fun `an empty document falls back to defaults`() {
        assertEquals(LunaticChatConfiguration(), load("{}"))
    }

    @Test
    fun `a config missing a section keeps that section's defaults`() {
        val config = load("debug: true")

        assertTrue(config.debug)
        assertEquals(LunaticChatConfiguration().features, config.features)
        assertEquals(LunaticChatConfiguration().messageFormat, config.messageFormat)
    }

    @Test
    fun `values present in the file win over the defaults`() {
        val config =
            load(
                """
                debug: true
                checkForUpdates: false
                userSettingsFilePath: "custom.yaml"
                language: "ja"
                """.trimIndent(),
            )

        assertTrue(config.debug)
        assertFalse(config.checkForUpdates)
        assertEquals("custom.yaml", config.userSettingsFilePath)
        assertEquals(Language.JA, config.language)
    }

    @Test
    fun `an unknown language code falls back to English`() {
        assertEquals(Language.EN, load("""language: "kl"""").language)
    }

    @Test
    fun `a language code is matched regardless of case`() {
        assertEquals(Language.JA, load("""language: "JA"""").language)
    }

    @Test
    fun `nested japanese conversion settings are read`() {
        val config =
            load(
                """
                features:
                  japaneseConversion:
                    enabled: true
                    cache:
                      maxEntries: 1000
                      saveIntervalSeconds: 600
                      filePath: "custom_cache.json"
                    api:
                      timeout: 5000
                """.trimIndent(),
            )

        val japanese = config.features.japaneseConversion
        assertTrue(japanese.enabled)
        assertEquals(1000, japanese.cache.maxEntries)
        assertEquals(600, japanese.cache.saveIntervalSeconds)
        assertEquals("custom_cache.json", japanese.cache.filePath)
        assertEquals(5000L, japanese.api.timeout)
    }

    @Test
    fun `channel message logging is read from the file`() {
        // This block was documented in config.yml but never parsed, so editing it did nothing.
        val config =
            load(
                """
                features:
                  channelChat:
                    enabled: true
                    messageLogging:
                      enabled: false
                      retentionDays: 7
                      maxFileSizeMB: 20
                """.trimIndent(),
            )

        val logging = config.features.channelChat.messageLogging
        assertFalse(logging.enabled)
        assertEquals(7, logging.retentionDays)
        assertEquals(20, logging.maxFileSizeMB)
    }

    @Test
    fun `velocity integration settings are read from the file`() {
        val config =
            load(
                """
                features:
                  velocityIntegration:
                    enabled: true
                    crossServerGlobalChat: true
                    crossServerDirectMessage: true
                    serverName: "survival"
                    messageDeduplicationCacheSize: 250
                """.trimIndent(),
            )

        val velocity = config.features.velocityIntegration
        assertTrue(velocity.enabled)
        assertTrue(velocity.crossServerGlobalChat)
        assertTrue(velocity.crossServerDirectMessage)
        assertEquals("survival", velocity.serverName)
        assertEquals(250, velocity.messageDeduplicationCacheSize)
    }

    @Test
    fun `message formats are read from the file`() {
        val config =
            load(
                """
                messageFormat:
                  directMessageFormat: "DM {sender} {message}"
                  channelMessageFormat: "CH {channel} {message}"
                  crossServerGlobalChatFormat: "GL {server} {message}"
                """.trimIndent(),
            )

        assertEquals("DM {sender} {message}", config.messageFormat.directMessageFormat)
        assertEquals("CH {channel} {message}", config.messageFormat.channelMessageFormat)
        assertEquals("GL {server} {message}", config.messageFormat.crossServerGlobalChatFormat)
    }

    @Test
    fun `a key this build no longer knows is ignored`() {
        // An operator upgrading from a build that had extra keys must still be able to start.
        val config = load("debug: true\nsomeRetiredOption: 42")

        assertTrue(config.debug)
    }

    @Test
    fun `a malformed file falls back to defaults instead of failing startup`() {
        assertEquals(LunaticChatConfiguration(), load("features: [this is not a map"))
    }

    @Test
    fun `a document that is not YAML at all says that every setting was reset`() {
        val logger = TestUtils.TestLogger()

        val config = ConfigManager(logger).loadConfiguration("features: [this is not a map")

        assertEquals(LunaticChatConfiguration(), config)
        assertTrue(logger.severeMessages.any { it.contains("EVERY setting") })
    }

    @Test
    fun `a file holding only comments is not reported as a failure`() {
        val logger = TestUtils.TestLogger()

        val config = ConfigManager(logger).loadConfiguration("# everything left at its default\n")

        assertEquals(LunaticChatConfiguration(), config)
        assertTrue(logger.severeMessages.isEmpty())
    }

    @Test
    fun `the boolean spellings Bukkit accepted are still booleans`() {
        // Bukkit read config.yml as YAML 1.1, where these are booleans. A file written against that
        // must keep meaning what it says.
        val config = load("debug: yes\ncheckForUpdates: off")

        assertTrue(config.debug)
        assertFalse(config.checkForUpdates)
    }

    @Test
    fun `boolean spellings are matched regardless of case`() {
        assertTrue(load("debug: YES").debug)
    }

    @Test
    fun `an unreadable setting falls back alone and leaves the rest of the file standing`() {
        val logger = TestUtils.TestLogger()

        val config =
            ConfigManager(logger).loadConfiguration(
                """
                debug: perhaps
                userSettingsFilePath: "custom.yaml"
                language: "ja"
                """.trimIndent(),
            )

        assertFalse(config.debug)
        assertEquals("custom.yaml", config.userSettingsFilePath)
        assertEquals(Language.JA, config.language)
        assertTrue(logger.warningMessages.any { it.contains("debug") })
        assertTrue(logger.severeMessages.isEmpty())
    }

    @Test
    fun `an unreadable nested setting leaves its siblings standing`() {
        val logger = TestUtils.TestLogger()

        val config =
            ConfigManager(logger).loadConfiguration(
                """
                features:
                  velocityIntegration:
                    enabled: true
                    serverName: "survival"
                    messageDeduplicationCacheSize: "not a number"
                """.trimIndent(),
            )

        val velocity = config.features.velocityIntegration
        assertTrue(velocity.enabled)
        assertEquals("survival", velocity.serverName)
        assertEquals(
            LunaticChatConfiguration().features.velocityIntegration.messageDeduplicationCacheSize,
            velocity.messageDeduplicationCacheSize,
        )
        assertTrue(logger.warningMessages.any { it.contains("features.velocityIntegration.messageDeduplicationCacheSize") })
    }

    @Test
    fun `several unreadable settings each fall back without taking the others`() {
        val config =
            load(
                """
                debug: perhaps
                checkForUpdates: sometimes
                userSettingsFilePath: "custom.yaml"
                """.trimIndent(),
            )

        assertFalse(config.debug)
        assertTrue(config.checkForUpdates)
        assertEquals("custom.yaml", config.userSettingsFilePath)
    }
}
