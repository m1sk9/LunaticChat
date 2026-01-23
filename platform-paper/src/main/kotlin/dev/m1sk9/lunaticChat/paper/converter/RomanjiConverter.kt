package dev.m1sk9.lunaticChat.paper.converter

import dev.m1sk9.lunaticChat.engine.converter.GoogleIMEClient
import dev.m1sk9.lunaticChat.engine.converter.KanaConverter
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

        val words = input.split(" ")
        val results = mutableListOf<String>()

        for (word in words) {
            if (word.isEmpty()) {
                continue
            }

            // Check cache first
            val cached = cache.get(word)
            if (cached != null) {
                if (debugMode) {
                    logger.info("Cache hit for word: $word -> $cached")
                }
                results.add(cached)
                continue
            }

            // Step 1: Romanji -> Hiragana
            val hiragana = KanaConverter.toHiragana(word)
            if (debugMode) {
                logger.info("Romanji -> Hiragana: $word -> $hiragana")
            }

            // Step 2: Hiragana -> Kanji/Kana
            val converted =
                try {
                    apiClient.convert(hiragana)
                } catch (e: Exception) {
                    logger.warning("Failed to convert $hiragana: ${e.message}")
                    hiragana // Use hiragana if API fails
                }

            // Cache the word-level conversion
            cache.put(word, converted)
            results.add(converted)
        }

        return results.joinToString(" ")
    }

    private fun isRomajiOnly(input: String): Boolean =
        input.all {
            it.code in 0x20..0x7E
        }
}
