package dev.m1sk9.lunaticChat.paper.converter

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.logging.Logger

class RomanjiConverter(
    private val cache: ConversionCache,
    private val apiClient: GoogleIMEClient,
    private val logger: Logger,
    private val debugMode: Boolean = false,
    maxConcurrentRequests: Int = 4,
) {
    // A long message would otherwise open one request per word at once. Google IME answering with
    // a rate limit lands in convertWord's catch and degrades silently to hiragana, so it is better
    // not to ask that hard in the first place.
    private val limiter = Semaphore(maxConcurrentRequests)

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
        //
        // Converted once per distinct word: the sequential version got that for free because it
        // cached each word before looking up the next, and a line that repeats a word should not
        // ask the API twice for it.
        val converted =
            coroutineScope {
                words
                    .distinct()
                    .map { word -> async { word to limiter.withPermit { convertWord(word) } } }
                    .awaitAll()
            }.toMap()

        return words.joinToString(" ") { converted.getValue(it) }
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
            } catch (e: CancellationException) {
                // Not an API failure: the caller's timeout fired. Caching the hiragana here would
                // pin every word of the message to its unconverted form for good, because the words
                // are converted concurrently and the timeout cancels all of them at once.
                throw e
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
