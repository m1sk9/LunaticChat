package dev.m1sk9.lunaticChat.engine.exception

import java.util.UUID

class ChannelNoOwnerPermissionException(
    ownerId: UUID,
) : Exception("No permission for channel owned by player with ID $ownerId.")
