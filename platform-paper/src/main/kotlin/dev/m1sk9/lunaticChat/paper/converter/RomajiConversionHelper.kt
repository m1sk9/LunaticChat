package dev.m1sk9.lunaticChat.paper.converter

import kotlinx.coroutines.CancellationException
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
suspend fun convertWithRomaji(
    message: String,
    converter: RomanjiConverter,
    timeoutMs: Long = 1000,
): String =
    try {
        withTimeoutOrNull(timeoutMs) {
            converter.convert(message)
        }?.let { "$message §e($it)" } ?: message
    } catch (e: CancellationException) {
        // Rethrown rather than degraded to the original message: this runs on the delivery queue, so
        // swallowing it would let a message be delivered after the plugin scope has been cancelled.
        // The conversion's own timeout is handled by withTimeoutOrNull and does not reach here.
        throw e
    } catch (_: Exception) {
        message
    }

/**
 * Blocking form of [convertWithRomaji], for callers that cannot suspend.
 *
 * AsyncChatEvent is the only such caller: it has to decide whether to cancel the event and what
 * body to set before the handler returns, and it already runs off the tick thread. Command
 * handlers do run on the tick thread and must use the suspending form instead.
 */
fun convertWithRomajiBlocking(
    message: String,
    converter: RomanjiConverter,
    timeoutMs: Long = 1000,
): String = runBlocking { convertWithRomaji(message, converter, timeoutMs) }
