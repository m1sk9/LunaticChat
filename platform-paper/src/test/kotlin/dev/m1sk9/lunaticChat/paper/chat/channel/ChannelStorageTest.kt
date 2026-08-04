package dev.m1sk9.lunaticChat.paper.chat.channel

import dev.m1sk9.lunaticChat.engine.chat.channel.Channel
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelData
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelMember
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.exception.ChannelStorageLoadException
import dev.m1sk9.lunaticChat.paper.DebouncedSaver
import dev.m1sk9.lunaticChat.paper.TestUtils
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.nio.file.Files
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

    private fun withStorage(block: (ChannelStorage, Path) -> Unit) {
        val directory = Files.createTempDirectory("channel-storage-test")
        try {
            val channelsFile = directory.resolve("channels.json")
            block(ChannelStorage(channelsFile, mockk(relaxed = true), TestUtils.TestLogger()), channelsFile)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `saved data is loaded back unchanged`() =
        withStorage { storage, _ ->
            val data = sampleData()

            storage.saveToDisk(data)

            assertEquals(data, storage.loadFromDisk())
        }

    @Test
    fun `saving leaves no temporary file beside the channel file`() =
        withStorage { storage, channelsFile ->
            storage.saveToDisk(sampleData())

            assertEquals(listOf(channelsFile), channelsFile.parent.listDirectoryEntries())
        }

    @Test
    fun `loading a missing file yields empty data rather than failing`() =
        withStorage { storage, _ ->
            assertEquals(ChannelData(), storage.loadFromDisk())
        }

    @Test
    fun `loading an unparseable file fails loudly`() =
        withStorage { storage, channelsFile ->
            channelsFile.writeText("{ this is not json")

            assertFailsWith<ChannelStorageLoadException> { storage.loadFromDisk() }
        }

    @Test
    fun `unknown fields in the file are ignored`() =
        withStorage { storage, channelsFile ->
            channelsFile.writeText("""{"version":1,"channels":{},"members":{},"activeChannels":{},"future":true}""")

            assertEquals(ChannelData(), storage.loadFromDisk())
        }

    @Test
    fun `a queued save reads the data when the write runs, not when it is queued`() {
        val directory = Files.createTempDirectory("channel-storage-test")
        try {
            val channelsFile = directory.resolve("channels.json")
            val saver = mockk<DebouncedSaver>(relaxed = true)
            val storage = ChannelStorage(channelsFile, saver, TestUtils.TestLogger())
            val queued = slot<() -> Unit>()
            var supplied = false

            storage.queueAsyncSave {
                supplied = true
                sampleData()
            }

            verify { saver.request(capture(queued)) }
            assertTrue(!supplied, "the snapshot must not be taken while queueing")

            queued.captured.invoke()

            assertTrue(supplied)
            assertEquals(sampleData(), storage.loadFromDisk())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
