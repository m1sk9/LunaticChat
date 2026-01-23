package dev.m1sk9.lunaticChat.paper.i18n

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageFormatterTest {
    private val plainSerializer = PlainTextComponentSerializer.plainText()

    @Test
    fun `format should add LC prefix`() {
        val result = MessageFormatter.format("test message")
        val plainText = plainSerializer.serialize(result)
        assertTrue(plainText.startsWith("[LC] "))
    }

    @Test
    fun `format should include message text`() {
        val result = MessageFormatter.format("test message")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] test message", plainText)
    }

    @Test
    fun `format should preserve placeholder braces in plain text`() {
        val result = MessageFormatter.format("Hello {name}")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] Hello {name}", plainText)
    }

    @Test
    fun `format should handle multiple placeholders`() {
        val result = MessageFormatter.format("Player {player} sent message to {target}")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] Player {player} sent message to {target}", plainText)
    }

    @Test
    fun `format should handle message without placeholders`() {
        val result = MessageFormatter.format("Simple message", highlightPlaceholders = false)
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] Simple message", plainText)
    }

    @Test
    fun `formatError should add LC prefix`() {
        val result = MessageFormatter.formatError("error message")
        val plainText = plainSerializer.serialize(result)
        assertTrue(plainText.startsWith("[LC] "))
    }

    @Test
    fun `formatError should include error message text`() {
        val result = MessageFormatter.formatError("Something went wrong")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] Something went wrong", plainText)
    }

    @Test
    fun `formatError should preserve placeholders in plain text`() {
        val result = MessageFormatter.formatError("Player {player} not found")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] Player {player} not found", plainText)
    }

    @Test
    fun `formatSuccess should add LC prefix`() {
        val result = MessageFormatter.formatSuccess("success message")
        val plainText = plainSerializer.serialize(result)
        assertTrue(plainText.startsWith("[LC] "))
    }

    @Test
    fun `formatSuccess should include success message text`() {
        val result = MessageFormatter.formatSuccess("Operation completed")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] Operation completed", plainText)
    }

    @Test
    fun `formatSuccess should preserve placeholders in plain text`() {
        val result = MessageFormatter.formatSuccess("Setting {setting} enabled")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] Setting {setting} enabled", plainText)
    }

    @Test
    fun `format should handle empty message`() {
        val result = MessageFormatter.format("")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] ", plainText)
    }

    @Test
    fun `format should handle message with only placeholder`() {
        val result = MessageFormatter.format("{value}")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] {value}", plainText)
    }

    @Test
    fun `format should handle nested braces`() {
        val result = MessageFormatter.format("{{nested}}")
        val plainText = plainSerializer.serialize(result)
        // Only {nested} should be treated as placeholder, outer braces are literal
        assertTrue(plainText.contains("{nested}"))
    }

    @Test
    fun `format should handle Japanese text`() {
        val result = MessageFormatter.format("こんにちは {name}")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] こんにちは {name}", plainText)
    }

    @Test
    fun `formatError should handle Japanese text`() {
        val result = MessageFormatter.formatError("エラーが発生しました")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] エラーが発生しました", plainText)
    }

    @Test
    fun `formatSuccess should handle Japanese text`() {
        val result = MessageFormatter.formatSuccess("設定を有効にしました")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] 設定を有効にしました", plainText)
    }

    @Test
    fun `format should handle very long message`() {
        val longMessage = "a".repeat(1000)
        val result = MessageFormatter.format(longMessage)
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] $longMessage", plainText)
    }

    @Test
    fun `format should handle special characters`() {
        val result = MessageFormatter.format("Special: !@#$%^&*()")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] Special: !@#$%^&*()", plainText)
    }

    @Test
    fun `format should handle message with newlines`() {
        val result = MessageFormatter.format("Line 1\nLine 2")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] Line 1\nLine 2", plainText)
    }

    @Test
    fun `format should handle unclosed brace`() {
        val result = MessageFormatter.format("Unclosed {placeholder")
        val plainText = plainSerializer.serialize(result)
        // Unclosed braces should not be treated as placeholders
        assertEquals("[LC] Unclosed {placeholder", plainText)
    }

    @Test
    fun `format should handle placeholder at start`() {
        val result = MessageFormatter.format("{player} joined the game")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] {player} joined the game", plainText)
    }

    @Test
    fun `format should handle placeholder at end`() {
        val result = MessageFormatter.format("Welcome {player}")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] Welcome {player}", plainText)
    }

    @Test
    fun `format should handle consecutive placeholders`() {
        val result = MessageFormatter.format("{player1}{player2}")
        val plainText = plainSerializer.serialize(result)
        assertEquals("[LC] {player1}{player2}", plainText)
    }
}
