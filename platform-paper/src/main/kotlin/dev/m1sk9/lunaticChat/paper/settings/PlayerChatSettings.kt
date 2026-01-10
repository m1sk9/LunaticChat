package dev.m1sk9.lunaticChat.paper.settings

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Data model for per-player chat settings.
 *
 * @property uuid The unique identifier of the player
 * @property japaneseConversionEnabled Whether romaji-to-Japanese conversion is enabled for this player
 */
@Serializable
data class PlayerChatSettings(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val japaneseConversionEnabled: Boolean = false,
)
