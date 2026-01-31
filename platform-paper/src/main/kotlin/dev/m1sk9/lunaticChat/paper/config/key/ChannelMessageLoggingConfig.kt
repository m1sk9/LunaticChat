package dev.m1sk9.lunaticChat.paper.config.key

import kotlinx.serialization.Serializable

/**
 * Configuration for channel message logging feature.
 *
 * Enables file-based logging of all channel messages in NDJSON format,
 * compatible with log aggregation systems like Grafana Loki.
 *
 * @property enabled Whether message logging is enabled
 * @property retentionDays Number of days to retain log files (0 = keep forever)
 * @property maxFileSizeMB Maximum size of a single log file in megabytes
 */
@Serializable
data class ChannelMessageLoggingConfig(
    val enabled: Boolean = true,
    val retentionDays: Int = 30,
    val maxFileSizeMB: Int = 100,
)
