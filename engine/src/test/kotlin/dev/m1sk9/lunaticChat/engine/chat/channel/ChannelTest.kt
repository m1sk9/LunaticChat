package dev.m1sk9.lunaticChat.engine.chat.channel

import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChannelTest {
    private val testOwnerId = UUID.fromString("00000001-0000-0000-0000-000000000000")
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `valid channel ID should be accepted`() {
        val channel = Channel(id = "test-channel", name = "Test", ownerId = testOwnerId)
        assertEquals("test-channel", channel.id)
    }

    @Test
    fun `channel ID with underscores and hyphens should be accepted`() {
        val channel = Channel(id = "my_test-channel", name = "Test", ownerId = testOwnerId)
        assertEquals("my_test-channel", channel.id)
    }

    @Test
    fun `channel ID with exactly 3 characters should be accepted`() {
        val channel = Channel(id = "abc", name = "Test", ownerId = testOwnerId)
        assertEquals("abc", channel.id)
    }

    @Test
    fun `channel ID with exactly 30 characters should be accepted`() {
        val id = "a".repeat(30)
        val channel = Channel(id = id, name = "Test", ownerId = testOwnerId)
        assertEquals(id, channel.id)
    }

    @Test
    fun `channel ID with less than 3 characters should be rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Channel(id = "ab", name = "Test", ownerId = testOwnerId)
        }
    }

    @Test
    fun `channel ID with more than 30 characters should be rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Channel(id = "a".repeat(31), name = "Test", ownerId = testOwnerId)
        }
    }

    @Test
    fun `channel ID with spaces should be rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Channel(id = "test channel", name = "Test", ownerId = testOwnerId)
        }
    }

    @Test
    fun `channel ID with special characters should be rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Channel(id = "test@channel", name = "Test", ownerId = testOwnerId)
        }
    }

    @Test
    fun `empty channel ID should be rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Channel(id = "", name = "Test", ownerId = testOwnerId)
        }
    }

    @Test
    fun `blank channel name should be rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Channel(id = "valid-id", name = "   ", ownerId = testOwnerId)
        }
    }

    @Test
    fun `empty channel name should be rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Channel(id = "valid-id", name = "", ownerId = testOwnerId)
        }
    }

    @Test
    fun `default values should be correct`() {
        val channel = Channel(id = "test-ch", name = "Test Channel", ownerId = testOwnerId)
        assertNull(channel.description)
        assertFalse(channel.isPrivate)
        assertTrue(channel.bannedPlayers.isEmpty())
    }

    @Test
    fun `serialization round-trip should preserve all fields`() {
        val bannedPlayer = UUID.fromString("00000002-0000-0000-0000-000000000000")
        val original =
            Channel(
                id = "test-channel",
                name = "Test Channel",
                description = "A test channel",
                isPrivate = true,
                ownerId = testOwnerId,
                createdAt = 1000L,
                bannedPlayers = setOf(bannedPlayer),
            )

        val jsonString = json.encodeToString(Channel.serializer(), original)
        val decoded = json.decodeFromString(Channel.serializer(), jsonString)

        assertEquals(original.id, decoded.id)
        assertEquals(original.name, decoded.name)
        assertEquals(original.description, decoded.description)
        assertEquals(original.isPrivate, decoded.isPrivate)
        assertEquals(original.ownerId, decoded.ownerId)
        assertEquals(original.createdAt, decoded.createdAt)
        assertEquals(original.bannedPlayers, decoded.bannedPlayers)
    }

    @Test
    fun `CHANNEL_ID_PATTERN should match valid patterns`() {
        assertTrue("abc".matches(Channel.CHANNEL_ID_PATTERN))
        assertTrue("test-123".matches(Channel.CHANNEL_ID_PATTERN))
        assertTrue("my_channel".matches(Channel.CHANNEL_ID_PATTERN))
        assertTrue("ABC123".matches(Channel.CHANNEL_ID_PATTERN))
    }

    @Test
    fun `CHANNEL_ID_PATTERN should reject invalid patterns`() {
        assertFalse("ab".matches(Channel.CHANNEL_ID_PATTERN))
        assertFalse("".matches(Channel.CHANNEL_ID_PATTERN))
        assertFalse("test channel".matches(Channel.CHANNEL_ID_PATTERN))
        assertFalse("test@ch".matches(Channel.CHANNEL_ID_PATTERN))
    }
}
