package dev.m1sk9.lunaticChat.engine.exception

import java.util.UUID

class ChannelPlayerBannedException(
    playerId: UUID,
    val channelId: String,
) : Exception("Player $playerId is banned from channel $channelId")
