package dev.m1sk9.lunaticChat.paper.velocity

import io.mockk.mockk
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageDeduplicationCacheTest {
    private var now = 0L

    private fun cache(cacheSize: Int) = MessageDeduplicationCache(cacheSize, mockk<Logger>(relaxed = true), "test") { now }

    @Test
    fun `an unseen message id is new`() {
        assertTrue(cache(10).isNew("m1"))
    }

    @Test
    fun `a recorded message id is no longer new`() {
        val cache = cache(10)

        cache.markProcessed("m1")

        assertFalse(cache.isNew("m1"))
    }

    @Test
    fun `recording one id does not mask another`() {
        val cache = cache(10)

        cache.markProcessed("m1")

        assertTrue(cache.isNew("m2"))
    }

    @Test
    fun `eviction keeps the cache from growing without bound`() {
        val cache = cache(4)

        repeat(100) { cache.markProcessed("m$it") }

        val remembered = (0 until 100).count { !cache.isNew("m$it") }
        assertTrue(remembered <= 4, "expected at most 4 retained entries, got $remembered")
    }

    @Test
    fun `eviction at capacity drops the oldest ids and keeps the newest`() {
        val cache = cache(3)

        (1..5).forEach { cache.markProcessed("m$it") }

        assertEquals(listOf(true, true, false, false, false), (1..5).map { cache.isNew("m$it") })
    }

    @Test
    fun `recording an id again makes it the newest`() {
        val cache = cache(2)
        cache.markProcessed("m1")
        cache.markProcessed("m2")

        cache.markProcessed("m1")
        cache.markProcessed("m3")

        assertEquals(listOf(false, true, false), listOf("m1", "m2", "m3").map { cache.isNew(it) })
    }

    @Test
    fun `an overflow also drops every expired id`() {
        val cache = cache(3)
        cache.markProcessed("old1")
        cache.markProcessed("old2")
        cache.markProcessed("old3")

        now = 60_001L
        cache.markProcessed("fresh")

        assertEquals(listOf(true, true, true, false), listOf("old1", "old2", "old3", "fresh").map { cache.isNew(it) })
    }

    @Test
    fun `ids below capacity are kept past expiry until the cache overflows`() {
        val cache = cache(3)
        cache.markProcessed("old")

        now = 60_001L
        cache.markProcessed("fresh")

        assertFalse(cache.isNew("old"))
    }
}
