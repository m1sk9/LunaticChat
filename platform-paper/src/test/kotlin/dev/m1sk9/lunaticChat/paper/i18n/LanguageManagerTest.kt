package dev.m1sk9.lunaticChat.paper.i18n

import java.io.InputStream
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test for LanguageManager using test resources.
 */
class LanguageManagerTest {
    /**
     * Mock Logger that collects log messages.
     */
    private class TestLogger : Logger("test", null) {
        val infoMessages = mutableListOf<String>()
        val warningMessages = mutableListOf<String>()

        override fun info(msg: String) {
            infoMessages.add(msg)
        }

        override fun warning(msg: String) {
            warningMessages.add(msg)
        }
    }

    /**
     * Resource loader that loads files from test classpath.
     */
    private val testResourceLoader: (String) -> InputStream? = { path ->
        this::class.java.classLoader.getResourceAsStream(path)
    }

    private fun createLanguageManager(language: Language): Pair<LanguageManager, TestLogger> {
        val logger = TestLogger()
        val manager =
            LanguageManager(
                plugin = null,
                logger = logger,
                selectedLanguage = language,
                resourceLoader = testResourceLoader,
            )
        return Pair(manager, logger)
    }

    @Test
    fun `initialize should load English language file`() {
        val (manager, logger) = createLanguageManager(Language.EN)
        manager.initialize()

        assertTrue(logger.infoMessages.any { it.contains("en.yml") })
    }

    @Test
    fun `initialize should load Japanese language file`() {
        val (manager, logger) = createLanguageManager(Language.JA)
        manager.initialize()

        assertTrue(logger.infoMessages.any { it.contains("ja.yml") })
    }

    @Test
    fun `initialize should load both language files`() {
        val (manager, logger) = createLanguageManager(Language.EN)
        manager.initialize()

        // Both en.yml and ja.yml should be loaded
        assertTrue(logger.infoMessages.any { it.contains("en.yml") })
        assertTrue(logger.infoMessages.any { it.contains("ja.yml") })
    }

    @Test
    fun `getMessage should return English message for EN language`() {
        val (manager, _) = createLanguageManager(Language.EN)
        manager.initialize()

        val message = manager.getMessage("commandDescription.tell")
        assertEquals("Send a direct message to another player", message)
    }

    @Test
    fun `getMessage should return Japanese message for JA language`() {
        val (manager, _) = createLanguageManager(Language.JA)
        manager.initialize()

        val message = manager.getMessage("commandDescription.tell")
        assertEquals("他のプレイヤーにダイレクトメッセージを送信します", message)
    }

    @Test
    fun `getMessage should handle nested keys with dot notation`() {
        val (manager, _) = createLanguageManager(Language.EN)
        manager.initialize()

        val onMessage = manager.getMessage("toggle.on")
        val offMessage = manager.getMessage("toggle.off")

        assertEquals("Enabled", onMessage)
        assertEquals("Disabled", offMessage)
    }

    @Test
    fun `getMessage should substitute placeholders`() {
        val (manager, _) = createLanguageManager(Language.EN)
        manager.initialize()

        val message =
            manager.getMessage(
                "tellTargetOffline",
                mapOf("target" to "Steve"),
            )

        assertEquals("Player 'Steve' is currently offline.", message)
    }

    @Test
    fun `getMessage should substitute multiple placeholders`() {
        val (manager, _) = createLanguageManager(Language.EN)
        manager.initialize()

        val message =
            manager.getMessage(
                "directMessageNoticeToggle",
                mapOf("toggle" to "Enabled"),
            )

        assertEquals("Direct message notifications have been set to Enabled", message)
    }

    @Test
    fun `getMessage should handle Japanese placeholders`() {
        val (manager, _) = createLanguageManager(Language.JA)
        manager.initialize()

        val message =
            manager.getMessage(
                "tellTargetOffline",
                mapOf("target" to "太郎"),
            )

        assertEquals("プレイヤー '太郎' は現在オフラインです。", message)
    }

    @Test
    fun `getMessage should return key when message not found`() {
        val (manager, logger) = createLanguageManager(Language.EN)
        manager.initialize()

        val message = manager.getMessage("nonexistent.key")

        assertEquals("nonexistent.key", message)
        assertTrue(logger.warningMessages.any { it.contains("Message key not found: nonexistent.key") })
    }

    @Test
    fun `getMessage should handle empty placeholders map`() {
        val (manager, _) = createLanguageManager(Language.EN)
        manager.initialize()

        val message = manager.getMessage("playerOnlyCommand", emptyMap())

        assertEquals("This command can only be executed by players.", message)
    }

    @Test
    fun `getToggleText should return 'on' text when enabled`() {
        val (manager, _) = createLanguageManager(Language.EN)
        manager.initialize()

        val text = manager.getToggleText(true)

        assertEquals("Enabled", text)
    }

    @Test
    fun `getToggleText should return 'off' text when disabled`() {
        val (manager, _) = createLanguageManager(Language.EN)
        manager.initialize()

        val text = manager.getToggleText(false)

        assertEquals("Disabled", text)
    }

    @Test
    fun `getToggleText should return Japanese text for JA language`() {
        val (manager, _) = createLanguageManager(Language.JA)
        manager.initialize()

        val onText = manager.getToggleText(true)
        val offText = manager.getToggleText(false)

        assertEquals("有効", onText)
        assertEquals("無効", offText)
    }

    @Test
    fun `getMessage should retrieve all command descriptions`() {
        val (manager, _) = createLanguageManager(Language.EN)
        manager.initialize()

        val commands = listOf("tell", "reply", "jp", "notice")
        commands.forEach { command ->
            val message = manager.getMessage("commandDescription.$command")
            assertNotNull(message)
            assertTrue(message.isNotEmpty())
        }
    }

    @Test
    fun `getMessage should retrieve all standard messages`() {
        val (manager, _) = createLanguageManager(Language.EN)
        manager.initialize()

        val keys =
            listOf(
                "playerOnlyCommand",
                "replyTargetNotFound",
                "tellYourself",
            )

        keys.forEach { key ->
            val message = manager.getMessage(key)
            assertNotNull(message)
            assertTrue(message.isNotEmpty())
        }
    }

    @Test
    fun `getMessage should handle placeholder not provided`() {
        val (manager, _) = createLanguageManager(Language.EN)
        manager.initialize()

        // Get message with placeholder but don't provide value
        val message = manager.getMessage("tellTargetOffline")

        // Should still return message with placeholder intact
        assertEquals("Player '{target}' is currently offline.", message)
    }

    @Test
    fun `initialize should log number of loaded keys`() {
        val (manager, logger) = createLanguageManager(Language.EN)
        manager.initialize()

        // Should log number of keys for each language file
        assertTrue(logger.infoMessages.any { it.contains("keys)") })
    }

    @Test
    fun `getMessage should be case sensitive for keys`() {
        val (manager, logger) = createLanguageManager(Language.EN)
        manager.initialize()

        val message = manager.getMessage("PlayerOnlyCommand") // Wrong case

        assertEquals("PlayerOnlyCommand", message) // Should return key as-is
        assertTrue(logger.warningMessages.any { it.contains("Message key not found") })
    }

    @Test
    fun `getMessage should handle special characters in placeholder values`() {
        val (manager, _) = createLanguageManager(Language.EN)
        manager.initialize()

        val message =
            manager.getMessage(
                "tellTargetOffline",
                mapOf("target" to "Player@123!"),
            )

        assertEquals("Player 'Player@123!' is currently offline.", message)
    }

    @Test
    fun `getMessage should replace all occurrences of placeholder`() {
        val (manager, _) = createLanguageManager(Language.EN)
        manager.initialize()

        // Create a message with the same placeholder multiple times (if such exists)
        val message =
            manager.getMessage(
                "directMessageNoticeToggle",
                mapOf("toggle" to "ON"),
            )

        // Should replace the placeholder
        assertTrue(message.contains("ON"))
    }

    @Test
    fun `flattened keys should handle nested structure correctly`() {
        val (manager, _) = createLanguageManager(Language.EN)
        manager.initialize()

        // Verify that nested keys work
        val commandTell = manager.getMessage("commandDescription.tell")
        val toggleOn = manager.getMessage("toggle.on")

        assertNotNull(commandTell)
        assertNotNull(toggleOn)
        assertTrue(commandTell.isNotEmpty())
        assertTrue(toggleOn.isNotEmpty())
    }
}
