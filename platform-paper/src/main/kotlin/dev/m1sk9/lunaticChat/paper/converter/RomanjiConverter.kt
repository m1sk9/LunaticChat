package dev.m1sk9.lunaticChat.paper.converter

import java.util.logging.Logger

class RomanjiConverter(
    private val cache: ConversionCache,
    private val apiClient: GoogleIMEClient,
    private val logger: Logger,
    private val debugMode: Boolean = false,
) {
    /**
     * Converts the given romaji input to Japanese using the API client.
     * Utilizes a cache to store and retrieve previous conversion results.
     *
     * Step 1: Romanji -> Hiragana (using KanaConverter)
     * Step 2: Hiragana -> Kanji/Kana (using Google IME API)
     *
     * @param input The romaji string to convert.
     * @return The converted Japanese string, or the original input if conversion fails.
     * @throws Exception if the conversion process encounters an error.
     */
    suspend fun convert(input: String): String {
        cache.get(input)?.let {
            return it
        }

        // Step 1: Romanji -> Hiragana
        val hiragana = KanaConverter.toHiragana(input)
        if (debugMode) {
            logger.info("Romanji -> Hiragana: $input -> $hiragana")
        }

        // Step 2: Hiragana -> Kanji/Kana
        val result =
            try {
                apiClient.convert(hiragana)
            } catch (e: Exception) {
                logger.warning("Failed to convert $hiragana: ${e.message}")
                return hiragana // Return hiragana if API fails
            }

        cache.put(input, result)
        return result
    }
}
