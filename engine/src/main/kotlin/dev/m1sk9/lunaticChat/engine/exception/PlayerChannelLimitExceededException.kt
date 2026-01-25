package dev.m1sk9.lunaticChat.engine.exception

import java.util.UUID

class PlayerChannelLimitExceededException(
    val playerId: UUID,
    val limit: Int,
) : Exception(
        "Player $playerId has reached the maximum channel membership limit of $limit",
    )
