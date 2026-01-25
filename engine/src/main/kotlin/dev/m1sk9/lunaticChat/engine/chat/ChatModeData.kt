package dev.m1sk9.lunaticChat.engine.chat

import dev.m1sk9.lunaticChat.engine.settings.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ChatModeData(
    val modes: Map<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        ChatMode,
    > = emptyMap(),
)
