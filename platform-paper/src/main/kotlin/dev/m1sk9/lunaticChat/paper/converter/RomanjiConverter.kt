package dev.m1sk9.lunaticChat.paper.converter

import dev.m1sk9.lunaticChat.engine.converter.GoogleIMEClient
import dev.m1sk9.lunaticChat.engine.converter.KanaConverter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.logging.Logger

class RomanjiConverter(
    private val cache: ConversionCache,
    private val apiClient: GoogleIMEClient,
    private val logger: Logger,
    private val debugMode: Boolean = false,
) {
    /**
     * Converts the given romaji input to Japanese using the API client.
     * Utilizes a word-level cache to store and retrieve previous conversion results.
     * Each word is cached separately to improve cache hit rate.
     *
     * Step 1: Romanji -> Hiragana (using KanaConverter)
     * Step 2: Hiragana -> Kanji/Kana (using Google IME API)
     *
     * @param input The romaji string to convert.
     * @return The converted Japanese string, or null if the input contains non-romaji characters.
     * @throws Exception if the conversion process encounters an error.
     */
    suspend fun convert(input: String): String? {
        if (!isRomajiOnly(input)) {
            if (debugMode) {
                logger.info("Input contains non-romaji characters, skipping conversion: $input")
            }
            return null
        }

        val words = input.split(" ").filter { it.isNotEmpty() }

        // The words are independent, and callers convert under a timeout covering the whole
        // message. Awaiting them one at a time makes an N-word message cost N round trips, so a
        // long message runs out of budget after the first word or two.
        val results =
            coroutineScope {
                words.map { word -> async { convertWord(word) } }.awaitAll()
            }

        return results.joinToString(" ")
    }

    private suspend fun convertWord(word: String): String {
        cache.get(word)?.let { cached ->
            if (debugMode) {
                logger.info("Cache hit for word: $word -> $cached")
            }
            return cached
        }

        // Pre-validate: Check if the word is valid romaji before attempting conversion
        // This prevents partial conversion of English words (e.g., "This" -> "てぃs")
        if (!KanaConverter.isValidRomaji(word)) {
            if (debugMode) {
                logger.info("Word is not valid romaji, keeping original: $word")
            }
            return word
        }

        // Step 1: Romanji -> Hiragana
        val hiragana = KanaConverter.toHiragana(word)

        // Step 2: Hiragana -> Kanji/Kana
        val converted =
            try {
                apiClient.convert(hiragana)
            } catch (e: Exception) {
                logger.warning("Failed to convert $hiragana: ${e.message}")
                hiragana // Use hiragana if API fails
            }

        cache.put(word, converted)
        return converted
    }

    private fun isRomajiOnly(input: String): Boolean =
        input.all {
            it.code in 0x20..0x7E
        }
}
