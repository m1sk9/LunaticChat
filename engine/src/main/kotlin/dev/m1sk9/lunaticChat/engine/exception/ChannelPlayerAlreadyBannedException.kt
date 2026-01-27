package dev.m1sk9.lunaticChat.engine.exception

import java.util.UUID

class ChannelPlayerAlreadyBannedException(
    val playerId: UUID,
    val channelId: String,
) : Exception("Player $playerId is already banned from channel $channelId")
