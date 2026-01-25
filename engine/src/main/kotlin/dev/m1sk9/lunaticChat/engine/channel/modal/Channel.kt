package dev.m1sk9.lunaticChat.engine.channel.modal

import dev.m1sk9.lunaticChat.engine.settings.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a communication channel within the LunaticChat application.
 *
 * @property id Unique identifier for the channel.
 * @property name Name of the channel.
 * @property description Optional description of the channel.
 * @property isPrivate Indicates whether the channel is private or public.
 * @property ownerId UUID of the user who owns the channel.
 * @property createdAt Timestamp of when the channel was created.
 */
@Serializable
data class Channel(
    val id: String,
    val name: String,
    val description: String? = null,
    val isPrivate: Boolean = false,
    @Serializable(with = UUIDSerializer::class)
    val ownerId: UUID,
    val createdAt: Long = System.currentTimeMillis(),
) {
    init {
        require(id.matches(CHANNEL_ID_PATTERN)) {
            "Channel ID must be 3-30 characters long and can only contain letters, numbers, underscores, and hyphens."
        }

        require(name.isNotBlank()) {
            "Channel name cannot be blank."
        }
    }

    companion object {
        val CHANNEL_ID_PATTERN = Regex("^[a-zA-Z0-9_-]{3,30}$")
    }
}
