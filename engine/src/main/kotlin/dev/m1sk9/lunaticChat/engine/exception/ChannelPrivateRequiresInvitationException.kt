package dev.m1sk9.lunaticChat.engine.exception

import java.util.UUID

class ChannelPrivateRequiresInvitationException(
    val playerId: UUID,
    val channelId: String,
) : Exception("Player $playerId cannot join private channel $channelId without invitation")
