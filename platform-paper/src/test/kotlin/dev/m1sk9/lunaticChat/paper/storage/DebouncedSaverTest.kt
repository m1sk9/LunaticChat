package dev.m1sk9.lunaticChat.paper.storage

import dev.m1sk9.lunaticChat.paper.TestUtils
import kotlin.test.Test
import kotlin.test.assertEquals

class DebouncedSaverTest {
    @Test
    fun `a burst of requests costs one write`() {
        val scheduler = TestUtils.ManualScheduler()
        val saver = DebouncedSaver(scheduler)
        var writes = 0

        repeat(10) { saver.request { writes++ } }
        scheduler.runPending()

        assertEquals(1, writes)
    }

    @Test
    fun `the write runs at the scheduled time, not when it was requested`() {
        val scheduler = TestUtils.ManualScheduler()
        val saver = DebouncedSaver(scheduler)
        var writes = 0

        saver.request { writes++ }

        assertEquals(0, writes)
        scheduler.runPending()
        assertEquals(1, writes)
    }

    @Test
    fun `a request after the write lands is scheduled again`() {
        val scheduler = TestUtils.ManualScheduler()
        val saver = DebouncedSaver(scheduler)
        var writes = 0

        saver.request { writes++ }
        scheduler.runPending()
        saver.request { writes++ }
        scheduler.runPending()

        assertEquals(2, writes)
    }

    @Test
    fun `a saver serves one file only`() {
        val scheduler = TestUtils.ManualScheduler()
        val saver = DebouncedSaver(scheduler)
        val written = mutableListOf<String>()

        // Why FileStore owns one saver each: a request arriving while a write is pending is dropped,
        // so a shared saver would silently lose the second file's save.
        saver.request { written.add("channels.json") }
        saver.request { written.add("settings.yml") }
        scheduler.runPending()

        assertEquals(listOf("channels.json"), written)
    }
}
