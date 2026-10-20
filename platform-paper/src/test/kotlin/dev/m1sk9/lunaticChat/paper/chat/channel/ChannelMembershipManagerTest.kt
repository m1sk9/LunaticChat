package dev.m1sk9.lunaticChat.paper.chat.channel

import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelData
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
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
import dev.m1sk9.lunaticChat.engine.exception.ChannelTargetNotOutrankedException
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.TestUtils.createTestChannel
import dev.m1sk9.lunaticChat.paper.TestUtils.createTestUUID
import dev.m1sk9.lunaticChat.paper.config.key.ChannelChatFeatureConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.bukkit.Bukkit
import java.util.UUID
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
        playersWithBypass: Set<UUID> = emptySet(),
        now: () -> Long = { 0L },
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

        val membershipManager =
            ChannelMembershipManager(channelManager, logger, config, hasModerationBypass = { it in playersWithBypass }, now = now)
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
    fun `joinChannel admits an invited player to a private channel and uses up the invitation`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        val playerId = createTestUUID(2)
        channelManager.createChannel(
            createTestChannel(id = "priv-ch", name = "Private", ownerId = ownerId, isPrivate = true),
        )
        membership.inviteToChannel(ownerId, playerId, "priv-ch")

        assertTrue(membership.joinChannel(playerId, "priv-ch").isSuccess)
        assertEquals("priv-ch", channelManager.getPlayerChannel(playerId))
        assertFalse(membership.hasInvitation(playerId, "priv-ch"))
    }

    @Test
    fun `joinChannel refuses a private channel once the invitation has expired`() {
        var clock = 0L
        val (membership, channelManager, _) = createManagers(now = { clock })

        val ownerId = createTestUUID(1)
        val playerId = createTestUUID(2)
        channelManager.createChannel(
            createTestChannel(id = "priv-ch", name = "Private", ownerId = ownerId, isPrivate = true),
        )
        membership.inviteToChannel(ownerId, playerId, "priv-ch")
        clock = ChannelMembershipManager.INVITATION_LIFETIME.inWholeMilliseconds

        assertIs<ChannelPrivateRequiresInvitationException>(membership.joinChannel(playerId, "priv-ch").exceptionOrNull())
    }

    @Test
    fun `an invitation to one private channel does not open another`() {
        val (membership, channelManager, _) = createManagers()

        val ownerId = createTestUUID(1)
        val playerId = createTestUUID(2)
        channelManager.createChannel(createTestChannel(id = "priv-a", name = "A", ownerId = ownerId, isPrivate = true))
        channelManager.createChannel(createTestChannel(id = "priv-b", name = "B", ownerId = ownerId, isPrivate = true))
        membership.inviteToChannel(ownerId, playerId, "priv-a")

        assertIs<ChannelPrivateRequiresInvitationException>(membership.joinChannel(playerId, "priv-b").exceptionOrNull())
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

        val channels = membership.getPlayerChannels(playerId)
        assertEquals(2, channels.size)
        assertTrue(channels.contains("ch1"))
        assertTrue(channels.contains("ch2"))
    }

    @Test
    fun `banPlayer refuses a target holding the moderation bypass`() {
        val ownerId = createTestUUID(1)
        val protectedPlayer = createTestUUID(2)
        val (membership, channelManager, _) = createManagers(playersWithBypass = setOf(protectedPlayer))
        channelManager.createChannel(createTestChannel(id = "mod-ch", name = "Moderated", ownerId = ownerId))

        val result = membership.banPlayer(ownerId, protectedPlayer, "mod-ch")

        assertIs<ChannelPlayerBypassBanException>(result.exceptionOrNull())
        assertFalse(channelManager.isPlayerBanned("mod-ch", protectedPlayer).getOrThrow())
    }

    @Test
    fun `banPlayer bans a target without the bypass`() {
        val ownerId = createTestUUID(1)
        val playerId = createTestUUID(2)
        val (membership, channelManager, _) = createManagers()
        channelManager.createChannel(createTestChannel(id = "mod-ch", name = "Moderated", ownerId = ownerId))

        assertTrue(membership.banPlayer(ownerId, playerId, "mod-ch").isSuccess)
        assertTrue(channelManager.isPlayerBanned("mod-ch", playerId).getOrThrow())
    }

    @Test
    fun `kickPlayer refuses a target holding the moderation bypass`() {
        val ownerId = createTestUUID(1)
        val protectedPlayer = createTestUUID(2)
        val (membership, channelManager, _) = createManagers(playersWithBypass = setOf(protectedPlayer))
        channelManager.createChannel(createTestChannel(id = "mod-ch", name = "Moderated", ownerId = ownerId))
        membership.joinChannel(protectedPlayer, "mod-ch")

        val result = membership.kickPlayer(ownerId, protectedPlayer, "mod-ch")

        assertIs<ChannelPlayerBypassKickException>(result.exceptionOrNull())
        assertTrue(membership.isMember(protectedPlayer, "mod-ch").getOrThrow())
    }

    @Test
    fun `kickPlayer removes a target without the bypass`() {
        val ownerId = createTestUUID(1)
        val playerId = createTestUUID(2)
        val (membership, channelManager, _) = createManagers()
        channelManager.createChannel(createTestChannel(id = "mod-ch", name = "Moderated", ownerId = ownerId))
        membership.joinChannel(playerId, "mod-ch")

        assertTrue(membership.kickPlayer(ownerId, playerId, "mod-ch").isSuccess)
        assertFalse(membership.isMember(playerId, "mod-ch").getOrThrow())
    }

    @Test
    fun `by default the moderation bypass is read from the online player's permission`() {
        val ownerId = createTestUUID(1)
        val protectedPlayer = createTestUUID(2)
        val offlinePlayer = createTestUUID(3)
        val ordinaryPlayer = createTestUUID(4)
        val (_, channelManager, logger) = createManagers()
        val membership = ChannelMembershipManager(channelManager, logger, ChannelChatFeatureConfig(enabled = true))
        channelManager.createChannel(createTestChannel(id = "mod-ch", name = "Moderated", ownerId = ownerId))

        val online = TestUtils.createMockPlayer(uuid = protectedPlayer)
        every { online.hasPermission(LunaticChatPermissionNode.ChannelBypass.permissionNode) } returns true
        val ordinary = TestUtils.createMockPlayer(uuid = ordinaryPlayer)
        every { ordinary.hasPermission(any<String>()) } returns false
        mockkStatic(Bukkit::class)
        try {
            every { Bukkit.getPlayer(any<UUID>()) } returns null
            every { Bukkit.getPlayer(protectedPlayer) } returns online
            every { Bukkit.getPlayer(ordinaryPlayer) } returns ordinary

            assertIs<ChannelPlayerBypassBanException>(membership.banPlayer(ownerId, protectedPlayer, "mod-ch").exceptionOrNull())
            assertTrue(membership.banPlayer(ownerId, offlinePlayer, "mod-ch").isSuccess)
            assertTrue(membership.banPlayer(ownerId, ordinaryPlayer, "mod-ch").isSuccess)
        } finally {
            unmockkStatic(Bukkit::class)
        }
    }

    @Test
    fun `inviteToChannel refuses an actor inviting themselves`() {
        val ownerId = createTestUUID(1)
        val (membership, channelManager, _) = createManagers()
        channelManager.createChannel(createTestChannel(id = "mod-ch", name = "Moderated", ownerId = ownerId))

        val result = membership.inviteToChannel(ownerId, ownerId, "mod-ch")

        assertIs<ChannelCannotInviteSelfException>(result.exceptionOrNull())
    }

    @Test
    fun `inviteToChannel records an invitation without adding the guest or touching their active channel`() {
        val ownerId = createTestUUID(1)
        val guestId = createTestUUID(2)
        val (membership, channelManager, _) = createManagers()
        channelManager.createChannel(createTestChannel(id = "mod-ch", name = "Moderated", ownerId = ownerId).copy(isPrivate = true))
        channelManager.createChannel(createTestChannel(id = "own-ch", name = "Own", ownerId = guestId))

        assertTrue(membership.inviteToChannel(ownerId, guestId, "mod-ch").isSuccess)

        assertTrue(membership.hasInvitation(guestId, "mod-ch"))
        assertFalse(membership.isMember(guestId, "mod-ch").getOrThrow())
        assertEquals("own-ch", channelManager.getPlayerChannel(guestId))
    }

    @Test
    fun `inviteToChannel refuses a guest who is already a member`() {
        val ownerId = createTestUUID(1)
        val guestId = createTestUUID(2)
        val (membership, channelManager, _) = createManagers()
        channelManager.createChannel(createTestChannel(id = "mod-ch", name = "Moderated", ownerId = ownerId))
        membership.joinChannel(guestId, "mod-ch")

        assertIs<ChannelMemberAlreadyException>(membership.inviteToChannel(ownerId, guestId, "mod-ch").exceptionOrNull())
    }

    @Test
    fun `a ban withdraws an outstanding invitation`() {
        val ownerId = createTestUUID(1)
        val guestId = createTestUUID(2)
        val (membership, channelManager, _) = createManagers()
        channelManager.createChannel(createTestChannel(id = "mod-ch", name = "Moderated", ownerId = ownerId).copy(isPrivate = true))
        membership.inviteToChannel(ownerId, guestId, "mod-ch")

        membership.banPlayer(ownerId, guestId, "mod-ch")

        assertFalse(membership.hasInvitation(guestId, "mod-ch"))
    }

    @Test
    fun `a moderator cannot ban or kick the owner`() {
        val ownerId = createTestUUID(1)
        val modId = createTestUUID(2)
        val (membership, channelManager, _) = createManagers()
        channelManager.createChannel(createTestChannel(id = "mod-ch", name = "Moderated", ownerId = ownerId))
        channelManager.addMember("mod-ch", modId, ChannelRole.MODERATOR)

        assertIs<ChannelTargetNotOutrankedException>(membership.banPlayer(modId, ownerId, "mod-ch").exceptionOrNull())
        assertIs<ChannelTargetNotOutrankedException>(membership.kickPlayer(modId, ownerId, "mod-ch").exceptionOrNull())
        assertFalse(channelManager.isPlayerBanned("mod-ch", ownerId).getOrThrow())
        assertEquals(ChannelRole.OWNER, membership.getMemberRole(ownerId, "mod-ch").getOrThrow())
    }

    @Test
    fun `a moderator cannot ban or kick another moderator`() {
        val ownerId = createTestUUID(1)
        val modId = createTestUUID(2)
        val otherModId = createTestUUID(3)
        val (membership, channelManager, _) = createManagers()
        channelManager.createChannel(createTestChannel(id = "mod-ch", name = "Moderated", ownerId = ownerId))
        channelManager.addMember("mod-ch", modId, ChannelRole.MODERATOR)
        channelManager.addMember("mod-ch", otherModId, ChannelRole.MODERATOR)

        assertIs<ChannelTargetNotOutrankedException>(membership.banPlayer(modId, otherModId, "mod-ch").exceptionOrNull())
        assertIs<ChannelTargetNotOutrankedException>(membership.kickPlayer(modId, otherModId, "mod-ch").exceptionOrNull())
    }

    @Test
    fun `a moderator can ban a member and a non-member`() {
        val ownerId = createTestUUID(1)
        val modId = createTestUUID(2)
        val memberId = createTestUUID(3)
        val strangerId = createTestUUID(4)
        val (membership, channelManager, _) = createManagers()
        channelManager.createChannel(createTestChannel(id = "mod-ch", name = "Moderated", ownerId = ownerId))
        channelManager.addMember("mod-ch", modId, ChannelRole.MODERATOR)
        channelManager.addMember("mod-ch", memberId, ChannelRole.MEMBER)

        assertTrue(membership.banPlayer(modId, memberId, "mod-ch").isSuccess)
        assertTrue(membership.banPlayer(modId, strangerId, "mod-ch").isSuccess)
    }

    @Test
    fun `the owner can ban a moderator`() {
        val ownerId = createTestUUID(1)
        val modId = createTestUUID(2)
        val (membership, channelManager, _) = createManagers()
        channelManager.createChannel(createTestChannel(id = "mod-ch", name = "Moderated", ownerId = ownerId))
        channelManager.addMember("mod-ch", modId, ChannelRole.MODERATOR)

        assertTrue(membership.banPlayer(ownerId, modId, "mod-ch").isSuccess)
    }

    @Test
    fun `the owner stays protected even after losing their member entry`() {
        val ownerId = createTestUUID(1)
        val modId = createTestUUID(2)
        val (membership, channelManager, _) = createManagers()
        channelManager.createChannel(createTestChannel(id = "mod-ch", name = "Moderated", ownerId = ownerId))
        channelManager.addMember("mod-ch", modId, ChannelRole.MODERATOR)
        channelManager.removeMember("mod-ch", ownerId)

        assertIs<ChannelTargetNotOutrankedException>(membership.banPlayer(modId, ownerId, "mod-ch").exceptionOrNull())
    }

    @Test
    fun `inviteToChannel still refuses a banned guest`() {
        val ownerId = createTestUUID(1)
        val bannedId = createTestUUID(2)
        val (membership, channelManager, _) = createManagers()
        channelManager.createChannel(createTestChannel(id = "mod-ch", name = "Moderated", ownerId = ownerId))
        membership.banPlayer(ownerId, bannedId, "mod-ch")

        val result = membership.inviteToChannel(ownerId, bannedId, "mod-ch")

        assertIs<ChannelPlayerBannedException>(result.exceptionOrNull())
    }

    @Test
    fun `isMember should fail for a channel that does not exist`() {
        val (membership, _, _) = createManagers()

        val result = membership.isMember(createTestUUID(1), "no-such-ch")

        assertIs<ChannelNotFoundException>(result.exceptionOrNull())
    }

    @Test
    fun `getPlayerChannels should not report a deleted channel`() {
        val ownerId = createTestUUID(1)
        val playerId = createTestUUID(2)
        val (membership, channelManager, _) = createManagers()
        channelManager.createChannel(createTestChannel(id = "keep-ch", name = "Keep", ownerId = ownerId))
        channelManager.createChannel(createTestChannel(id = "drop-ch", name = "Drop", ownerId = ownerId))
        membership.joinChannel(playerId, "keep-ch")
        channelManager.setPlayerChannel(playerId, null)
        membership.joinChannel(playerId, "drop-ch")

        channelManager.deleteChannel("drop-ch", ownerId)

        assertEquals(listOf("keep-ch"), membership.getPlayerChannels(playerId))
    }
}
