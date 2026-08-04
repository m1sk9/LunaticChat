package dev.m1sk9.lunaticChat.paper.chat.channel

import dev.m1sk9.lunaticChat.engine.chat.channel.Channel
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelData
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelMember
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.exception.ChannelStorageLoadException
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.storage.FileStore
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChannelStorageTest {
    private val owner = UUID.fromString("00000001-0000-0000-0000-000000000000")

    private fun sampleData() =
        ChannelData(
            version = 1,
            channels = mapOf("general" to Channel(id = "general", name = "General", ownerId = owner, createdAt = 42)),
            members =
                mapOf(
                    "general" to
                        listOf(
                            ChannelMember(
                                channelId = "general",
                                playerId = owner,
                                role = ChannelRole.OWNER,
                                joinedAt = 42,
                            ),
                        ),
                ),
            activeChannels = mapOf(owner.toString() to "general"),
        )

    @TempDir
    lateinit var directory: Path

    private fun withStorage(block: (ChannelStorage, Path, TestUtils.ManualScheduler) -> Unit) {
        val channelsFile = directory.resolve("channels.json")
        val scheduler = TestUtils.ManualScheduler()
        val store = FileStore(channelsFile, scheduler, TestUtils.TestLogger())
        block(ChannelStorage(store, TestUtils.TestLogger()), channelsFile, scheduler)
    }

    @Test
    fun `saved data is loaded back unchanged`() =
        withStorage { storage, _, _ ->
            val data = sampleData()

            storage.saveToDisk(data)

            assertEquals(data, storage.loadFromDisk())
        }

    @Test
    fun `saving leaves no temporary file beside the channel file`() =
        withStorage { storage, channelsFile, _ ->
            storage.saveToDisk(sampleData())

            assertEquals(listOf(channelsFile), channelsFile.parent.listDirectoryEntries())
        }

    @Test
    fun `loading a missing file yields empty data rather than failing`() =
        withStorage { storage, _, _ ->
            assertEquals(ChannelData(), storage.loadFromDisk())
        }

    @Test
    fun `loading an unparseable file fails loudly`() =
        withStorage { storage, channelsFile, _ ->
            channelsFile.writeText("{ this is not json")

            assertFailsWith<ChannelStorageLoadException> { storage.loadFromDisk() }
        }

    @Test
    fun `unknown fields in the file are ignored`() =
        withStorage { storage, channelsFile, _ ->
            channelsFile.writeText("""{"version":1,"channels":{},"members":{},"activeChannels":{},"future":true}""")

            assertEquals(ChannelData(), storage.loadFromDisk())
        }

    @Test
    fun `a queued save reads the data when the write runs, not when it is queued`() =
        withStorage { storage, _, scheduler ->
            var supplied = false

            storage.queueAsyncSave {
                supplied = true
                sampleData()
            }

            assertEquals(1, scheduler.pendingCount)
            assertTrue(!supplied, "the snapshot must not be taken while queueing")

            scheduler.runPending()

            assertTrue(supplied)
            assertEquals(sampleData(), storage.loadFromDisk())
        }
}
