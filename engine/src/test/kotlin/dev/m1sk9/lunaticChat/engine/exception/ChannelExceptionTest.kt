package dev.m1sk9.lunaticChat.engine.exception

import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChannelExceptionTest {
    private val testPlayerId = UUID.fromString("00000001-0000-0000-0000-000000000000")
    private val testChannelId = "test-channel"

    // --- ChannelAlreadyActiveException ---

    @Test
    fun `ChannelAlreadyActiveException should contain playerId and channelId in message`() {
        val ex = ChannelAlreadyActiveException(testPlayerId, testChannelId)
        assertTrue(ex.message!!.contains(testPlayerId.toString()))
        assertTrue(ex.message!!.contains(testChannelId))
        assertEquals(testPlayerId, ex.playerId)
        assertEquals(testChannelId, ex.channelId)
    }

    // --- ChannelCannotInviteSelfException ---

    @Test
    fun `ChannelCannotInviteSelfException should contain playerId in message`() {
        val ex = ChannelCannotInviteSelfException(testPlayerId)
        assertTrue(ex.message!!.contains(testPlayerId.toString()))
    }

    // --- ChannelLimitExceededException ---

    @Test
    fun `ChannelLimitExceededException should contain limit in message`() {
        val ex = ChannelLimitExceededException(10)
        assertTrue(ex.message!!.contains("10"))
        assertEquals(10, ex.limit)
    }

    // --- ChannelMemberAlreadyException ---

    @Test
    fun `ChannelMemberAlreadyException should contain playerId and channelId in message`() {
        val ex = ChannelMemberAlreadyException(testPlayerId, testChannelId)
        assertTrue(ex.message!!.contains(testPlayerId.toString()))
        assertTrue(ex.message!!.contains(testChannelId))
        assertEquals(testChannelId, ex.channelId)
    }

    // --- ChannelMemberLimitExceededException ---

    @Test
    fun `ChannelMemberLimitExceededException should contain channelId and limit in message`() {
        val ex = ChannelMemberLimitExceededException(testChannelId, 50)
        assertTrue(ex.message!!.contains(testChannelId))
        assertTrue(ex.message!!.contains("50"))
        assertEquals(testChannelId, ex.channelId)
        assertEquals(50, ex.limit)
    }

    // --- ChannelMemberNotFoundException ---

    @Test
    fun `ChannelMemberNotFoundException should contain channelId in message`() {
        val ex = ChannelMemberNotFoundException(testChannelId)
        assertTrue(ex.message!!.contains(testChannelId))
    }

    // --- ChannelNoOwnerPermissionException ---

    @Test
    fun `ChannelNoOwnerPermissionException should contain ownerId in message`() {
        val ex = ChannelNoOwnerPermissionException(testPlayerId)
        assertTrue(ex.message!!.contains(testPlayerId.toString()))
    }

    // --- ChannelNotFoundException ---

    @Test
    fun `ChannelNotFoundException should contain channelId in message`() {
        val ex = ChannelNotFoundException(testChannelId)
        assertTrue(ex.message!!.contains(testChannelId))
    }

    // --- ChannelNotMemberException ---

    @Test
    fun `ChannelNotMemberException should contain playerId and channelId in message`() {
        val ex = ChannelNotMemberException(testPlayerId, testChannelId)
        assertTrue(ex.message!!.contains(testPlayerId.toString()))
        assertTrue(ex.message!!.contains(testChannelId))
    }

    // --- ChannelPlayerAlreadyBannedException ---

    @Test
    fun `ChannelPlayerAlreadyBannedException should contain playerId and channelId in message`() {
        val ex = ChannelPlayerAlreadyBannedException(testPlayerId, testChannelId)
        assertTrue(ex.message!!.contains(testPlayerId.toString()))
        assertTrue(ex.message!!.contains(testChannelId))
        assertEquals(testPlayerId, ex.playerId)
        assertEquals(testChannelId, ex.channelId)
    }

    // --- ChannelPlayerBannedException ---

    @Test
    fun `ChannelPlayerBannedException should contain playerId and channelId in message`() {
        val ex = ChannelPlayerBannedException(testPlayerId, testChannelId)
        assertTrue(ex.message!!.contains(testPlayerId.toString()))
        assertTrue(ex.message!!.contains(testChannelId))
        assertEquals(testChannelId, ex.channelId)
    }

    // --- ChannelPlayerBypassBanException ---

    @Test
    fun `ChannelPlayerBypassBanException should contain playerId in message and store channelId`() {
        val ex = ChannelPlayerBypassBanException(testPlayerId, testChannelId)
        assertTrue(ex.message!!.contains(testPlayerId.toString()))
        assertEquals(testChannelId, ex.channelId)
    }

    // --- ChannelPlayerBypassKickException ---

    @Test
    fun `ChannelPlayerBypassKickException should contain playerId in message and store channelId`() {
        val ex = ChannelPlayerBypassKickException(testPlayerId, testChannelId)
        assertTrue(ex.message!!.contains(testPlayerId.toString()))
        assertEquals(testChannelId, ex.channelId)
    }

    // --- ChannelPlayerMembershipLimitExceededException ---

    @Test
    fun `ChannelPlayerMembershipLimitExceededException should contain playerId and limit in message`() {
        val ex = ChannelPlayerMembershipLimitExceededException(testPlayerId, 5)
        assertTrue(ex.message!!.contains(testPlayerId.toString()))
        assertTrue(ex.message!!.contains("5"))
        assertEquals(testPlayerId, ex.playerId)
        assertEquals(5, ex.limit)
    }

    // --- ChannelPlayerNotBannedException ---

    @Test
    fun `ChannelPlayerNotBannedException should contain playerId and channelId in message`() {
        val ex = ChannelPlayerNotBannedException(testPlayerId, testChannelId)
        assertTrue(ex.message!!.contains(testPlayerId.toString()))
        assertTrue(ex.message!!.contains(testChannelId))
        assertEquals(testChannelId, ex.channelId)
    }

    // --- ChannelPrivateRequiresInvitationException ---

    @Test
    fun `ChannelPrivateRequiresInvitationException should contain playerId and channelId in message`() {
        val ex = ChannelPrivateRequiresInvitationException(testPlayerId, testChannelId)
        assertTrue(ex.message!!.contains(testPlayerId.toString()))
        assertTrue(ex.message!!.contains(testChannelId))
        assertEquals(testPlayerId, ex.playerId)
        assertEquals(testChannelId, ex.channelId)
    }

    // --- ChannelRuntimeException ---

    @Test
    fun `ChannelRuntimeException should preserve message`() {
        val ex = ChannelRuntimeException("runtime error")
        assertEquals("runtime error", ex.message)
        assertNull(ex.cause)
    }

    @Test
    fun `ChannelRuntimeException should preserve cause`() {
        val cause = RuntimeException("root cause")
        val ex = ChannelRuntimeException("runtime error", cause)
        assertEquals("runtime error", ex.message)
        assertEquals(cause, ex.cause)
    }

    // --- ChannelStorageLoadException ---

    @Test
    fun `ChannelStorageLoadException should preserve message`() {
        val ex = ChannelStorageLoadException("load error")
        assertEquals("load error", ex.message)
        assertNull(ex.cause)
    }

    @Test
    fun `ChannelStorageLoadException should preserve cause`() {
        val cause = RuntimeException("io error")
        val ex = ChannelStorageLoadException("load error", cause)
        assertEquals("load error", ex.message)
        assertEquals(cause, ex.cause)
    }

    // --- ChannelStorageSaveException ---

    @Test
    fun `ChannelStorageSaveException should preserve message`() {
        val ex = ChannelStorageSaveException("save error")
        assertEquals("save error", ex.message)
        assertNull(ex.cause)
    }

    @Test
    fun `ChannelStorageSaveException should preserve cause`() {
        val cause = RuntimeException("io error")
        val ex = ChannelStorageSaveException("save error", cause)
        assertEquals("save error", ex.message)
        assertEquals(cause, ex.cause)
    }

    // --- ChatModeStorageException ---

    @Test
    fun `ChatModeStorageException should preserve message`() {
        val ex = ChatModeStorageException("chatmode error")
        assertEquals("chatmode error", ex.message)
        assertNull(ex.cause)
    }

    @Test
    fun `ChatModeStorageException should preserve cause`() {
        val cause = RuntimeException("io error")
        val ex = ChatModeStorageException("chatmode error", cause)
        assertEquals("chatmode error", ex.message)
        assertEquals(cause, ex.cause)
    }

    // --- RequirePermissionException ---

    @Test
    fun `RequirePermissionException should join permission nodes in message`() {
        val permissions = listOf(LunaticChatPermissionNode.Lc, LunaticChatPermissionNode.Tell)
        val ex = RequirePermissionException(permissions)
        assertTrue(ex.message!!.contains("lunaticchat.command.lc"))
        assertTrue(ex.message!!.contains("lunaticchat.command.tell"))
    }

    @Test
    fun `RequirePermissionException with single permission should contain that node`() {
        val permissions = listOf(LunaticChatPermissionNode.Spy)
        val ex = RequirePermissionException(permissions)
        assertTrue(ex.message!!.contains("lunaticchat.spy"))
    }

    // --- All exceptions are instances of Exception ---

    @Test
    fun `all exceptions should be instances of Exception`() {
        assertIs<Exception>(ChannelAlreadyActiveException(testPlayerId, testChannelId))
        assertIs<Exception>(ChannelCannotInviteSelfException(testPlayerId))
        assertIs<Exception>(ChannelLimitExceededException(10))
        assertIs<Exception>(ChannelMemberAlreadyException(testPlayerId, testChannelId))
        assertIs<Exception>(ChannelMemberLimitExceededException(testChannelId, 50))
        assertIs<Exception>(ChannelMemberNotFoundException(testChannelId))
        assertIs<Exception>(ChannelNoOwnerPermissionException(testPlayerId))
        assertIs<Exception>(ChannelNotFoundException(testChannelId))
        assertIs<Exception>(ChannelNotMemberException(testPlayerId, testChannelId))
        assertIs<Exception>(ChannelPlayerAlreadyBannedException(testPlayerId, testChannelId))
        assertIs<Exception>(ChannelPlayerBannedException(testPlayerId, testChannelId))
        assertIs<Exception>(ChannelPlayerBypassBanException(testPlayerId, testChannelId))
        assertIs<Exception>(ChannelPlayerBypassKickException(testPlayerId, testChannelId))
        assertIs<Exception>(ChannelPlayerMembershipLimitExceededException(testPlayerId, 5))
        assertIs<Exception>(ChannelPlayerNotBannedException(testPlayerId, testChannelId))
        assertIs<Exception>(ChannelPrivateRequiresInvitationException(testPlayerId, testChannelId))
        assertIs<Exception>(ChannelRuntimeException("test"))
        assertIs<Exception>(ChannelStorageLoadException("test"))
        assertIs<Exception>(ChannelStorageSaveException("test"))
        assertIs<Exception>(ChatModeStorageException("test"))
        assertIs<Exception>(RequirePermissionException(listOf(LunaticChatPermissionNode.Lc)))
    }
}
