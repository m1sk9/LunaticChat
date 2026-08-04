package dev.m1sk9.lunaticChat.paper.config.key

import dev.m1sk9.lunaticChat.paper.config.LenientBoolean
import kotlinx.serialization.Serializable

@Serializable
data class QuickRepliesFeatureConfig(
    val enabled: LenientBoolean = true,
)
