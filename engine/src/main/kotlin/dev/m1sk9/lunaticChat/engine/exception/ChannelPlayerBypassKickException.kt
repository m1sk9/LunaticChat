package dev.m1sk9.lunaticChat.engine.exception

import java.util.UUID

class ChannelPlayerBypassKickException(
    playerId: UUID,
    val channelId: String,
) : Exception("Cannot kick player $playerId: player has bypass permission")
