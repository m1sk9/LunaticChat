package dev.m1sk9.lunaticChat.engine.exception

import java.util.UUID

class ChannelAlreadyActiveException(
    val playerId: UUID,
    val channelId: String,
) : Exception("Channel $channelId is already active for player $playerId")
