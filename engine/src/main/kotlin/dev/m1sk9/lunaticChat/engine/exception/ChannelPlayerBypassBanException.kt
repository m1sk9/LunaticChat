package dev.m1sk9.lunaticChat.engine.exception

import java.util.UUID

class ChannelPlayerBypassBanException(
    playerId: UUID,
    val channelId: String,
) : Exception("Cannot ban player $playerId: player has bypass permission")
