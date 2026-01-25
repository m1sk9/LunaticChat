package dev.m1sk9.lunaticChat.paper.channel

import dev.m1sk9.lunaticChat.engine.channel.modal.ChannelRole
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotMemberException
import java.util.UUID
import java.util.logging.Logger

class ChannelMembershipManager(
    private val channelManager: ChannelManager,
    private val logger: Logger,
) {
    /**
     * Checks if a player is a member of a channel.
     *
     * @param playerId The UUID of the player.
     * @param channelId The ID of the channel.
     * @return Result containing true if the player is a member, false otherwise.
     */
    fun isMember(
        playerId: UUID,
        channelId: String,
    ): Result<Boolean> =
        channelManager.getChannelMembers(channelId).map { members ->
            members.any { it.playerId == playerId }
        }

    /**
     * Gets the role of a member in a channel.
     *
     * @param playerId The UUID of the player.
     * @param channelId The ID of the channel.
     * @return Result containing the ChannelRole of the member.
     * @throws ChannelNotMemberException if the player is not a member of the channel.
     */
    fun getMemberRole(
        playerId: UUID,
        channelId: String,
    ): Result<ChannelRole> =
        channelManager.getChannelMembers(channelId).mapCatching { members ->
            members
                .find {
                    it.playerId == playerId
                }?.role ?: throw ChannelNotMemberException(playerId, channelId)
        }

    /**
     * Checks if a player has a specific role or higher in a channel.
     *
     * @param playerId The UUID of the player.
     * @param channelId The ID of the channel.
     * @param requireRole The required ChannelRole.
     * @return Result containing true if the player has the required role or higher, false otherwise.
     */
    fun hasRole(
        playerId: UUID,
        channelId: String,
        requireRole: ChannelRole,
    ): Result<Boolean> =
        getMemberRole(playerId, channelId).fold(
            onSuccess = { playerRole ->
                val hasRequiredRole =
                    when (requireRole) {
                        ChannelRole.MEMBER -> true
                        ChannelRole.MODERATOR -> playerRole in setOf(ChannelRole.MODERATOR, ChannelRole.OWNER)
                        ChannelRole.OWNER -> playerRole == ChannelRole.OWNER
                    }
                Result.success(hasRequiredRole)
            },
            onFailure = { Result.success(false) },
        )
}
