package dev.m1sk9.lunaticChat.engine.protocol

/**
 * The first [limit] bytes as space-separated hex, for logging a payload that failed to decode.
 */
fun ByteArray.toHexPreview(limit: Int = 16): String = take(limit).joinToString(" ") { "%02x".format(it) }
