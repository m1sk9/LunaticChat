package dev.m1sk9.lunaticChat.paper.chat.channel

import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelData
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.exception.ChannelAlreadyActiveException
import dev.m1sk9.lunaticChat.engine.exception.ChannelMemberAlreadyException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotMemberException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerBannedException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerMembershipLimitExceededException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPrivateRequiresInvitationException
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.TestUtils.createTestChannel
import dev.m1sk9.lunaticChat.paper.TestUtils.createTestUUID
import dev.m1sk9.lunaticChat.paper.config.key.ChannelChatFeatureConfig
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChannelMembershipManagerTest {
    private fun createManagers(
        initialData: ChannelData = ChannelData(),
        maxChannelsPerServer: Int = 10,
        maxMembersPerChannel: Int = 50,
        maxMembershipPerPlayer: Int = 5,
    ): Triple<ChannelMembershipManager, ChannelManager, TestUtils.TestLogger> {
        val logger = TestUtils.TestLogger()
        val storage = mockk<ChannelStorage>(relaxed = true)
        val config =
            ChannelChatFeatureConfig(
                enabled = true,
                maxChannelsPerServer = maxChannelsPerServer,
                maxMembersPerChannel = maxMembersPerChannel,
                maxMembershipPerPlayer = maxMembershipPerPlayer,
            )

        every { storage.loadFromDisk() } returns initialData

        val channelManager = ChannelManager(storage, logger, config)
        channelManager.initialize()

        val membershipManager = ChannelMembershipManager(channelManager, logger, config)
        return Triple(membershipManager, channelManager, logger)
    }

    @Test
    fun `joinChannel should succeed for public channel`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        val playerId = createTestUUID(2)
        channelManager.createChannel(createTestChannel(id = "pub-ch", name = "Public", ownerId = ownerId))

        val result = membership.joinChannel(playerId, "pub-ch")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `joinChannel should set channel as active`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        val playerId = createTestUUID(2)
        channelManager.createChannel(createTestChannel(id = "pub-ch", name = "Public", ownerId = ownerId))

        membership.joinChannel(playerId, "pub-ch")
        assertEquals("pub-ch", channelManager.getPlayerChannel(playerId))
    }

    @Test
    fun `joinChannel should fail for nonexistent channel`() {
        val (membership, _, _) = createManagers()

        val result = membership.joinChannel(createTestUUID(1), "nonexistent")
        assertTrue(result.isFailure)
        assertIs<ChannelNotFoundException>(result.exceptionOrNull())
    }

    @Test
    fun `joinChannel should fail if channel is already active`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        channelManager.createChannel(createTestChannel(id = "active-ch", name = "Active", ownerId = ownerId))

        // Owner already has this as active channel after creation
        val result = membership.joinChannel(ownerId, "active-ch")
        assertTrue(result.isFailure)
        assertIs<ChannelAlreadyActiveException>(result.exceptionOrNull())
    }

    @Test
    fun `joinChannel should fail if player is banned`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        val bannedId = createTestUUID(2)
        channelManager.createChannel(createTestChannel(id = "ban-ch", name = "Channel", ownerId = ownerId))
        channelManager.banPlayer("ban-ch", bannedId)

        val result = membership.joinChannel(bannedId, "ban-ch")
        assertTrue(result.isFailure)
        assertIs<ChannelPlayerBannedException>(result.exceptionOrNull())
    }

    @Test
    fun `joinChannel should fail for private channel without bypass`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        val playerId = createTestUUID(2)
        channelManager.createChannel(
            createTestChannel(id = "priv-ch", name = "Private", ownerId = ownerId, isPrivate = true),
        )

        val result = membership.joinChannel(playerId, "priv-ch")
        assertTrue(result.isFailure)
        assertIs<ChannelPrivateRequiresInvitationException>(result.exceptionOrNull())
    }

    @Test
    fun `joinChannel should succeed for private channel with bypass`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        val playerId = createTestUUID(2)
        channelManager.createChannel(
            createTestChannel(id = "priv-ch", name = "Private", ownerId = ownerId, isPrivate = true),
        )

        val result = membership.joinChannel(playerId, "priv-ch", bypassPrivateCheck = true)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `joinChannel should fail if already a member`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        val playerId = createTestUUID(2)
        channelManager.createChannel(createTestChannel(id = "mem-ch", name = "Channel", ownerId = ownerId))
        membership.joinChannel(playerId, "mem-ch")

        // Switch active to another channel first so we can try joining again
        channelManager.createChannel(createTestChannel(id = "other-ch", name = "Other", ownerId = createTestUUID(3)))
        channelManager.setPlayerChannel(playerId, "other-ch")

        val result = membership.joinChannel(playerId, "mem-ch")
        assertTrue(result.isFailure)
        assertIs<ChannelMemberAlreadyException>(result.exceptionOrNull())
    }

    @Test
    fun `joinChannel should fail when membership limit reached`() {
        val (membership, channelManager, _) = createManagers(maxMembershipPerPlayer = 1)

        val ownerId1 = createTestUUID(1)
        val ownerId2 = createTestUUID(2)
        val playerId = createTestUUID(3)

        channelManager.createChannel(createTestChannel(id = "ch1", name = "Ch1", ownerId = ownerId1))
        channelManager.createChannel(createTestChannel(id = "ch2", name = "Ch2", ownerId = ownerId2))

        membership.joinChannel(playerId, "ch1")

        // Clear active channel to try joining second
        channelManager.setPlayerChannel(playerId, null)

        val result = membership.joinChannel(playerId, "ch2")
        assertTrue(result.isFailure)
        assertIs<ChannelPlayerMembershipLimitExceededException>(result.exceptionOrNull())
    }

    @Test
    fun `leaveChannel should remove from membership and clear active`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        val playerId = createTestUUID(2)
        channelManager.createChannel(createTestChannel(id = "leave-ch", name = "Channel", ownerId = ownerId))
        membership.joinChannel(playerId, "leave-ch")

        val result = membership.leaveChannel(playerId)
        assertTrue(result.isSuccess)
        assertNull(channelManager.getPlayerChannel(playerId))
    }

    @Test
    fun `leaveChannel should fail if no active channel`() {
        val (membership, _, _) = createManagers()

        val result = membership.leaveChannel(createTestUUID(99))
        assertTrue(result.isFailure)
        assertIs<ChannelNotMemberException>(result.exceptionOrNull())
    }

    @Test
    fun `switchChannel should switch to another member channel`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId1 = createTestUUID(1)
        val ownerId2 = createTestUUID(2)
        val playerId = createTestUUID(3)

        channelManager.createChannel(createTestChannel(id = "ch1", name = "Ch1", ownerId = ownerId1))
        channelManager.createChannel(createTestChannel(id = "ch2", name = "Ch2", ownerId = ownerId2))

        membership.joinChannel(playerId, "ch1")
        // Now switch active to ch2 first by joining
        channelManager.setPlayerChannel(playerId, null)
        membership.joinChannel(playerId, "ch2")

        // Switch back to ch1
        val result = membership.switchChannel(playerId, "ch1")
        assertTrue(result.isSuccess)
        assertEquals("ch1", channelManager.getPlayerChannel(playerId))
    }

    @Test
    fun `switchChannel should fail for nonexistent channel`() {
        val (membership, _, _) = createManagers()

        val result = membership.switchChannel(createTestUUID(1), "nonexistent")
        assertTrue(result.isFailure)
        assertIs<ChannelNotFoundException>(result.exceptionOrNull())
    }

    @Test
    fun `switchChannel should fail if already active`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        val playerId = createTestUUID(2)
        channelManager.createChannel(createTestChannel(id = "sw-ch", name = "Channel", ownerId = ownerId))
        membership.joinChannel(playerId, "sw-ch")

        val result = membership.switchChannel(playerId, "sw-ch")
        assertTrue(result.isFailure)
        assertIs<ChannelAlreadyActiveException>(result.exceptionOrNull())
    }

    @Test
    fun `switchChannel should fail if not a member`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        val playerId = createTestUUID(2)
        channelManager.createChannel(createTestChannel(id = "sw-ch", name = "Channel", ownerId = ownerId))

        val result = membership.switchChannel(playerId, "sw-ch")
        assertTrue(result.isFailure)
        assertIs<ChannelNotMemberException>(result.exceptionOrNull())
    }

    @Test
    fun `isMember should return true for members`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        channelManager.createChannel(createTestChannel(id = "mem-ch", name = "Channel", ownerId = ownerId))

        assertTrue(membership.isMember(ownerId, "mem-ch").getOrThrow())
    }

    @Test
    fun `isMember should return false for non-members`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        channelManager.createChannel(createTestChannel(id = "mem-ch", name = "Channel", ownerId = ownerId))

        assertFalse(membership.isMember(createTestUUID(2), "mem-ch").getOrThrow())
    }

    @Test
    fun `getMemberRole should return role for member`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        channelManager.createChannel(createTestChannel(id = "role-ch", name = "Channel", ownerId = ownerId))

        assertEquals(ChannelRole.OWNER, membership.getMemberRole(ownerId, "role-ch").getOrThrow())
    }

    @Test
    fun `getMemberRole should fail for non-member`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        channelManager.createChannel(createTestChannel(id = "role-ch", name = "Channel", ownerId = ownerId))

        val result = membership.getMemberRole(createTestUUID(2), "role-ch")
        assertTrue(result.isFailure)
        assertIs<ChannelNotMemberException>(result.exceptionOrNull())
    }

    @Test
    fun `getMemberRoleOrNull should return null for non-member`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        channelManager.createChannel(createTestChannel(id = "role-ch", name = "Channel", ownerId = ownerId))

        assertNull(membership.getMemberRoleOrNull(createTestUUID(2), "role-ch"))
    }

    @Test
    fun `hasRole OWNER should return true only for owner`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        val memberId = createTestUUID(2)
        channelManager.createChannel(createTestChannel(id = "has-ch", name = "Channel", ownerId = ownerId))
        channelManager.addMember("has-ch", memberId, ChannelRole.MEMBER)

        assertTrue(membership.hasRole(ownerId, "has-ch", ChannelRole.OWNER).getOrThrow())
        assertFalse(membership.hasRole(memberId, "has-ch", ChannelRole.OWNER).getOrThrow())
    }

    @Test
    fun `hasRole MODERATOR should return true for moderator and owner`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        val modId = createTestUUID(2)
        val memberId = createTestUUID(3)
        channelManager.createChannel(createTestChannel(id = "has-ch", name = "Channel", ownerId = ownerId))
        channelManager.addMember("has-ch", modId, ChannelRole.MODERATOR)
        channelManager.addMember("has-ch", memberId, ChannelRole.MEMBER)

        assertTrue(membership.hasRole(ownerId, "has-ch", ChannelRole.MODERATOR).getOrThrow())
        assertTrue(membership.hasRole(modId, "has-ch", ChannelRole.MODERATOR).getOrThrow())
        assertFalse(membership.hasRole(memberId, "has-ch", ChannelRole.MODERATOR).getOrThrow())
    }

    @Test
    fun `hasRole MEMBER should return true for all members`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        val memberId = createTestUUID(2)
        channelManager.createChannel(createTestChannel(id = "has-ch", name = "Channel", ownerId = ownerId))
        channelManager.addMember("has-ch", memberId, ChannelRole.MEMBER)

        assertTrue(membership.hasRole(ownerId, "has-ch", ChannelRole.MEMBER).getOrThrow())
        assertTrue(membership.hasRole(memberId, "has-ch", ChannelRole.MEMBER).getOrThrow())
    }

    @Test
    fun `hasRole should return false for non-member`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        channelManager.createChannel(createTestChannel(id = "has-ch", name = "Channel", ownerId = ownerId))

        assertFalse(membership.hasRole(createTestUUID(99), "has-ch", ChannelRole.MEMBER).getOrThrow())
    }

    @Test
    fun `getPlayerChannels should return all channels player is member of`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId1 = createTestUUID(1)
        val ownerId2 = createTestUUID(2)
        val playerId = createTestUUID(3)

        channelManager.createChannel(createTestChannel(id = "ch1", name = "Ch1", ownerId = ownerId1))
        channelManager.createChannel(createTestChannel(id = "ch2", name = "Ch2", ownerId = ownerId2))

        membership.joinChannel(playerId, "ch1")
        channelManager.setPlayerChannel(playerId, null)
        membership.joinChannel(playerId, "ch2")

        val channels = membership.getPlayerChannels(playerId).getOrThrow()
        assertEquals(2, channels.size)
        assertTrue(channels.contains("ch1"))
        assertTrue(channels.contains("ch2"))
    }
}
