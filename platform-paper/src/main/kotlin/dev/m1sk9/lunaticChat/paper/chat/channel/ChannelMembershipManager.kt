package dev.m1sk9.lunaticChat.paper.chat.channel

import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.engine.debug.DebugLogger
import dev.m1sk9.lunaticChat.engine.exception.ChannelAlreadyActiveException
import dev.m1sk9.lunaticChat.engine.exception.ChannelCannotInviteSelfException
import dev.m1sk9.lunaticChat.engine.exception.ChannelMemberAlreadyException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotMemberException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerBannedException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerBypassBanException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerBypassKickException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerMembershipLimitExceededException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPrivateRequiresInvitationException
import dev.m1sk9.lunaticChat.engine.exception.ChannelRuntimeException
import dev.m1sk9.lunaticChat.engine.exception.ChannelTargetNotOutrankedException
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.common.has
import dev.m1sk9.lunaticChat.paper.config.key.ChannelChatFeatureConfig
import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Whether the player is currently online and holds the moderation bypass permission.
 * Offline players cannot be checked, matching how the permission is evaluated elsewhere.
 */
private fun hasChannelBypassPermission(playerId: UUID): Boolean =
    Bukkit.getPlayer(playerId)?.let(LunaticChatPermissionNode.ChannelBypass::has) == true

/**
 * @param hasModerationBypass Whether a player is exempt from being kicked or banned. Injected
 *   rather than read from Bukkit inline so this manager stays testable without a running server.
 * @param now The current time in milliseconds, injected so invitation expiry can be tested.
 */
class ChannelMembershipManager(
    private val channelManager: ChannelManager,
    private val logger: Logger,
    private val config: ChannelChatFeatureConfig,
    private val debug: DebugLogger = DebugLogger.Disabled,
    private val hasModerationBypass: (UUID) -> Boolean = ::hasChannelBypassPermission,
    private val now: () -> Long = System::currentTimeMillis,
) {
    companion object {
        /** How long an invitation can be accepted with `/lc channel join`. */
        val INVITATION_LIFETIME: Duration = 5.minutes
    }

    // Held in memory only: an invitation is meant to be taken up while both players are around,
    // so losing the outstanding ones on a restart costs no more than inviting again.
    private val invitations = ConcurrentHashMap<Pair<UUID, String>, Long>()

    /**
     * Names the refusal in the debug log, leaving the result untouched.
     *
     * The successful paths already log at INFO; a refusal only ever reached the player as a chat
     * message, so nothing in the server log said which of the several checks turned them away.
     */
    private fun Result<Unit>.reportRefusal(
        action: String,
        playerId: UUID,
        channelId: String,
    ): Result<Unit> =
        also {
            val failure = exceptionOrNull() ?: return@also
            debug.log(DebugCategory.CHANNEL) {
                "Refused $action of $channelId by $playerId: ${failure::class.simpleName}: ${failure.message}"
            }
        }

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
    ): Result<Boolean> = channelManager.isMember(channelId, playerId)

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
     * Gets the role of a member in a channel without throwing exceptions.
     *
     * @param playerId The UUID of the player.
     * @param channelId The ID of the channel.
     * @return The ChannelRole if the player is a member, null otherwise.
     */
    fun getMemberRoleOrNull(
        playerId: UUID,
        channelId: String,
    ): ChannelRole? =
        channelManager
            .getChannelMembers(channelId)
            .getOrNull()
            ?.find { it.playerId == playerId }
            ?.role

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
     * A private channel admits only a player holding an unexpired invitation, which joining uses up.
     *
     * @param playerId The UUID of the player.
     * @param channelId The ID of the channel.
     * @return Result indicating success or failure.
     * @throws ChannelNotFoundException if the channel does not exist.
     * @throws ChannelMemberAlreadyException if the player is already a member of this channel.
     * @throws ChannelPlayerMembershipLimitExceededException if the player has reached the maximum channel membership limit.
     * @throws ChannelRuntimeException for other runtime errors.
     */
    fun joinChannel(
        playerId: UUID,
        channelId: String,
    ): Result<Unit> = join(playerId, channelId).reportRefusal("join", playerId, channelId)

    private fun join(
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

        // Check if player is banned
        val isBanned = channelManager.isPlayerBanned(channelId, playerId).getOrElse { false }
        if (isBanned) {
            return Result.failure(
                ChannelPlayerBannedException(playerId, channelId),
            )
        }

        // Check if this is a private channel (only invited members can join)
        if (channel.isPrivate && !hasInvitation(playerId, channelId)) {
            return Result.failure(
                ChannelPrivateRequiresInvitationException(playerId, channelId),
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

        // Check if player has reached max membership limit (0 means unlimited)
        if (config.maxMembershipPerPlayer > 0) {
            val playerChannelCount = getPlayerChannels(playerId).size

            if (playerChannelCount >= config.maxMembershipPerPlayer) {
                return Result.failure(
                    ChannelPlayerMembershipLimitExceededException(playerId, config.maxMembershipPerPlayer),
                )
            }
        }

        // Add as member
        channelManager.addMember(channelId, playerId, ChannelRole.MEMBER).getOrElse {
            return Result.failure(it)
        }

        invitations.remove(playerId to channelId)

        // Set as active channel
        channelManager.setPlayerChannel(playerId, channelId)
        logger.info("Player $playerId joined channel $channelId")
        return Result.success(Unit)
    }

    /**
     * Removes the player from the active channel completely.
     * The player will be removed from the channel membership and the active channel will be cleared.
     *
     * @param playerId The UUID of the player.
     * @return Result indicating success or failure.
     * @throws ChannelNotMemberException if the player does not have an active channel.
     */
    fun leaveChannel(playerId: UUID): Result<Unit> = leave(playerId).reportRefusal("leave", playerId, "the active channel")

    private fun leave(playerId: UUID): Result<Unit> {
        val currentChannel =
            channelManager.getPlayerChannel(playerId)
                ?: return Result.failure(
                    ChannelNotMemberException(playerId, "no active channel"),
                )

        // Remove from channel members
        channelManager.removeMember(currentChannel, playerId).getOrElse {
            return Result.failure(it)
        }

        // Clear active channel
        channelManager.setPlayerChannel(playerId, null)
        logger.info("Player $playerId left channel $currentChannel (removed from members)")
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
    ): Result<Unit> = switchTo(playerId, channelId).reportRefusal("switch", playerId, channelId)

    private fun switchTo(
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
     * Bans [targetId] from [channelId] on [actorId]'s authority.
     *
     * The moderation rules live here rather than in the ban command so that every path to a ban -
     * commands today, anything else later - is held to them.
     *
     * @return Result indicating success or failure.
     * @throws ChannelTargetNotOutrankedException if [actorId] does not hold a higher role than the target.
     * @throws ChannelPlayerBypassBanException if the target is exempt from moderation.
     */
    fun banPlayer(
        actorId: UUID,
        targetId: UUID,
        channelId: String,
    ): Result<Unit> {
        refuseUnlessOutranked(actorId, targetId, channelId)?.let { return it.reportRefusal("ban", targetId, channelId) }
        if (hasModerationBypass(targetId)) {
            return Result.failure<Unit>(ChannelPlayerBypassBanException(targetId, channelId)).reportRefusal("ban", targetId, channelId)
        }
        invitations.remove(targetId to channelId)
        return channelManager.banPlayer(channelId, targetId).map { }.reportRefusal("ban", targetId, channelId)
    }

    /**
     * Removes [targetId] from [channelId] as a moderation action on [actorId]'s authority.
     *
     * @return Result indicating success or failure.
     * @throws ChannelTargetNotOutrankedException if [actorId] does not hold a higher role than the target.
     * @throws ChannelPlayerBypassKickException if the target is exempt from moderation.
     */
    fun kickPlayer(
        actorId: UUID,
        targetId: UUID,
        channelId: String,
    ): Result<Unit> {
        refuseUnlessOutranked(actorId, targetId, channelId)?.let { return it.reportRefusal("kick", targetId, channelId) }
        if (hasModerationBypass(targetId)) {
            return Result.failure<Unit>(ChannelPlayerBypassKickException(targetId, channelId)).reportRefusal("kick", targetId, channelId)
        }
        return channelManager.removeMember(channelId, targetId).reportRefusal("kick", targetId, channelId)
    }

    /**
     * Null when [actorId] holds a strictly higher role than [targetId] in [channelId].
     *
     * The channel's owner ranks as OWNER even without a member entry, since a member entry lost to
     * an earlier kick must not leave the owner open to moderation by their own moderators.
     */
    private fun refuseUnlessOutranked(
        actorId: UUID,
        targetId: UUID,
        channelId: String,
    ): Result<Unit>? {
        val channel = channelManager.getChannel(channelId).getOrElse { return Result.failure(it) }

        fun rankOf(playerId: UUID): Int =
            if (playerId == channel.ownerId) {
                ChannelRole.OWNER.rank
            } else {
                getMemberRoleOrNull(playerId, channelId)?.rank ?: NON_MEMBER_RANK
            }
        if (rankOf(actorId) > rankOf(targetId)) return null
        return Result.failure(ChannelTargetNotOutrankedException(actorId, targetId, channelId))
    }

    /**
     * Records [actorId]'s invitation of [targetId] to [channelId], which the target accepts by
     * joining within [INVITATION_LIFETIME].
     *
     * The target is deliberately not added here: joining also makes the channel their active one,
     * so an invitation that took effect at once would let anyone who owns a channel redirect
     * another player's next message into it.
     *
     * @return Result indicating success or failure.
     * @throws ChannelCannotInviteSelfException if the actor invited themselves.
     * @throws ChannelPlayerBannedException if the target is banned from the channel.
     * @throws ChannelMemberAlreadyException if the target is already a member.
     */
    fun inviteToChannel(
        actorId: UUID,
        targetId: UUID,
        channelId: String,
    ): Result<Unit> = invite(actorId, targetId, channelId).reportRefusal("invite", actorId, channelId)

    private fun invite(
        actorId: UUID,
        targetId: UUID,
        channelId: String,
    ): Result<Unit> {
        if (actorId == targetId) return Result.failure(ChannelCannotInviteSelfException(actorId))
        channelManager.getChannel(channelId).getOrElse { return Result.failure(ChannelNotFoundException(channelId)) }
        if (channelManager.isPlayerBanned(channelId, targetId).getOrElse { false }) {
            return Result.failure(ChannelPlayerBannedException(targetId, channelId))
        }
        if (isMember(targetId, channelId).getOrElse { false }) {
            return Result.failure(ChannelMemberAlreadyException(targetId, channelId))
        }
        invitations[targetId to channelId] = now() + INVITATION_LIFETIME.inWholeMilliseconds
        logger.info("Player $actorId invited $targetId to channel $channelId")
        return Result.success(Unit)
    }

    /** Whether [playerId] holds an unexpired invitation to [channelId]. */
    fun hasInvitation(
        playerId: UUID,
        channelId: String,
    ): Boolean {
        val key = playerId to channelId
        val expiresAt = invitations[key] ?: return false
        if (now() < expiresAt) return true
        invitations.remove(key, expiresAt)
        return false
    }

    /**
     * Gets all channels where the player is a member.
     *
     * @param playerId The UUID of the player.
     * @return The channel IDs where the player is a member.
     */
    fun getPlayerChannels(playerId: UUID): List<String> = channelManager.channelIdsOf(playerId)
}

private const val NON_MEMBER_RANK = -1

private val ChannelRole.rank: Int
    get() =
        when (this) {
            ChannelRole.MEMBER -> 0
            ChannelRole.MODERATOR -> 1
            ChannelRole.OWNER -> 2
        }
