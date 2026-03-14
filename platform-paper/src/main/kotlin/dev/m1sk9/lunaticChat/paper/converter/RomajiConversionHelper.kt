package dev.m1sk9.lunaticChat.paper.converter

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Converts a message using romaji-to-Japanese conversion with timeout protection.
 * Returns the original message appended with the conversion result, or the original message on failure.
 *
 * @param message The message to convert.
 * @param converter The RomanjiConverter to use.
 * @param timeoutMs Timeout in milliseconds for the conversion (default: 1000ms).
 * @return The message with conversion appended (e.g., "hello §e(こんにちは)"), or the original message.
 */
fun convertWithRomaji(
    message: String,
    converter: RomanjiConverter,
    timeoutMs: Long = 1000,
): String =
    runCatching {
        runBlocking {
            withTimeoutOrNull(timeoutMs) {
                converter.convert(message)
            }?.let { "$message §e($it)" } ?: message
        }
    }.getOrElse { message }
