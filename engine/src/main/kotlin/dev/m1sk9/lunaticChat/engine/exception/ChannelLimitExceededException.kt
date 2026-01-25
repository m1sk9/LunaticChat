package dev.m1sk9.lunaticChat.engine.exception

class ChannelLimitExceededException(
    val limit: Int,
) : Exception(
        "Channel creation failed: server has reached the maximum limit of $limit channels",
    )
