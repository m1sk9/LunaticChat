package dev.m1sk9.lunaticChat.paper.config.key

import kotlinx.serialization.Serializable

@Serializable
data class VelocityIntegrationConfig(
    val enabled: Boolean = false,
    val crossServerGlobalChat: Boolean = false,
    val crossServerDirectMessage: Boolean = false,
    val serverName: String = "Unknown",
    val messageDeduplicationCacheSize: Int = 100,
)
