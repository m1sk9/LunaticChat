package dev.m1sk9.lunaticChat.paper.config.key

import dev.m1sk9.lunaticChat.paper.config.LenientBoolean
import kotlinx.serialization.Serializable

@Serializable
data class ChannelChatFeatureConfig(
    val enabled: LenientBoolean = false,
    val maxChannelsPerServer: Int = 0,
    val maxMembersPerChannel: Int = 0,
    val maxMembershipPerPlayer: Int = 0,
    val messageLogging: ChannelMessageLoggingConfig = ChannelMessageLoggingConfig(),
)
