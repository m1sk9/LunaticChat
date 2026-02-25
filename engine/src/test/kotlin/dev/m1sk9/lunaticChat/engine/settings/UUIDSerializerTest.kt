package dev.m1sk9.lunaticChat.engine.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UUIDSerializerTest {
    private val json = Json

    @Serializable
    private data class UUIDHolder(
        @Serializable(with = UUIDSerializer::class)
        val uuid: UUID,
    )

    @Serializable
    private data class UUIDAsStringHolder(
        @Serializable(with = UUIDASStringSerializer::class)
        val uuid: UUID,
    )

    @Test
    fun `UUIDSerializer should serialize UUID to string`() {
        val uuid = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        val holder = UUIDHolder(uuid)

        val jsonString = json.encodeToString(UUIDHolder.serializer(), holder)

        assert(jsonString.contains("12345678-1234-1234-1234-123456789abc"))
    }

    @Test
    fun `UUIDSerializer should deserialize string to UUID`() {
        val jsonString = """{"uuid":"12345678-1234-1234-1234-123456789abc"}"""
        val holder = json.decodeFromString(UUIDHolder.serializer(), jsonString)

        assertEquals(UUID.fromString("12345678-1234-1234-1234-123456789abc"), holder.uuid)
    }

    @Test
    fun `UUIDSerializer round-trip should preserve UUID`() {
        val originalUuid = UUID.randomUUID()
        val holder = UUIDHolder(originalUuid)

        val jsonString = json.encodeToString(UUIDHolder.serializer(), holder)
        val decoded = json.decodeFromString(UUIDHolder.serializer(), jsonString)

        assertEquals(originalUuid, decoded.uuid)
    }

    @Test
    fun `UUIDASStringSerializer should serialize UUID to string`() {
        val uuid = UUID.fromString("abcdef01-2345-6789-abcd-ef0123456789")
        val holder = UUIDAsStringHolder(uuid)

        val jsonString = json.encodeToString(UUIDAsStringHolder.serializer(), holder)

        assert(jsonString.contains("abcdef01-2345-6789-abcd-ef0123456789"))
    }

    @Test
    fun `UUIDASStringSerializer should deserialize string to UUID`() {
        val jsonString = """{"uuid":"abcdef01-2345-6789-abcd-ef0123456789"}"""
        val holder = json.decodeFromString(UUIDAsStringHolder.serializer(), jsonString)

        assertEquals(UUID.fromString("abcdef01-2345-6789-abcd-ef0123456789"), holder.uuid)
    }

    @Test
    fun `UUIDASStringSerializer round-trip should preserve UUID`() {
        val originalUuid = UUID.randomUUID()
        val holder = UUIDAsStringHolder(originalUuid)

        val jsonString = json.encodeToString(UUIDAsStringHolder.serializer(), holder)
        val decoded = json.decodeFromString(UUIDAsStringHolder.serializer(), jsonString)

        assertEquals(originalUuid, decoded.uuid)
    }

    @Test
    fun `UUIDSerializer should fail on invalid UUID string`() {
        val jsonString = """{"uuid":"not-a-valid-uuid"}"""

        assertFailsWith<Exception> {
            json.decodeFromString(UUIDHolder.serializer(), jsonString)
        }
    }

    @Test
    fun `UUIDASStringSerializer should fail on invalid UUID string`() {
        val jsonString = """{"uuid":"not-a-valid-uuid"}"""

        assertFailsWith<Exception> {
            json.decodeFromString(UUIDAsStringHolder.serializer(), jsonString)
        }
    }
}
