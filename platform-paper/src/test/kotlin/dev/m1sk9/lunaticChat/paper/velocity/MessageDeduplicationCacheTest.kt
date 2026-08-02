package dev.m1sk9.lunaticChat.paper.velocity

import io.mockk.mockk
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageDeduplicationCacheTest {
    private fun cache(cacheSize: Int) = MessageDeduplicationCache(cacheSize, mockk<Logger>(relaxed = true), "test")

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
}
