package dev.m1sk9.lunaticChat.paper.chat.channel

import dev.m1sk9.lunaticChat.engine.chat.channel.Channel
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelContext
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelData
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelMember
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.exception.ChannelNoOwnerPermissionException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Logger
import kotlin.collections.forEach

class ChannelManager(
    private val storage: ChannelStorage,
    private val logger: Logger,
) {
    private val channelsCache = ConcurrentHashMap<String, Channel>()
    private val membersCache = ConcurrentHashMap<String, CopyOnWriteArrayList<ChannelMember>>()
    private val activeChannels = ConcurrentHashMap<UUID, String>()

    /**
     * Initializes the ChannelManager by loading data from storage.
     */
    fun initialize() {
        val data = storage.loadFromDisk()
        channelsCache.putAll(data.channels)
        data.members.forEach { (channelId, members) ->
            membersCache[channelId] = CopyOnWriteArrayList(members)
        }
        data.activeChannels.forEach { (playerIdStr, channelId) ->
            try {
                val playerId = UUID.fromString(playerIdStr)
                activeChannels[playerId] = channelId
            } catch (e: IllegalArgumentException) {
                logger.warning("Invalid UUID in activeChannels: $playerIdStr")
            }
        }
        logger.info("ChannelManager initialized with ${channelsCache.size} channels and ${activeChannels.size} active channels.")
    }

    /**
     * Creates a new channel.
     *
     * @param channel The channel to create.
     * @return Result containing the created channel or an error if the channel already exists.
     * @throws ChannelNotFoundException if a channel with the same ID already exists.
     */
    fun createChannel(channel: Channel): Result<Channel> {
        if (channelsCache.containsKey(channel.id)) {
            return Result.failure(ChannelNotFoundException(channel.id))
        }

        channelsCache[channel.id] = channel

        val ownerMember =
            ChannelMember(
                channelId = channel.id,
                playerId = channel.ownerId,
                role = ChannelRole.OWNER,
            )
        membersCache[channel.id] = CopyOnWriteArrayList(listOf(ownerMember))

        // Set the owner's active channel
        setPlayerChannel(channel.ownerId, channel.id)

        saveToStorage()
        logger.info("Created new channel with ID ${channel.id}.")
        return Result.success(channel)
    }

    /**
     * Deletes a channel.
     *
     * @param channelId The ID of the channel to delete.
     * @param requesterId The ID of the player requesting the deletion.
     * @return Result indicating success or failure of the deletion.
     * @throws ChannelNotFoundException if the channel does not exist.
     */
    fun deleteChannel(
        channelId: String,
        requesterId: UUID,
    ): Result<Unit> {
        val channel =
            channelsCache[channelId]
                ?: return Result.failure(ChannelNotFoundException(channelId))

        if (channel.ownerId != requesterId) {
            return Result.failure(ChannelNoOwnerPermissionException(requesterId))
        }

        // Clear active channel for all players who have this channel active
        activeChannels.entries.removeIf { it.value == channelId }

        channelsCache.remove(channelId)
        membersCache.remove(channelId)

        saveToStorage()
        logger.info("Owner with ID $requesterId deleted channel with ID $channelId.")
        return Result.success(Unit)
    }

    /**
     * Retrieves a channel by its ID.
     *
     * @param channelId The ID of the channel to retrieve.
     * @return Result containing the channel or an error if not found.
     * @throws ChannelNotFoundException if the channel does not exist.
     */
    fun getChannel(channelId: String): Result<Channel> {
        val channel =
            channelsCache[channelId]
                ?: return Result.failure(ChannelNotFoundException(channelId))
        return Result.success(channel)
    }

    /**
     * Retrieves all channels.
     *
     * @return Result containing the list of all channels.
     */
    fun getAllChannels(): Result<List<Channel>> = Result.success(channelsCache.values.toList())

    /**
     * Retrieves all public channels.
     *
     * @return Result containing the list of public channels.
     */
    fun getPublicChannels(): Result<List<Channel>> {
        val channels =
            channelsCache.values
                .filter { !it.isPrivate }
                .sortedBy { it.name }
        return Result.success(channels)
    }

    /**
     * Retrieves members of a channel.
     *
     * @param channelId The ID of the channel.
     * @return Result containing the list of channel members or an error if the channel is not found.
     * @throws ChannelNotFoundException if the channel does not exist.
     */
    fun getChannelMembers(channelId: String): Result<List<ChannelMember>> {
        channelsCache[channelId]
            ?: return Result.failure(ChannelNotFoundException(channelId))

        val members = membersCache[channelId]?.toList() ?: emptyList()
        return Result.success(members)
    }

    /**
     * Adds a member to a channel.
     *
     * @param channelId The ID of the channel.
     * @param playerId The UUID of the player to add.
     * @param role The role of the new member.
     * @return Result indicating success or failure of the operation.
     * @throws ChannelNotFoundException if the channel does not exist.
     */
    fun addMember(
        channelId: String,
        playerId: UUID,
        role: ChannelRole,
    ): Result<Unit> {
        channelsCache[channelId]
            ?: return Result.failure(ChannelNotFoundException(channelId))

        val members =
            membersCache.getOrPut(channelId) {
                CopyOnWriteArrayList()
            }
        val newMember =
            ChannelMember(
                channelId = channelId,
                playerId = playerId,
                role = role,
                joinedAt = System.currentTimeMillis(),
            )

        members.add(newMember)
        saveToStorage()

        return Result.success(Unit)
    }

    /**
     * Removes a member from a channel.
     *
     * @param channelId The ID of the channel.
     * @param playerId The UUID of the player to remove.
     * @return Result indicating success or failure of the operation.
     * @throws ChannelNotFoundException if the channel does not exist.
     */
    fun removeMember(
        channelId: String,
        playerId: UUID,
    ): Result<Unit> {
        channelsCache[channelId]
            ?: return Result.failure(ChannelNotFoundException(channelId))

        val members =
            membersCache[channelId]
                ?: return Result.failure(ChannelNotFoundException(channelId))

        val removed = members.removeIf { it.playerId == playerId }
        if (!removed) {
            return Result.failure(ChannelNotFoundException(channelId))
        }

        saveToStorage()
        return Result.success(Unit)
    }

    /**
     * Saves the current state of channels and members to storage asynchronously.
     */
    private fun saveToStorage() {
        val data =
            ChannelData(
                channels = channelsCache.toMap(),
                members = membersCache.mapValues { it.value.toList() },
                activeChannels = activeChannels.mapKeys { it.key.toString() },
            )
        storage.queueAsyncSave(data)
        logger.fine("${channelsCache.size} channels queued for saving to storage.")
    }

    /**
     * Saves the current state of channels and members to storage synchronously.
     * Should only br called during server shutdown.
     */
    fun saveToDisk() {
        val data =
            ChannelData(
                channels = channelsCache.toMap(),
                members = membersCache.mapValues { it.value.toList() },
                activeChannels = activeChannels.mapKeys { it.key.toString() },
            )
        storage.saveToDisk(data)
    }

    /**
     * Gets the active channel of a player.
     *
     * @param playerId The UUID of the player.
     * @return The ID of the active channel or null if none is set.
     */
    fun getPlayerChannel(playerId: UUID): String? = activeChannels[playerId]

    /**
     * Gets the full channel context (channel and members) of a player's active channel.
     *
     * @param playerId The UUID of the player.
     * @return The ChannelContext of the active channel or null if none is set.
     */
    fun getPlayerChannelContext(playerId: UUID): ChannelContext? {
        val channelId = activeChannels[playerId] ?: return null
        val channel = channelsCache[channelId] ?: return null
        val members = membersCache[channelId]?.toList() ?: return null

        return ChannelContext(
            channelId = channelId,
            channel = channel,
            members = members,
        )
    }

    /**
     * Sets the active channel of a player.
     *
     * @param playerId The UUID of the player.
     * @param channelId The ID of the channel to set as active, or null to clear the active channel.
     */
    fun setPlayerChannel(
        playerId: UUID,
        channelId: String?,
    ) {
        if (channelId == null) {
            activeChannels.remove(playerId)
        } else {
            activeChannels[playerId] = channelId
        }
        saveToStorage()
    }
}
