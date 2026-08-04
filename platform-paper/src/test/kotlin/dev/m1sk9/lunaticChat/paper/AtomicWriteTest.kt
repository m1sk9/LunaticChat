package dev.m1sk9.lunaticChat.paper

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AtomicWriteTest {
    private fun withTemporaryDirectory(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("atomic-write-test")
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `writes the content and leaves no temporary file behind`() =
        withTemporaryDirectory { directory ->
            val target = directory.resolve("channels.json")

            target.writeTextAtomically("""{"channels":[]}""")

            assertEquals("""{"channels":[]}""", target.readText())
            assertEquals(listOf(target), directory.listDirectoryEntries())
        }

    @Test
    fun `replaces existing content`() =
        withTemporaryDirectory { directory ->
            val target = directory.resolve("settings.yml")
            target.writeText("version: 1")

            target.writeTextAtomically("version: 2")

            assertEquals("version: 2", target.readText())
        }

    @Test
    fun `concurrent writers each publish a whole file rather than colliding`() =
        withTemporaryDirectory { directory ->
            val target = directory.resolve("channels.json")
            val writerCount = 8
            val contents = (1..writerCount).map { "content-$it".repeat(4_000) }
            val failures = ConcurrentLinkedQueue<Throwable>()
            val barrier = CyclicBarrier(writerCount)

            // A shutdown save and a still-pending debounced save can reach the same file at once.
            // With a shared temporary path they interleave there instead, and the losing move fails
            // with the temporary file already gone.
            val writers =
                contents.map { content ->
                    Thread {
                        barrier.await()
                        runCatching { target.writeTextAtomically(content) }
                            .onFailure { failures.add(it) }
                    }
                }
            writers.forEach { it.start() }
            writers.forEach { it.join() }

            assertTrue(failures.isEmpty(), "writes failed: ${failures.map { it.toString() }}")
            assertContains(contents, target.readText())
            assertEquals(listOf(target), directory.listDirectoryEntries())
            assertTrue(target.exists())
        }
}
