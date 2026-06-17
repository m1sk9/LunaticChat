package dev.m1sk9.lunaticChat.paper.velocity

import dev.m1sk9.lunaticChat.engine.protocol.PresenceEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RemotePlayerRegistryTest {
    @Test
    fun `serverOf returns the server for a known player case-insensitively`() {
        val registry = RemotePlayerRegistry(localServerName = "lobby")
        registry.replaceAll(listOf(PresenceEntry("Alice", "survival")))

        assertEquals("survival", registry.serverOf("Alice"))
        assertEquals("survival", registry.serverOf("alice"))
    }

    @Test
    fun `serverOf returns null for an unknown player`() {
        val registry = RemotePlayerRegistry(localServerName = "lobby")
        registry.replaceAll(listOf(PresenceEntry("Alice", "survival")))

        assertNull(registry.serverOf("Bob"))
    }

    @Test
    fun `remotePlayers excludes players on the local server`() {
        val registry = RemotePlayerRegistry(localServerName = "lobby")
        registry.replaceAll(
            listOf(
                PresenceEntry("Alice", "survival"),
                PresenceEntry("Bob", "lobby"),
                PresenceEntry("Carol", "creative"),
            ),
        )

        val remoteNames = registry.remotePlayers().map { it.playerName }.toSet()
        assertEquals(setOf("Alice", "Carol"), remoteNames)
    }

    @Test
    fun `remotePlayers preserves original-cased names`() {
        val registry = RemotePlayerRegistry(localServerName = "lobby")
        registry.replaceAll(listOf(PresenceEntry("AliceCased", "survival")))

        assertEquals("AliceCased", registry.remotePlayers().single().playerName)
    }

    @Test
    fun `replaceAll replaces the entire roster`() {
        val registry = RemotePlayerRegistry(localServerName = "lobby")
        registry.replaceAll(listOf(PresenceEntry("Alice", "survival")))
        registry.replaceAll(listOf(PresenceEntry("Bob", "creative")))

        assertNull(registry.serverOf("Alice"))
        assertEquals("creative", registry.serverOf("Bob"))
    }

    @Test
    fun `clear empties the roster`() {
        val registry = RemotePlayerRegistry(localServerName = "lobby")
        registry.replaceAll(listOf(PresenceEntry("Alice", "survival")))
        registry.clear()

        assertNull(registry.serverOf("Alice"))
        assertTrue(registry.remotePlayers().isEmpty())
    }

    @Test
    fun `local server name match is case-insensitive`() {
        val registry = RemotePlayerRegistry(localServerName = "Lobby")
        registry.replaceAll(
            listOf(
                PresenceEntry("Alice", "lobby"),
                PresenceEntry("Bob", "survival"),
            ),
        )

        assertEquals(listOf("Bob"), registry.remotePlayers().map { it.playerName })
    }
}
