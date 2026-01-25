package dev.m1sk9.lunaticChat.engine.exception

import java.util.UUID

class ChannelNotMemberException(
    playerId: UUID,
    channelId: String,
) : Exception("Player $playerId is not a member of channel $channelId")
