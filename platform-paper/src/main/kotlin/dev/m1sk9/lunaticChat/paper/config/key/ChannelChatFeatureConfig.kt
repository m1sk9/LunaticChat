package dev.m1sk9.lunaticChat.paper.config.key

data class ChannelChatFeatureConfig(
    val enabled: Boolean,
    val maxChannelsPerServer: Int = 0,
    val maxMembersPerChannel: Int = 0,
    val maxMembershipPerPlayer: Int = 0,
)
