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
    fun `UUIDSerializer should fail on invalid UUID string`() {
        val jsonString = """{"uuid":"not-a-valid-uuid"}"""

        assertFailsWith<Exception> {
            json.decodeFromString(UUIDHolder.serializer(), jsonString)
        }
    }
}
