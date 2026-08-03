package dev.m1sk9.lunaticChat.paper.config.key

import kotlinx.serialization.Serializable

@Serializable
data class QuickRepliesFeatureConfig(
    val enabled: Boolean = true,
)
