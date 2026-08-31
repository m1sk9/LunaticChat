package dev.m1sk9.lunaticChat.paper.config

import com.charleskorn.kaml.Yaml
import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.engine.debug.DebugState
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.config.key.DebugConfig
import dev.m1sk9.lunaticChat.paper.config.key.FeaturesConfig
import dev.m1sk9.lunaticChat.paper.config.key.MessageFormatConfig
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConfigurationReloaderTest {
    private val startup = TestUtils.createTestConfiguration()
    private val holder = MessageFormatHolder(startup.messageFormat)
    private val debugState = DebugState(startup.debug.activeCategories)
    private val logger = TestUtils.TestLogger()

    private fun reloaderReading(contents: () -> String) =
        ConfigurationReloader(
            configManager = ConfigManager(logger),
            startupConfiguration = startup,
            messageFormatHolder = holder,
            debugState = debugState,
            logger = logger,
            readConfigFile = contents,
        )

    /**
     * The config.yml an operator would have after editing the startup configuration into [edit].
     *
     * Written out through the serializer rather than assembled by hand so the document is always a
     * complete, well-formed file - the reload path treats a partial one as a settings change.
     */
    private fun fileFor(edit: LunaticChatConfiguration.() -> LunaticChatConfiguration): String =
        Yaml.default.encodeToString(LunaticChatConfiguration.serializer(), startup.edit())

    @Test
    fun `a changed message format is applied and needs no restart`() {
        val result =
            reloaderReading {
                fileFor { copy(messageFormat = messageFormat.copy(directMessageFormat = "new format {message}")) }
            }.reload()

        assertIs<ReloadResult.Applied>(result)
        assertEquals(listOf("messageFormat.directMessageFormat"), result.applied)
        assertEquals(emptyList(), result.restartRequired)
        assertEquals("new format {message}", holder.current.directMessageFormat)
    }

    @Test
    fun `a changed feature is reported as needing a restart and is not applied`() {
        val result =
            reloaderReading {
                fileFor {
                    copy(features = features.copy(channelChat = features.channelChat.copy(enabled = !features.channelChat.enabled)))
                }
            }.reload()

        assertIs<ReloadResult.Applied>(result)
        assertEquals(emptyList(), result.applied)
        assertEquals(listOf("features.channelChat"), result.restartRequired)
        assertEquals(startup.messageFormat, holder.current)
    }

    @Test
    fun `a file identical to the running configuration changes nothing`() {
        val result = reloaderReading { fileFor { this } }.reload()

        assertIs<ReloadResult.Applied>(result)
        assertEquals(emptyList(), result.applied)
        assertEquals(emptyList(), result.restartRequired)
    }

    @Test
    fun `a setting still needing a restart is reported again by a second reload`() {
        val contents =
            fileFor {
                copy(
                    messageFormat = messageFormat.copy(directMessageFormat = "new format {message}"),
                    features = features.copy(channelChat = features.channelChat.copy(enabled = !features.channelChat.enabled)),
                )
            }
        val reloader = reloaderReading { contents }

        reloader.reload()
        val second = reloader.reload()

        assertIs<ReloadResult.Applied>(second)
        // Already applied, so no longer new; still not in effect, so still worth reporting.
        assertEquals(emptyList(), second.applied)
        assertEquals(listOf("features.channelChat"), second.restartRequired)
    }

    @Test
    fun `an unreadable setting leaves the running formats alone`() {
        val result = reloaderReading { "checkForUpdates: maybe" }.reload()

        assertIs<ReloadResult.InvalidSettings>(result)
        assertEquals(listOf("checkForUpdates"), result.fallbacks.map { it.settingKey })
        assertEquals(startup.messageFormat, holder.current)
    }

    @Test
    fun `a changed debug switch is applied and needs no restart`() {
        val result =
            reloaderReading {
                fileFor { copy(debug = DebugConfig(enabled = true, categories = setOf(DebugCategory.VELOCITY))) }
            }.reload()

        assertIs<ReloadResult.Applied>(result)
        assertEquals(listOf("debug"), result.applied)
        assertEquals(emptyList(), result.restartRequired)
        assertEquals(setOf(DebugCategory.VELOCITY), debugState.enabled)
    }

    @Test
    fun `a reload puts the file back in charge of a switch that lc debug had moved`() {
        // /lc debug is deliberately volatile, so an unchanged file is still a change to apply when
        // the running server has been switched by hand since it was read.
        debugState.replace(setOf(DebugCategory.CHAT))

        val result = reloaderReading { fileFor { this } }.reload()

        assertIs<ReloadResult.Applied>(result)
        assertEquals(listOf("debug"), result.applied)
        assertEquals(emptySet(), debugState.enabled)
    }

    @Test
    fun `a document holding no settings leaves the running formats alone`() {
        // A config.yml read while an editor or an upload is halfway through writing it must not
        // reset every format to its default.
        val result = reloaderReading { "# halfway through a write" }.reload()

        assertIs<ReloadResult.InvalidDocument>(result)
        assertEquals(startup.messageFormat, holder.current)
    }

    @Test
    fun `a file that cannot be read leaves the running formats alone`() {
        val result = reloaderReading { throw java.io.IOException("no such file") }.reload()

        assertIs<ReloadResult.Unreadable>(result)
        assertTrue(result.reason.contains("no such file"))
        assertEquals(startup.messageFormat, holder.current)
    }

    @Test
    fun `a read failure carrying no message is still reported`() {
        val result = reloaderReading { throw java.io.IOException() }.reload()

        assertIs<ReloadResult.Unreadable>(result)
        assertTrue(result.reason.contains("IOException"))
        assertEquals(startup.messageFormat, holder.current)
    }

    @Test
    fun `a reload is recorded in the server log`() {
        reloaderReading {
            fileFor { copy(messageFormat = messageFormat.copy(channelMessageFormat = "new channel format")) }
        }.reload()

        assertTrue(logger.infoMessages.any { it.contains("messageFormat.channelMessageFormat") })
    }

    /**
     * The two tables below decide what a reload claims to have done, so a setting missing from both
     * would be applied or refused without ever being named. These fail when config.yml grows a
     * setting nobody has classified.
     */
    @Test
    fun `every message format is one the reload applies`() {
        assertEquals(
            MessageFormatConfig::class.memberProperties.map { "messageFormat.${it.name}" }.sorted(),
            ConfigurationReloader.APPLIED.map { it.first }.sorted(),
        )
    }

    @Test
    fun `every message format is a leaf`() {
        // The applied settings are compared leaf by leaf, so a format block holding another block
        // would have its inner values changed without being named.
        MessageFormatConfig::class.memberProperties.forEach {
            assertEquals(String::class, it.returnType.jvmErasure, "messageFormat.${it.name} is no longer a leaf")
        }
    }

    @Test
    fun `every feature block is one the reload refuses to apply`() {
        assertEquals(
            FeaturesConfig::class.memberProperties.map { "features.${it.name}" }.sorted(),
            ConfigurationReloader.RESTART_REQUIRED
                .map { it.first }
                .filter { it.startsWith("features.") }
                .sorted(),
        )
    }

    @Test
    fun `every setting outside features and messageFormat is one the reload refuses to apply`() {
        assertEquals(
            LunaticChatConfiguration::class
                .memberProperties
                .map { it.name }
                // debug is applied rather than refused; the two tests above cover it.
                .filterNot { it == "features" || it == "messageFormat" || it == "debug" }
                .sorted(),
            ConfigurationReloader.RESTART_REQUIRED
                .map { it.first }
                .filterNot { it.startsWith("features.") }
                .sorted(),
        )
    }
}
