package dev.m1sk9.lunaticChat.paper.chat.channel

import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.exception.ChannelAlreadyActiveException
import dev.m1sk9.lunaticChat.engine.exception.ChannelMemberAlreadyException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotMemberException
import dev.m1sk9.lunaticChat.engine.exception.ChannelRuntimeException
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

    /**
     * Adds a player to a channel as a member and sets it as their active channel.
     * If the player is already a member of other channels, they remain members of those channels.
     * Only one channel can be active at a time.
     *
     * @param playerId The UUID of the player.
     * @param channelId The ID of the channel.
     * @return Result indicating success or failure.
     * @throws ChannelNotFoundException if the channel does not exist.
     * @throws ChannelMemberAlreadyException if the player is already a member of this channel.
     * @throws ChannelRuntimeException for other runtime errors.
     */
    fun joinChannel(
        playerId: UUID,
        channelId: String,
    ): Result<Unit> {
        // Check if channel exists
        val channel =
            channelManager.getChannel(channelId).getOrElse {
                return Result.failure(
                    ChannelNotFoundException(channelId),
                )
            }

        // Check if this channel is already active
        val currentActiveChannel = channelManager.getPlayerChannel(playerId)
        if (currentActiveChannel == channelId) {
            return Result.failure(
                ChannelAlreadyActiveException(playerId, channelId),
            )
        }

        // Check if player is already a member
        val isAlreadyMember =
            isMember(playerId, channelId).getOrElse {
                return Result.failure(
                    ChannelRuntimeException("Failed to check membership for player $playerId in channel $channelId", it),
                )
            }

        // If already a member, return error
        if (isAlreadyMember) {
            return Result.failure(
                ChannelMemberAlreadyException(playerId, channelId),
            )
        }

        // Add as member
        channelManager.addMember(channelId, playerId, ChannelRole.MEMBER).getOrElse {
            return Result.failure(it)
        }

        // Set as active channel
        channelManager.setPlayerChannel(playerId, channelId)
        logger.info("Player $playerId joined channel $channelId")
        return Result.success(Unit)
    }

    /**
     * Clears the player's active channel without removing them from channel membership.
     * The player remains a member of the channel and can rejoin by using the join command.
     *
     * @param playerId The UUID of the player.
     * @return Result indicating success or failure.
     * @throws ChannelNotMemberException if the player does not have an active channel.
     */
    fun leaveChannel(playerId: UUID): Result<Unit> {
        val currentChannel =
            channelManager.getPlayerChannel(playerId)
                ?: return Result.failure(
                    ChannelNotMemberException(playerId, "no active channel"),
                )

        // Clear active channel
        channelManager.setPlayerChannel(playerId, null)
        logger.info("Player $playerId left active channel $currentChannel (still a member)")
        return Result.success(Unit)
    }

    /**
     * Switches the player's active channel to a channel they are already a member of.
     *
     * @param playerId The UUID of the player.
     * @param channelId The ID of the channel to switch to.
     * @return Result indicating success or failure.
     * @throws ChannelNotFoundException if the channel does not exist.
     * @throws ChannelNotMemberException if the player is not a member of the channel.
     */
    fun switchChannel(
        playerId: UUID,
        channelId: String,
    ): Result<Unit> {
        // Check if channel exists
        val channel =
            channelManager.getChannel(channelId).getOrElse {
                return Result.failure(
                    ChannelNotFoundException(channelId),
                )
            }

        // Check if this channel is already active
        val currentActiveChannel = channelManager.getPlayerChannel(playerId)
        if (currentActiveChannel == channelId) {
            return Result.failure(
                ChannelAlreadyActiveException(playerId, channelId),
            )
        }

        // Check if player is a member
        val isAlreadyMember =
            isMember(playerId, channelId).getOrElse {
                return Result.failure(
                    ChannelRuntimeException("Failed to check membership for player $playerId in channel $channelId", it),
                )
            }

        if (!isAlreadyMember) {
            return Result.failure(
                ChannelNotMemberException(playerId, channelId),
            )
        }

        // Set as active channel
        channelManager.setPlayerChannel(playerId, channelId)
        logger.info("Player $playerId switched to channel $channelId")
        return Result.success(Unit)
    }

    /**
     * Gets all channels where the player is a member.
     *
     * @param playerId The UUID of the player.
     * @return Result containing a list of channel IDs where the player is a member.
     */
    fun getPlayerChannels(playerId: UUID): Result<List<String>> {
        val allChannels =
            channelManager.getAllChannels().getOrElse {
                return Result.failure(it)
            }

        val playerChannels =
            allChannels
                .filter { channel ->
                    isMember(playerId, channel.id).getOrElse { false }
                }.map { it.id }

        return Result.success(playerChannels)
    }
}
