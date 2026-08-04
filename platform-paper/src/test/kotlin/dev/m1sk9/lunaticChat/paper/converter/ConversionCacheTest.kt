package dev.m1sk9.lunaticChat.paper.converter

import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.storage.FileStore
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConversionCacheTest {
    @TempDir
    lateinit var directory: Path

    private fun withCacheFile(block: (Path) -> Unit) = block(directory.resolve("cache.json"))

    private fun createCache(
        cacheFile: Path,
        maxEntries: Int = 500,
    ) = ConversionCache(
        FileStore(cacheFile, TestUtils.ManualScheduler(), TestUtils.TestLogger()),
        maxEntries,
        TestUtils.TestLogger(),
    )

    @Test
    fun `entries survive a save and reload`() =
        withCacheFile { cacheFile ->
            createCache(cacheFile).apply {
                put("konnichiwa", "こんにちは")
                put("ohayou", "おはよう")
                saveToDisk()
            }

            val reloaded = createCache(cacheFile).apply { loadFromDisk() }

            assertEquals("こんにちは", reloaded.get("konnichiwa"))
            assertEquals("おはよう", reloaded.get("ohayou"))
        }

    @Test
    fun `saving leaves no temporary file beside the cache file`() =
        withCacheFile { cacheFile ->
            createCache(cacheFile).apply {
                put("konnichiwa", "こんにちは")
                saveToDisk()
            }

            assertEquals(listOf(cacheFile), cacheFile.parent.listDirectoryEntries())
        }

    @Test
    fun `a missing cache file is created empty`() =
        withCacheFile { cacheFile ->
            val cache = createCache(cacheFile)

            cache.loadFromDisk()

            assertTrue(Files.exists(cacheFile))
            assertNull(cache.get("konnichiwa"))
        }

    @Test
    fun `a cache written by an incompatible version is discarded`() =
        withCacheFile { cacheFile ->
            cacheFile.writeText("""{"version":"0","entries":{"konnichiwa":"こんにちは"}}""")
            val cache = createCache(cacheFile)

            cache.loadFromDisk()

            assertNull(cache.get("konnichiwa"))
        }

    @Test
    fun `an unparseable cache file is discarded rather than failing the load`() =
        withCacheFile { cacheFile ->
            cacheFile.writeText("{ this is not json")
            val cache = createCache(cacheFile)

            cache.loadFromDisk()

            assertNull(cache.get("konnichiwa"))
        }

    @Test
    fun `an unchanged cache is not rewritten`() =
        withCacheFile { cacheFile ->
            val cache =
                createCache(cacheFile).apply {
                    put("konnichiwa", "こんにちは")
                    saveToDisk()
                }
            // The periodic task fires on a fixed interval whether or not anyone chatted, so a clean
            // cache must cost nothing.
            cacheFile.writeText("sentinel")

            cache.saveToDisk()

            assertEquals("sentinel", cacheFile.readText())
        }

    @Test
    fun `a change since the last save is written`() =
        withCacheFile { cacheFile ->
            val cache =
                createCache(cacheFile).apply {
                    put("konnichiwa", "こんにちは")
                    saveToDisk()
                }
            cacheFile.writeText("sentinel")

            cache.put("ohayou", "おはよう")
            cache.saveToDisk()

            assertNotEquals("sentinel", cacheFile.readText())
            assertEquals("おはよう", createCache(cacheFile).apply { loadFromDisk() }.get("ohayou"))
        }

    @Test
    fun `the cache stays within its entry limit`() =
        withCacheFile { cacheFile ->
            val cache = createCache(cacheFile, maxEntries = 10)

            repeat(20) { cache.put("word$it", "変換$it") }
            cache.saveToDisk()

            val entries = createCache(cacheFile).apply { loadFromDisk() }
            assertEquals("変換19", entries.get("word19"))
            assertTrue((0 until 20).count { entries.get("word$it") != null } <= 10)
        }
}
