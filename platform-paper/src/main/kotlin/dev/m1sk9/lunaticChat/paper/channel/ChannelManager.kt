package dev.m1sk9.lunaticChat.paper.channel

import dev.m1sk9.lunaticChat.engine.channel.modal.Channel
import dev.m1sk9.lunaticChat.engine.channel.modal.ChannelData
import dev.m1sk9.lunaticChat.engine.channel.modal.ChannelMember
import dev.m1sk9.lunaticChat.engine.channel.modal.ChannelRole
import dev.m1sk9.lunaticChat.engine.exception.ChannelNoOwnerPermissionException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import io.ktor.util.collections.ConcurrentMap
import java.util.UUID
import java.util.logging.Logger
import kotlin.collections.forEach

class ChannelManager(
    private val storage: ChannelStorage,
    private val logger: Logger,
) {
    private val channelsCache = ConcurrentMap<String, Channel>()
    private val membersCache = ConcurrentMap<String, MutableList<ChannelMember>>()

    /**
     * Initializes the ChannelManager by loading data from storage.
     */
    fun initialize() {
        val data = storage.loadFromDisk()
        channelsCache.putAll(data.channels)
        data.members.forEach { (channelId, members) ->
            membersCache[channelId] = members.toMutableList()
        }
        logger.info("ChannelManager initialized with ${channelsCache.size} channels.")
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
        membersCache[channel.id] = mutableListOf(ownerMember)

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
        val channel =
            channelsCache[channelId]
                ?: return Result.failure(ChannelNotFoundException(channelId))

        val members = membersCache[channelId]?.toList() ?: emptyList()
        return Result.success(members)
    }

    /**
     * Saves the current state of channels and members to storage asynchronously.
     */
    private fun saveToStorage() {
        val data =
            ChannelData(
                channels = channelsCache.toMap(),
                members = membersCache.mapValues { it.value.toList() },
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
            )
        storage.saveToDisk(data)
    }
}
