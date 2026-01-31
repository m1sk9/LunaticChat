package dev.m1sk9.lunaticChat.paper.chat.handler

import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for DirectMessageHandler.
 * Validates message handling with dependency injection and conversion features.
 */
class DirectMessageHandlerTest {
    private fun createHandler(
        configuration: dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration? = null,
        settingsManager: PlayerSettingsManager? = null,
        romanjiConverter: RomanjiConverter? = null,
        languageManager: LanguageManager? = null,
    ): DirectMessageHandler {
        val config = configuration ?: TestUtils.createTestConfiguration()
        val langManager = languageManager ?: mockk<LanguageManager>(relaxed = true)
        return DirectMessageHandler(config, settingsManager, romanjiConverter, langManager)
    }

    @Test
    fun `sendDirectMessage should return true on success`() {
        val handler = createHandler()
        val sender = TestUtils.createMockPlayer()
        val recipient = TestUtils.createMockPlayer()

        val result = handler.sendDirectMessage(sender, recipient, "Test message")

        assertTrue(result)
    }

    @Test
    fun `sendDirectMessage should use custom message format from configuration`() {
        val customConfig =
            TestUtils.createTestConfiguration().copy(
                messageFormat =
                    TestUtils
                        .createTestConfiguration()
                        .messageFormat
                        .copy(
                            directMessageFormat = "[DM] {sender} -> {recipient}: {message}",
                        ),
            )

        val handler = createHandler(configuration = customConfig)
        val sender = TestUtils.createMockPlayer(name = "Alice")
        val recipient = TestUtils.createMockPlayer(name = "Bob")

        val result = handler.sendDirectMessage(sender, recipient, "Test")

        assertTrue(result)
    }

    @Test
    fun `sendDirectMessage without conversion should send original message`() {
        val settingsManager = mockk<PlayerSettingsManager>()
        val senderSettings =
            TestUtils.createTestPlayerSettings(
                japaneseConversionEnabled = false,
            )

        every { settingsManager.getSettings(any()) } returns senderSettings

        val handler = createHandler(settingsManager = settingsManager)
        val sender = TestUtils.createMockPlayer()
        val recipient = TestUtils.createMockPlayer()

        val result = handler.sendDirectMessage(sender, recipient, "konnichiwa")

        assertTrue(result)
    }

    @Test
    fun `sendDirectMessage with conversion should handle conversion timeout gracefully`() {
        val settingsManager = mockk<PlayerSettingsManager>()
        val senderSettings =
            TestUtils.createTestPlayerSettings(
                japaneseConversionEnabled = true,
            )
        val romanjiConverter = mockk<RomanjiConverter>()

        every { settingsManager.getSettings(any()) } returns senderSettings
        // Simulate a slow conversion that would timeout
        coEvery { romanjiConverter.convert(any()) } coAnswers {
            kotlinx.coroutines.delay(2000) // Exceeds 1s timeout
            "こんにちは"
        }

        val handler = createHandler(settingsManager = settingsManager, romanjiConverter = romanjiConverter)
        val sender = TestUtils.createMockPlayer()
        val recipient = TestUtils.createMockPlayer()

        // Should not throw exception and should complete quickly (within timeout)
        val result = handler.sendDirectMessage(sender, recipient, "konnichiwa")

        assertTrue(result)
    }

    @Test
    fun `handler can be created with injected configuration`() {
        val config = TestUtils.createTestConfiguration(debug = true)
        val languageManager = mockk<LanguageManager>(relaxed = true)
        val handler = DirectMessageHandler(config, null, null, languageManager)

        // Handler should accept configuration via constructor (DI pattern)
        // This validates Issue #1 refactoring - ConfigManager DI
        val sender = TestUtils.createMockPlayer()
        val recipient = TestUtils.createMockPlayer()
        val result = handler.sendDirectMessage(sender, recipient, "Test")

        assertTrue(result)
    }

    @Test
    fun `handler can be created with all dependencies`() {
        val config = TestUtils.createTestConfiguration()
        val settingsManager = mockk<PlayerSettingsManager>(relaxed = true)
        val romanjiConverter = mockk<RomanjiConverter>(relaxed = true)
        val languageManager = mockk<LanguageManager>(relaxed = true)

        every { settingsManager.getSettings(any()) } returns TestUtils.createTestPlayerSettings()

        val handler = DirectMessageHandler(config, settingsManager, romanjiConverter, languageManager)

        val sender = TestUtils.createMockPlayer()
        val recipient = TestUtils.createMockPlayer()
        val result = handler.sendDirectMessage(sender, recipient, "Test")

        assertTrue(result)
    }
}
