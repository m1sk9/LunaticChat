package dev.m1sk9.lunaticChat.engine.chat.channel

import kotlinx.serialization.Serializable

@Serializable
data class ChannelData(
    val version: Int = 1,
    val channels: Map<String, Channel> = emptyMap(),
    val members: Map<String, List<ChannelMember>> = emptyMap(),
    val activeChannels: Map<String, String> = emptyMap(),
)
