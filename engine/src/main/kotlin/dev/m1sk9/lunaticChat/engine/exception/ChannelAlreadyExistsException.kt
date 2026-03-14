package dev.m1sk9.lunaticChat.engine.exception

class ChannelAlreadyExistsException(
    channelId: String,
) : Exception("Channel with ID $channelId already exists.")
