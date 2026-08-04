package dev.m1sk9.lunaticChat.paper.converter

import kotlin.time.Duration

/**
 * The Google IME request did not answer in time.
 *
 * Deliberately not a CancellationException: callers degrade a failed conversion to hiragana but must
 * rethrow cancellation, and reporting a timeout through the cancellation channel made an ordinary
 * slow request look like plugin shutdown - killing the sender's delivery queue for the rest of the
 * session.
 */
class ConversionTimeoutException(
    timeout: Duration,
) : Exception("Google IME did not answer within $timeout")
