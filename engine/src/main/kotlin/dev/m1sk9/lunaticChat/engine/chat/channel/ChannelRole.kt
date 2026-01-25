package dev.m1sk9.lunaticChat.engine.chat.channel

import kotlinx.serialization.Serializable

/**
 * Represents the role of a user within a channel.
 *
 * - OWNER: The creator and primary administrator of the channel.
 * - MODERATOR: A user with elevated permissions to manage channel content and users.
 * - MEMBER: A regular user with standard access to the channel.
 */
@Serializable
enum class ChannelRole {
    OWNER,
    MODERATOR,
    MEMBER,
}
