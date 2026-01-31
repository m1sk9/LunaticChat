package dev.m1sk9.lunaticChat.engine.chat.channel

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * Represents a single channel message log entry in NDJSON format.
 *
 * This data class is designed for append-only logging to daily rotated files,
 * compatible with Grafana Loki and other log aggregation systems.
 *
 * @property timestamp ISO-8601 formatted UTC timestamp of when the message was sent
 * @property playerId UUID of the player who sent the message
 * @property playerName Display name of the player
 * @property channelId Unique identifier of the channel
 * @property message The chat message content
 */
@Serializable
data class ChannelMessageLogEntry(
    val timestamp: String,
    val playerId: String,
    val playerName: String,
    val channelId: String,
    val message: String,
) {
    companion object {
        /**
         * Creates a new log entry with the current timestamp.
         *
         * @param playerId UUID of the player
         * @param playerName Display name of the player
         * @param channelId Channel identifier
         * @param message Message content
         * @return New ChannelMessageLogEntry instance
         */
        fun create(
            playerId: UUID,
            playerName: String,
            channelId: String,
            message: String,
        ): ChannelMessageLogEntry =
            ChannelMessageLogEntry(
                timestamp = Instant.now().toString(),
                playerId = playerId.toString(),
                playerName = playerName,
                channelId = channelId,
                message = message,
            )
    }
}
