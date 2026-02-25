package dev.m1sk9.lunaticChat.engine.chat.channel

import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ChannelDataClassesTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val testOwnerId = UUID.fromString("00000001-0000-0000-0000-000000000000")
    private val testPlayerId = UUID.fromString("00000002-0000-0000-0000-000000000000")

    // --- ChannelRole ---

    @Test
    fun `ChannelRole should have 3 entries`() {
        assertEquals(3, ChannelRole.entries.size)
    }

    @Test
    fun `ChannelRole entries should be OWNER, MODERATOR, MEMBER`() {
        val expected = setOf("OWNER", "MODERATOR", "MEMBER")
        val actual = ChannelRole.entries.map { it.name }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun `ChannelRole serialization round-trip should preserve value`() {
        ChannelRole.entries.forEach { role ->
            val serialized = json.encodeToString(ChannelRole.serializer(), role)
            val deserialized = json.decodeFromString(ChannelRole.serializer(), serialized)
            assertEquals(role, deserialized)
        }
    }

    // --- ChannelMember ---

    @Test
    fun `ChannelMember should have default joinedAt`() {
        val before = System.currentTimeMillis()
        val member = ChannelMember(channelId = "ch-1", playerId = testPlayerId, role = ChannelRole.MEMBER)
        val after = System.currentTimeMillis()

        assertTrue(member.joinedAt in before..after)
    }

    @Test
    fun `ChannelMember serialization round-trip should preserve all fields`() {
        val original =
            ChannelMember(
                channelId = "ch-1",
                playerId = testPlayerId,
                role = ChannelRole.MODERATOR,
                joinedAt = 12345L,
            )

        val serialized = json.encodeToString(ChannelMember.serializer(), original)
        val deserialized = json.decodeFromString(ChannelMember.serializer(), serialized)

        assertEquals(original.channelId, deserialized.channelId)
        assertEquals(original.playerId, deserialized.playerId)
        assertEquals(original.role, deserialized.role)
        assertEquals(original.joinedAt, deserialized.joinedAt)
    }

    @Test
    fun `ChannelMember copy should allow role change`() {
        val original = ChannelMember(channelId = "ch-1", playerId = testPlayerId, role = ChannelRole.MEMBER)
        val promoted = original.copy(role = ChannelRole.MODERATOR)

        assertEquals(ChannelRole.MODERATOR, promoted.role)
        assertEquals(original.channelId, promoted.channelId)
        assertEquals(original.playerId, promoted.playerId)
        assertEquals(original.joinedAt, promoted.joinedAt)
    }

    // --- ChannelData ---

    @Test
    fun `ChannelData default values should be correct`() {
        val data = ChannelData()
        assertEquals(1, data.version)
        assertTrue(data.channels.isEmpty())
        assertTrue(data.members.isEmpty())
        assertTrue(data.activeChannels.isEmpty())
    }

    @Test
    fun `ChannelData serialization round-trip should preserve all fields`() {
        val channel = Channel(id = "ch-1", name = "Test Channel", ownerId = testOwnerId, createdAt = 1000L)
        val member = ChannelMember(channelId = "ch-1", playerId = testPlayerId, role = ChannelRole.MEMBER, joinedAt = 2000L)
        val original =
            ChannelData(
                version = 1,
                channels = mapOf("ch-1" to channel),
                members = mapOf("ch-1" to listOf(member)),
                activeChannels = mapOf("player1" to "ch-1"),
            )

        val serialized = json.encodeToString(ChannelData.serializer(), original)
        val deserialized = json.decodeFromString(ChannelData.serializer(), serialized)

        assertEquals(original.version, deserialized.version)
        assertEquals(original.channels.size, deserialized.channels.size)
        assertEquals(original.members.size, deserialized.members.size)
        assertEquals(original.activeChannels, deserialized.activeChannels)
    }

    // --- ChannelContext ---

    @Test
    fun `ChannelContext should store all fields`() {
        val channel = Channel(id = "ch-1", name = "Test", ownerId = testOwnerId)
        val member = ChannelMember(channelId = "ch-1", playerId = testPlayerId, role = ChannelRole.MEMBER)
        val context = ChannelContext(channelId = "ch-1", channel = channel, members = listOf(member))

        assertEquals("ch-1", context.channelId)
        assertEquals(channel, context.channel)
        assertEquals(1, context.members.size)
        assertEquals(testPlayerId, context.members[0].playerId)
    }

    @Test
    fun `ChannelContext copy should create independent instance`() {
        val channel = Channel(id = "ch-1", name = "Test", ownerId = testOwnerId)
        val original = ChannelContext(channelId = "ch-1", channel = channel, members = emptyList())
        val copied = original.copy(channelId = "ch-2")

        assertEquals("ch-2", copied.channelId)
        assertNotEquals(original.channelId, copied.channelId)
    }

    // --- ChannelMessageLogEntry ---

    @Test
    fun `ChannelMessageLogEntry create should convert UUID to string`() {
        val entry =
            ChannelMessageLogEntry.create(
                playerId = testPlayerId,
                playerName = "TestPlayer",
                channelId = "ch-1",
                message = "Hello world",
            )

        assertEquals(testPlayerId.toString(), entry.playerId)
        assertEquals("TestPlayer", entry.playerName)
        assertEquals("ch-1", entry.channelId)
        assertEquals("Hello world", entry.message)
    }

    @Test
    fun `ChannelMessageLogEntry create should set non-empty timestamp`() {
        val entry =
            ChannelMessageLogEntry.create(
                playerId = testPlayerId,
                playerName = "TestPlayer",
                channelId = "ch-1",
                message = "Hello",
            )

        assertTrue(entry.timestamp.isNotEmpty())
    }

    @Test
    fun `ChannelMessageLogEntry serialization round-trip should preserve all fields`() {
        val original =
            ChannelMessageLogEntry(
                timestamp = "2024-01-01T00:00:00Z",
                playerId = testPlayerId.toString(),
                playerName = "TestPlayer",
                channelId = "ch-1",
                message = "Test message",
            )

        val serialized = json.encodeToString(ChannelMessageLogEntry.serializer(), original)
        val deserialized = json.decodeFromString(ChannelMessageLogEntry.serializer(), serialized)

        assertEquals(original.timestamp, deserialized.timestamp)
        assertEquals(original.playerId, deserialized.playerId)
        assertEquals(original.playerName, deserialized.playerName)
        assertEquals(original.channelId, deserialized.channelId)
        assertEquals(original.message, deserialized.message)
    }

    @Test
    fun `ChannelMessageLogEntry create timestamp should be ISO-8601 format`() {
        val entry =
            ChannelMessageLogEntry.create(
                playerId = testPlayerId,
                playerName = "TestPlayer",
                channelId = "ch-1",
                message = "Hello",
            )

        // ISO-8601 timestamps contain 'T' separator
        assertTrue(entry.timestamp.contains("T"), "Timestamp should be ISO-8601 format: ${entry.timestamp}")
    }
}
