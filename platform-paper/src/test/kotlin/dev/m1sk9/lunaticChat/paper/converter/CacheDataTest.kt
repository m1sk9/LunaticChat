package dev.m1sk9.lunaticChat.paper.converter

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class CacheDataTest {
    private val json = Json

    @Test
    fun `CacheData should store version and entries`() {
        val data = CacheData(version = "1.0", entries = mapOf("hello" to "こんにちは"))
        assertEquals("1.0", data.version)
        assertEquals("こんにちは", data.entries["hello"])
    }

    @Test
    fun `CacheData serialization round-trip should preserve all fields`() {
        val original =
            CacheData(
                version = "2.0",
                entries = mapOf("hello" to "こんにちは", "world" to "世界"),
            )

        val serialized = json.encodeToString(CacheData.serializer(), original)
        val deserialized = json.decodeFromString(CacheData.serializer(), serialized)

        assertEquals(original.version, deserialized.version)
        assertEquals(original.entries, deserialized.entries)
    }
}
