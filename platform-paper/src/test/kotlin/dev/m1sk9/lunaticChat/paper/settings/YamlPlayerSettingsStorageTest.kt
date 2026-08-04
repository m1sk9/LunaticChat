package dev.m1sk9.lunaticChat.paper.settings

import dev.m1sk9.lunaticChat.engine.settings.PlayerSettingsData
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.storage.FileStore
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YamlPlayerSettingsStorageTest {
    private val alice = UUID.fromString("00000001-0000-0000-0000-000000000000")
    private val bob = UUID.fromString("00000002-0000-0000-0000-000000000000")

    @TempDir
    lateinit var directory: Path

    private fun sampleData() =
        PlayerSettingsData(
            japaneseConversion = mapOf(alice to true, bob to false),
            directMessageNotification = mapOf(alice to false),
            channelMessageNotification = mapOf(bob to true),
        )

    private fun withStorage(block: (YamlPlayerSettingsStorage, Path, TestUtils.ManualScheduler, TestUtils.TestLogger) -> Unit) {
        val settingsFile = directory.resolve("player-settings.yaml")
        val scheduler = TestUtils.ManualScheduler()
        val logger = TestUtils.TestLogger()
        val store = FileStore(settingsFile, scheduler, logger)
        block(YamlPlayerSettingsStorage(store, logger), settingsFile, scheduler, logger)
    }

    @Test
    fun `saved settings are loaded back unchanged`() =
        withStorage { storage, _, _, _ ->
            val data = sampleData()

            storage.saveToDisk(data)

            assertEquals(data, storage.loadFromDisk())
        }

    @Test
    fun `saving leaves no temporary file beside the settings file`() =
        withStorage { storage, settingsFile, _, _ ->
            storage.saveToDisk(sampleData())

            assertEquals(listOf(settingsFile), settingsFile.parent.listDirectoryEntries())
        }

    @Test
    fun `a missing file loads as empty settings without complaint`() =
        withStorage { storage, _, _, logger ->
            assertEquals(PlayerSettingsData(), storage.loadFromDisk())
            assertTrue(logger.severeMessages.isEmpty())
        }

    @Test
    fun `an unparseable file falls back to empty settings rather than failing startup`() =
        withStorage { storage, settingsFile, _, logger ->
            settingsFile.writeText("japaneseConversion: [this is not a map")

            assertEquals(PlayerSettingsData(), storage.loadFromDisk())
            assertTrue(logger.severeMessages.any { it.contains("Failed to load settings file") })
        }

    @Test
    fun `a failed write leaves the previous file intact`() =
        withStorage { storage, settingsFile, _, logger ->
            storage.saveToDisk(sampleData())
            val saved = settingsFile.readText()

            // A directory where the temporary file has to be created cannot be written to.
            settingsFile.parent.toFile().setWritable(false)
            try {
                storage.saveToDisk(PlayerSettingsData(japaneseConversion = mapOf(alice to false)))
            } finally {
                settingsFile.parent.toFile().setWritable(true)
            }

            // Loading a torn file discards every player's settings, so a failed write must not
            // replace what is already there.
            assertEquals(saved, settingsFile.readText())
            assertTrue(logger.severeMessages.any { it.contains("Failed to save settings") })
        }

    @Test
    fun `a queued save reads the settings when the write runs, not when it is queued`() =
        withStorage { storage, _, scheduler, _ ->
            var supplied = false

            storage.queueAsyncSave {
                supplied = true
                sampleData()
            }

            assertTrue(!supplied, "the snapshot must not be taken while queueing")

            scheduler.runPending()

            assertTrue(supplied)
            assertEquals(sampleData(), storage.loadFromDisk())
        }
}
