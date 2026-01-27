package dev.m1sk9.lunaticChat.engine.exception

import java.util.UUID

class ChannelCannotInviteSelfException(
    playerId: UUID,
) : Exception("Player $playerId cannot invite themselves")
