package dev.m1sk9.lunaticChat.paper.config.key

import dev.m1sk9.lunaticChat.paper.config.LenientBoolean
import kotlinx.serialization.Serializable

@Serializable
data class VelocityIntegrationConfig(
    val enabled: LenientBoolean = false,
    val crossServerGlobalChat: LenientBoolean = false,
    val crossServerDirectMessage: LenientBoolean = false,
    val serverName: String = "Unknown",
    val messageDeduplicationCacheSize: Int = 100,
)
