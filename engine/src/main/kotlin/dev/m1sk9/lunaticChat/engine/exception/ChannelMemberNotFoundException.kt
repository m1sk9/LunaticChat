package dev.m1sk9.lunaticChat.engine.exception

class ChannelMemberNotFoundException(
    channelId: String,
) : Exception("Channel member not found in channel with ID $channelId.")
