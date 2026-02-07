package dev.m1sk9.lunaticChat.paper.converter

import dev.m1sk9.lunaticChat.engine.converter.GoogleIMEClient
import dev.m1sk9.lunaticChat.paper.TestUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for RomanjiConverter.
 * Verifies conversion logic, English word handling, and caching behavior.
 *
 * Note: The alphabetic character check after KanaConverter determines whether
 * a word is treated as English or romaji:
 * - "version" -> "vえrしおん" (has 'v', 'r') -> kept as "version"
 * - "hello" -> "へっぉ" (no alphabetic chars) -> treated as romaji, converted
 * - "API" -> "あぴ" (no alphabetic chars) -> treated as romaji, converted
 */
class RomanjiConverterTest {
    private fun createConverter(debugMode: Boolean = false): Triple<RomanjiConverter, ConversionCache, GoogleIMEClient> {
        val cache = mockk<ConversionCache>(relaxed = true)
        val apiClient = mockk<GoogleIMEClient>(relaxed = true)
        val logger = TestUtils.TestLogger()

        every { cache.get(any()) } returns null

        val converter = RomanjiConverter(cache, apiClient, logger, debugMode)
        return Triple(converter, cache, apiClient)
    }

    // ===== English words with unconvertible letters should NOT be converted =====
    // Words containing letters like 'v', 'l', 'x', 'q' that have no direct hiragana mapping
    // will retain those letters after KanaConverter, triggering the English word detection.

    @Test
    fun `English word 'version' should not be converted (has v and r)`() =
        runBlocking {
            val (converter, _, _) = createConverter()
            // "version" -> "vえrしおん" (v, r remain)

            val result = converter.convert("version")

            assertEquals("version", result)
        }

    @Test
    fun `English word 'server' should not be converted (has r and v)`() =
        runBlocking {
            val (converter, _, _) = createConverter()
            // "server" -> "せrvえr" (r, v, r remain)

            val result = converter.convert("server")

            assertEquals("server", result)
        }

    @Test
    fun `English word 'latest' should not be converted (has s and t)`() =
        runBlocking {
            val (converter, _, _) = createConverter()
            // "latest" -> "ぁてst" (s, t remain)

            val result = converter.convert("latest")

            assertEquals("latest", result)
        }

    @Test
    fun `English word 'running' should not be converted (has g)`() =
        runBlocking {
            val (converter, _, _) = createConverter()
            // "running" -> "るんいんg" (g remains)

            val result = converter.convert("running")

            assertEquals("running", result)
        }

    @Test
    fun `English word 'world' should not be converted (has r, l, d)`() =
        runBlocking {
            val (converter, _, _) = createConverter()
            // "world" -> "をrld" (r, l, d remain)

            val result = converter.convert("world")

            assertEquals("world", result)
        }

    @Test
    fun `English words should not call API`() =
        runBlocking {
            val (converter, _, apiClient) = createConverter()

            // These words have unconvertible letters
            converter.convert("version")
            converter.convert("server")
            converter.convert("world")

            coVerify(exactly = 0) { apiClient.convert(any()) }
        }

    // ===== Words that happen to be valid romaji will be converted =====
    // Some English words like "hello" (h->へ, ll->っ, o->ぉ) or "API" (a->あ, p->ぴ, i is consumed)
    // completely convert to hiragana, so they are treated as romaji.

    @Test
    fun `'hello' converts completely to hiragana so it is treated as romaji`() =
        runBlocking {
            val (converter, _, apiClient) = createConverter()
            // "hello" -> "へっぉ" (no alphabetic chars remain)
            coEvery { apiClient.convert("へっぉ") } returns "へっぉ"

            val result = converter.convert("hello")

            assertEquals("へっぉ", result)
            coVerify(exactly = 1) { apiClient.convert("へっぉ") }
        }

    @Test
    fun `'API' converts completely to hiragana so it is treated as romaji`() =
        runBlocking {
            val (converter, _, apiClient) = createConverter()
            // "API" -> "あぴ" (no alphabetic chars remain)
            coEvery { apiClient.convert("あぴ") } returns "あぴ"

            val result = converter.convert("API")

            assertEquals("あぴ", result)
            coVerify(exactly = 1) { apiClient.convert("あぴ") }
        }

    // ===== Pure romaji should be converted =====

    @Test
    fun `romaji 'konnichiwa' should be converted`() =
        runBlocking {
            val (converter, _, apiClient) = createConverter()
            // KanaConverter.toHiragana("konnichiwa") returns "こんいちわ"
            coEvery { apiClient.convert("こんいちわ") } returns "こんにちは"

            val result = converter.convert("konnichiwa")

            assertEquals("こんにちは", result)
            coVerify(exactly = 1) { apiClient.convert("こんいちわ") }
        }

    @Test
    fun `romaji 'ohayou' should be converted`() =
        runBlocking {
            val (converter, _, apiClient) = createConverter()
            coEvery { apiClient.convert("おはよう") } returns "おはよう"

            val result = converter.convert("ohayou")

            assertEquals("おはよう", result)
            coVerify(exactly = 1) { apiClient.convert("おはよう") }
        }

    @Test
    fun `romaji 'arigatou' should be converted`() =
        runBlocking {
            val (converter, _, apiClient) = createConverter()
            coEvery { apiClient.convert("ありがとう") } returns "ありがとう"

            val result = converter.convert("arigatou")

            assertEquals("ありがとう", result)
            coVerify(exactly = 1) { apiClient.convert("ありがとう") }
        }

    // ===== Mixed text (romaji + English) =====

    @Test
    fun `mixed text should convert romaji and keep English words with unconvertible letters`() =
        runBlocking {
            val (converter, _, apiClient) = createConverter()
            // "konnichiwa" -> "こんいちわ" (pure romaji, converted)
            // "server" -> "せrvえr" (has r, v, r, kept as English)
            coEvery { apiClient.convert("こんいちわ") } returns "こんにちは"

            val result = converter.convert("konnichiwa server")

            assertEquals("こんにちは server", result)
            coVerify(exactly = 1) { apiClient.convert("こんいちわ") }
        }

    @Test
    fun `multiple English words with unconvertible letters should all be preserved`() =
        runBlocking {
            val (converter, _, apiClient) = createConverter()
            // "version" has v, r
            // "server" has r, v, r
            // "world" has r, l, d

            val result = converter.convert("version server world")

            assertEquals("version server world", result)
            coVerify(exactly = 0) { apiClient.convert(any()) }
        }

    // ===== Edge cases =====

    @Test
    fun `empty string should return empty string`() =
        runBlocking {
            val (converter, _, _) = createConverter()

            val result = converter.convert("")

            assertEquals("", result)
        }

    @Test
    fun `non-ASCII input should return null`() =
        runBlocking {
            val (converter, _, _) = createConverter()

            val result = converter.convert("こんにちは")

            assertNull(result)
        }

    @Test
    fun `numbers only should return as-is`() =
        runBlocking {
            val (converter, _, apiClient) = createConverter()
            // Numbers pass through KanaConverter unchanged, no alphabetic chars
            coEvery { apiClient.convert("123") } returns "123"

            val result = converter.convert("123")

            assertEquals("123", result)
        }

    @Test
    fun `spaces only should return empty due to word filtering`() =
        runBlocking {
            val (converter, _, _) = createConverter()

            val result = converter.convert("   ")

            assertEquals("", result)
        }

    // ===== Cache behavior =====

    @Test
    fun `cached word should be retrieved from cache`() =
        runBlocking {
            val (converter, cache, apiClient) = createConverter()
            every { cache.get("konnichiwa") } returns "こんにちは"

            val result = converter.convert("konnichiwa")

            assertEquals("こんにちは", result)
            verify(exactly = 1) { cache.get("konnichiwa") }
            coVerify(exactly = 0) { apiClient.convert(any()) }
        }

    @Test
    fun `converted word should be stored in cache`() =
        runBlocking {
            val (converter, cache, apiClient) = createConverter()
            coEvery { apiClient.convert("おはよう") } returns "おはよう"

            converter.convert("ohayou")

            verify(exactly = 1) { cache.put("ohayou", "おはよう") }
        }

    @Test
    fun `English words with unconvertible letters should not be cached`() =
        runBlocking {
            val (converter, cache, _) = createConverter()

            converter.convert("version")

            verify(exactly = 0) { cache.put(any(), any()) }
        }

    // ===== API error handling =====

    @Test
    fun `API failure should fallback to hiragana`() =
        runBlocking {
            val (converter, cache, apiClient) = createConverter()
            coEvery { apiClient.convert("おはよう") } throws Exception("API Error")

            val result = converter.convert("ohayou")

            assertEquals("おはよう", result)
            verify(exactly = 1) { cache.put("ohayou", "おはよう") }
        }
}
