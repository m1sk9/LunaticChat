package dev.m1sk9.lunaticChat.paper.chat.channel

import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelData
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelMember
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.exception.ChannelAlreadyExistsException
import dev.m1sk9.lunaticChat.engine.exception.ChannelLimitExceededException
import dev.m1sk9.lunaticChat.engine.exception.ChannelMemberLimitExceededException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNoOwnerPermissionException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerAlreadyBannedException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerNotBannedException
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.TestUtils.createTestChannel
import dev.m1sk9.lunaticChat.paper.TestUtils.createTestUUID
import dev.m1sk9.lunaticChat.paper.config.key.ChannelChatFeatureConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChannelManagerTest {
    private fun createManager(
        initialData: ChannelData = ChannelData(),
        maxChannelsPerServer: Int = 10,
        maxMembersPerChannel: Int = 50,
    ): Triple<ChannelManager, ChannelStorage, TestUtils.TestLogger> {
        val logger = TestUtils.TestLogger()
        val storage = mockk<ChannelStorage>(relaxed = true)
        val config =
            ChannelChatFeatureConfig(
                enabled = true,
                maxChannelsPerServer = maxChannelsPerServer,
                maxMembersPerChannel = maxMembersPerChannel,
            )

        every { storage.loadFromDisk() } returns initialData

        val manager = ChannelManager(storage, logger, config)
        return Triple(manager, storage, logger)
    }

    @Test
    fun `initialize should load channels from storage`() {
        val ownerId = createTestUUID(1)
        val channel = createTestChannel(id = "test-ch", name = "Test", ownerId = ownerId)
        val members =
            listOf(
                ChannelMember(channelId = "test-ch", playerId = ownerId, role = ChannelRole.OWNER),
            )
        val data =
            ChannelData(
                channels = mapOf("test-ch" to channel),
                members = mapOf("test-ch" to members),
            )

        val (manager, _, logger) = createManager(initialData = data)
        manager.initialize()

        val result = manager.getChannel("test-ch")
        assertTrue(result.isSuccess)
        assertEquals("test-ch", result.getOrThrow().id)
        assertTrue(logger.infoMessages.any { it.contains("1 channels") })
    }

    @Test
    fun `createChannel should succeed for new channel`() {
        val (manager, storage, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        val channel = createTestChannel(id = "new-ch", name = "New Channel", ownerId = ownerId)

        val result = manager.createChannel(channel)

        assertTrue(result.isSuccess)
        assertEquals("new-ch", result.getOrThrow().id)
        verify(atLeast = 1) { storage.queueAsyncSave(any()) }
    }

    @Test
    fun `createChannel should auto-add owner as member`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        val channel = createTestChannel(id = "new-ch", name = "New Channel", ownerId = ownerId)
        manager.createChannel(channel)

        val members = manager.getChannelMembers("new-ch")
        assertTrue(members.isSuccess)
        assertEquals(1, members.getOrThrow().size)
        assertEquals(ownerId, members.getOrThrow()[0].playerId)
        assertEquals(ChannelRole.OWNER, members.getOrThrow()[0].role)
    }

    @Test
    fun `createChannel should set owner active channel`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        val channel = createTestChannel(id = "new-ch", name = "New Channel", ownerId = ownerId)
        manager.createChannel(channel)

        assertEquals("new-ch", manager.getPlayerChannel(ownerId))
    }

    @Test
    fun `createChannel should fail for duplicate ID`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        val channel = createTestChannel(id = "dup-ch", name = "Channel", ownerId = ownerId)
        manager.createChannel(channel)

        val result = manager.createChannel(channel)
        assertTrue(result.isFailure)
        assertIs<ChannelAlreadyExistsException>(result.exceptionOrNull())
    }

    @Test
    fun `createChannel should fail when limit reached`() {
        val (manager, _, _) = createManager(maxChannelsPerServer = 1)
        manager.initialize()

        val owner1 = createTestUUID(1)
        val owner2 = createTestUUID(2)
        manager.createChannel(createTestChannel(id = "ch1", name = "Ch1", ownerId = owner1))

        val result = manager.createChannel(createTestChannel(id = "ch2", name = "Ch2", ownerId = owner2))
        assertTrue(result.isFailure)
        assertIs<ChannelLimitExceededException>(result.exceptionOrNull())
    }

    @Test
    fun `createChannel with unlimited limit should allow many channels`() {
        val (manager, _, _) = createManager(maxChannelsPerServer = 0)
        manager.initialize()

        repeat(5) { i ->
            val result =
                manager.createChannel(
                    createTestChannel(id = "ch-$i", name = "Channel $i", ownerId = createTestUUID(i)),
                )
            assertTrue(result.isSuccess)
        }
    }

    @Test
    fun `deleteChannel should succeed for owner`() {
        val (manager, storage, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        manager.createChannel(createTestChannel(id = "del-ch", name = "To Delete", ownerId = ownerId))

        val result = manager.deleteChannel("del-ch", ownerId)
        assertTrue(result.isSuccess)

        val getResult = manager.getChannel("del-ch")
        assertTrue(getResult.isFailure)
    }

    @Test
    fun `deleteChannel should fail for non-owner without bypass`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        val otherId = createTestUUID(2)
        manager.createChannel(createTestChannel(id = "del-ch", name = "Channel", ownerId = ownerId))

        val result = manager.deleteChannel("del-ch", otherId)
        assertTrue(result.isFailure)
        assertIs<ChannelNoOwnerPermissionException>(result.exceptionOrNull())
    }

    @Test
    fun `deleteChannel should succeed for non-owner with bypass permission`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        val adminId = createTestUUID(2)
        manager.createChannel(createTestChannel(id = "del-ch", name = "Channel", ownerId = ownerId))

        val result = manager.deleteChannel("del-ch", adminId, hasBypassPermission = true)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteChannel should fail for nonexistent channel`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val result = manager.deleteChannel("nonexistent", createTestUUID(1))
        assertTrue(result.isFailure)
        assertIs<ChannelNotFoundException>(result.exceptionOrNull())
    }

    @Test
    fun `deleteChannel should clear active channels for affected players`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        manager.createChannel(createTestChannel(id = "del-ch", name = "Channel", ownerId = ownerId))
        assertEquals("del-ch", manager.getPlayerChannel(ownerId))

        manager.deleteChannel("del-ch", ownerId)
        assertNull(manager.getPlayerChannel(ownerId))
    }

    @Test
    fun `getChannel should return channel when exists`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        manager.createChannel(createTestChannel(id = "get-ch", name = "Get Channel", ownerId = ownerId))

        val result = manager.getChannel("get-ch")
        assertTrue(result.isSuccess)
        assertEquals("Get Channel", result.getOrThrow().name)
    }

    @Test
    fun `getChannel should fail for nonexistent channel`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val result = manager.getChannel("nonexistent")
        assertTrue(result.isFailure)
        assertIs<ChannelNotFoundException>(result.exceptionOrNull())
    }

    @Test
    fun `getAllChannels should return all channels`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        manager.createChannel(createTestChannel(id = "ch1", name = "Ch1", ownerId = createTestUUID(1)))
        manager.createChannel(createTestChannel(id = "ch2", name = "Ch2", ownerId = createTestUUID(2)))

        val result = manager.getAllChannels()
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
    }

    @Test
    fun `getPublicChannels should exclude private channels`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        manager.createChannel(createTestChannel(id = "public-ch", name = "Public", ownerId = createTestUUID(1)))
        manager.createChannel(
            createTestChannel(id = "private-ch", name = "Private", ownerId = createTestUUID(2), isPrivate = true),
        )

        val result = manager.getPublicChannels()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        assertEquals("public-ch", result.getOrThrow()[0].id)
    }

    @Test
    fun `addMember should add member to channel`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        val memberId = createTestUUID(2)
        manager.createChannel(createTestChannel(id = "mem-ch", name = "Channel", ownerId = ownerId))

        val result = manager.addMember("mem-ch", memberId, ChannelRole.MEMBER)
        assertTrue(result.isSuccess)

        val members = manager.getChannelMembers("mem-ch").getOrThrow()
        assertEquals(2, members.size)
        assertTrue(members.any { it.playerId == memberId })
    }

    @Test
    fun `addMember should fail when member limit reached`() {
        val (manager, _, _) = createManager(maxMembersPerChannel = 1)
        manager.initialize()

        val ownerId = createTestUUID(1)
        manager.createChannel(createTestChannel(id = "lim-ch", name = "Channel", ownerId = ownerId))

        val result = manager.addMember("lim-ch", createTestUUID(2), ChannelRole.MEMBER)
        assertTrue(result.isFailure)
        assertIs<ChannelMemberLimitExceededException>(result.exceptionOrNull())
    }

    @Test
    fun `addMember should fail for nonexistent channel`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val result = manager.addMember("nonexistent", createTestUUID(1), ChannelRole.MEMBER)
        assertTrue(result.isFailure)
        assertIs<ChannelNotFoundException>(result.exceptionOrNull())
    }

    @Test
    fun `removeMember should remove member from channel`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        val memberId = createTestUUID(2)
        manager.createChannel(createTestChannel(id = "rem-ch", name = "Channel", ownerId = ownerId))
        manager.addMember("rem-ch", memberId, ChannelRole.MEMBER)

        val result = manager.removeMember("rem-ch", memberId)
        assertTrue(result.isSuccess)

        val members = manager.getChannelMembers("rem-ch").getOrThrow()
        assertFalse(members.any { it.playerId == memberId })
    }

    @Test
    fun `banPlayer should add to banned list and remove from members`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        val bannedId = createTestUUID(2)
        manager.createChannel(createTestChannel(id = "ban-ch", name = "Channel", ownerId = ownerId))
        manager.addMember("ban-ch", bannedId, ChannelRole.MEMBER)

        val result = manager.banPlayer("ban-ch", bannedId)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().bannedPlayers.contains(bannedId))

        val members = manager.getChannelMembers("ban-ch").getOrThrow()
        assertFalse(members.any { it.playerId == bannedId })
    }

    @Test
    fun `banPlayer should clear active channel for banned player`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        val bannedId = createTestUUID(2)
        manager.createChannel(createTestChannel(id = "ban-ch", name = "Channel", ownerId = ownerId))
        manager.addMember("ban-ch", bannedId, ChannelRole.MEMBER)
        manager.setPlayerChannel(bannedId, "ban-ch")

        manager.banPlayer("ban-ch", bannedId)
        assertNull(manager.getPlayerChannel(bannedId))
    }

    @Test
    fun `banPlayer should fail if already banned`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        val bannedId = createTestUUID(2)
        manager.createChannel(createTestChannel(id = "ban-ch", name = "Channel", ownerId = ownerId))
        manager.banPlayer("ban-ch", bannedId)

        val result = manager.banPlayer("ban-ch", bannedId)
        assertTrue(result.isFailure)
        assertIs<ChannelPlayerAlreadyBannedException>(result.exceptionOrNull())
    }

    @Test
    fun `unbanPlayer should remove from banned list`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        val bannedId = createTestUUID(2)
        manager.createChannel(createTestChannel(id = "unban-ch", name = "Channel", ownerId = ownerId))
        manager.banPlayer("unban-ch", bannedId)

        val result = manager.unbanPlayer("unban-ch", bannedId)
        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().bannedPlayers.contains(bannedId))
    }

    @Test
    fun `unbanPlayer should fail if not banned`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        manager.createChannel(createTestChannel(id = "unban-ch", name = "Channel", ownerId = ownerId))

        val result = manager.unbanPlayer("unban-ch", createTestUUID(2))
        assertTrue(result.isFailure)
        assertIs<ChannelPlayerNotBannedException>(result.exceptionOrNull())
    }

    @Test
    fun `isPlayerBanned should return true for banned player`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        val bannedId = createTestUUID(2)
        manager.createChannel(createTestChannel(id = "ban-ch", name = "Channel", ownerId = ownerId))
        manager.banPlayer("ban-ch", bannedId)

        assertTrue(manager.isPlayerBanned("ban-ch", bannedId).getOrThrow())
    }

    @Test
    fun `isPlayerBanned should return false for non-banned player`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        manager.createChannel(createTestChannel(id = "ban-ch", name = "Channel", ownerId = ownerId))

        assertFalse(manager.isPlayerBanned("ban-ch", createTestUUID(2)).getOrThrow())
    }

    @Test
    fun `updateMemberRole should change member role`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        val memberId = createTestUUID(2)
        manager.createChannel(createTestChannel(id = "role-ch", name = "Channel", ownerId = ownerId))
        manager.addMember("role-ch", memberId, ChannelRole.MEMBER)

        val result = manager.updateMemberRole("role-ch", memberId, ChannelRole.MODERATOR)
        assertTrue(result.isSuccess)

        val members = manager.getChannelMembers("role-ch").getOrThrow()
        val updatedMember = members.find { it.playerId == memberId }
        assertNotNull(updatedMember)
        assertEquals(ChannelRole.MODERATOR, updatedMember.role)
    }

    @Test
    fun `updateChannelOwner should transfer ownership`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val oldOwnerId = createTestUUID(1)
        val newOwnerId = createTestUUID(2)
        manager.createChannel(createTestChannel(id = "own-ch", name = "Channel", ownerId = oldOwnerId))
        manager.addMember("own-ch", newOwnerId, ChannelRole.MEMBER)

        val result = manager.updateChannelOwner("own-ch", newOwnerId)
        assertTrue(result.isSuccess)
        assertEquals(newOwnerId, result.getOrThrow().ownerId)

        val members = manager.getChannelMembers("own-ch").getOrThrow()
        assertEquals(ChannelRole.OWNER, members.find { it.playerId == newOwnerId }?.role)
        assertEquals(ChannelRole.MODERATOR, members.find { it.playerId == oldOwnerId }?.role)
    }

    @Test
    fun `setPlayerChannel should set and clear active channel`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val playerId = createTestUUID(1)

        manager.setPlayerChannel(playerId, "test-ch")
        assertEquals("test-ch", manager.getPlayerChannel(playerId))

        manager.setPlayerChannel(playerId, null)
        assertNull(manager.getPlayerChannel(playerId))
    }

    @Test
    fun `getPlayerChannelContext should return context for active channel`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        val ownerId = createTestUUID(1)
        manager.createChannel(createTestChannel(id = "ctx-ch", name = "Context Channel", ownerId = ownerId))

        val context = manager.getPlayerChannelContext(ownerId)
        assertNotNull(context)
        assertEquals("ctx-ch", context.channelId)
        assertEquals("Context Channel", context.channel.name)
    }

    @Test
    fun `getPlayerChannelContext should return null for no active channel`() {
        val (manager, _, _) = createManager()
        manager.initialize()

        assertNull(manager.getPlayerChannelContext(createTestUUID(99)))
    }

    @Test
    fun `saveToDisk should call storage saveToDisk`() {
        val (manager, storage, _) = createManager()
        manager.initialize()

        manager.saveToDisk()

        verify { storage.saveToDisk(any()) }
    }
}
