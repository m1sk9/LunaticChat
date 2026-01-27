package dev.m1sk9.lunaticChat.engine.exception

import java.util.UUID

class ChannelPlayerNotBannedException(
    playerId: UUID,
    val channelId: String,
) : Exception("Player $playerId is not banned from channel $channelId")
