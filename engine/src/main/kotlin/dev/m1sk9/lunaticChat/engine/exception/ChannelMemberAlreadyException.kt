package dev.m1sk9.lunaticChat.engine.exception

import java.util.UUID

class ChannelMemberAlreadyException(
    playerId: UUID,
    val channelId: String,
) : Exception("Player $playerId is already a member of channel $channelId")
