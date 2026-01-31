package dev.m1sk9.lunaticChat.paper.config.key

import kotlinx.serialization.Serializable

@Serializable
data class ChannelChatFeatureConfig(
    val enabled: Boolean,
    val maxChannelsPerServer: Int = 0,
    val maxMembersPerChannel: Int = 0,
    val maxMembershipPerPlayer: Int = 0,
    val messageLogging: ChannelMessageLoggingConfig = ChannelMessageLoggingConfig(),
)
