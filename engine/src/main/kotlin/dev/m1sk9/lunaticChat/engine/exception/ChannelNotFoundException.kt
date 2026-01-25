package dev.m1sk9.lunaticChat.engine.exception

class ChannelNotFoundException(
    channelId: String,
) : Exception("Channel with ID $channelId not found.")
