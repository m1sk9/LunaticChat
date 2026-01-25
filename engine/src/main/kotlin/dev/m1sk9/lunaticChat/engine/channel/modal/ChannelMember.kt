package dev.m1sk9.lunaticChat.engine.channel.modal

import dev.m1sk9.lunaticChat.engine.settings.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a member of a channel with their role and join timestamp.
 *
 * @property channelId The ID of the channel.
 * @property playerId The UUID of the player.
 * @property role The role of the member in the channel.
 * @property joinedAt The timestamp when the member joined the channel.
 */
@Serializable
data class ChannelMember(
    val channelId: String,
    @Serializable(with = UUIDSerializer::class)
    val playerId: UUID,
    val role: ChannelRole,
    val joinedAt: Long = System.currentTimeMillis(),
)
