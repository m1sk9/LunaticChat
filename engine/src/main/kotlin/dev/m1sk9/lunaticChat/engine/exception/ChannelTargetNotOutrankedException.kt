package dev.m1sk9.lunaticChat.engine.exception

import java.util.UUID

class ChannelTargetNotOutrankedException(
    actorId: UUID,
    targetId: UUID,
    val channelId: String,
) : Exception("Player $actorId does not outrank $targetId in channel $channelId")
