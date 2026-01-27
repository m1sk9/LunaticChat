package dev.m1sk9.lunaticChat.engine.exception

class ChannelMemberLimitExceededException(
    val channelId: String,
    val limit: Int,
) : Exception(
        "Cannot join channel '$channelId': channel has reached the maximum member limit of $limit",
    )
