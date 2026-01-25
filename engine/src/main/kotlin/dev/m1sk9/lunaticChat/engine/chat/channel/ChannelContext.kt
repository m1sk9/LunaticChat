package dev.m1sk9.lunaticChat.engine.chat.channel

data class ChannelContext(
    val channelId: String,
    val channel: Channel,
    val members: List<ChannelMember>,
)
