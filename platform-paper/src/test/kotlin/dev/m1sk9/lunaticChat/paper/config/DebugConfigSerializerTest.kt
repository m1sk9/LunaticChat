package dev.m1sk9.lunaticChat.paper.config

import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.config.key.DebugConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugConfigSerializerTest {
    private val configManager = ConfigManager(TestUtils.TestLogger())

    private fun debugFrom(document: String): DebugConfig = configManager.loadConfiguration(document).debug

    @Test
    fun `the switch spellings config yml has always accepted turn every category on`() {
        listOf("true", "yes", "on").forEach { spelling ->
            assertEquals(DebugCategory.entries.toSet(), debugFrom("debug: $spelling").activeCategories, spelling)
        }
    }

    @Test
    fun `the switch spellings config yml has always accepted turn every category off`() {
        listOf("false", "no", "off").forEach { spelling ->
            assertEquals(emptySet(), debugFrom("debug: $spelling").activeCategories, spelling)
        }
    }

    @Test
    fun `an absent debug setting logs nothing`() {
        assertEquals(emptySet(), debugFrom("checkForUpdates: false").activeCategories)
    }

    @Test
    fun `a switch may name the categories directly`() {
        assertEquals(
            setOf(DebugCategory.VELOCITY, DebugCategory.PROTOCOL),
            debugFrom("debug: velocity,protocol").activeCategories,
        )
    }

    @Test
    fun `a block narrows the categories that log`() {
        val debug =
            debugFrom(
                """
                debug:
                  enabled: true
                  categories: [velocity, protocol]
                """.trimIndent(),
            )

        assertEquals(setOf(DebugCategory.VELOCITY, DebugCategory.PROTOCOL), debug.activeCategories)
    }

    @Test
    fun `a block without categories logs all of them`() {
        val debug =
            debugFrom(
                """
                debug:
                  enabled: true
                """.trimIndent(),
            )

        assertEquals(DebugCategory.entries.toSet(), debug.activeCategories)
    }

    @Test
    fun `enabled false wins over the categories listed beside it`() {
        val debug =
            debugFrom(
                """
                debug:
                  enabled: false
                  categories: [velocity]
                """.trimIndent(),
            )

        assertFalse(debug.enabled)
        assertEquals(emptySet(), debug.activeCategories)
    }

    @Test
    fun `a block without enabled logs nothing`() {
        val debug =
            debugFrom(
                """
                debug:
                  categories: [velocity]
                """.trimIndent(),
            )

        assertEquals(emptySet(), debug.activeCategories)
    }

    @Test
    fun `a block may name every category with the same word the switch takes`() {
        val debug =
            debugFrom(
                """
                debug:
                  enabled: true
                  categories: [all]
                """.trimIndent(),
            )

        assertEquals(DebugCategory.entries.toSet(), debug.activeCategories)
        assertEquals(emptyList(), debug.unknownCategories)
    }

    @Test
    fun `an unknown category is reported without costing the categories beside it`() {
        val debug =
            debugFrom(
                """
                debug:
                  enabled: true
                  categories: [velocity, proxy]
                """.trimIndent(),
            )

        assertEquals(setOf(DebugCategory.VELOCITY), debug.activeCategories)
        assertEquals(listOf("proxy"), debug.unknownCategories)
    }

    @Test
    fun `an unknown category never falls the whole setting back to its default`() {
        // Throwing here would make ConfigManager prune `debug` and switch off the categories the
        // operator did spell correctly, which is the opposite of what a typo deserves.
        val debug = debugFrom("debug: velocity,proxy")

        assertEquals(setOf(DebugCategory.VELOCITY), debug.activeCategories)
        assertEquals(listOf("proxy"), debug.unknownCategories)
    }

    @Test
    fun `a block whose enabled is not a boolean falls back to the default`() {
        val logger = TestUtils.TestLogger()
        val debug =
            ConfigManager(logger)
                .loadConfiguration(
                    """
                    debug:
                      enabled: maybe
                    """.trimIndent(),
                ).debug

        assertEquals(DebugConfig(), debug)
        // Named down to the leaf, so ConfigManager prunes only `enabled` and the categories beside
        // it survive.
        assertTrue(logger.warningMessages.any { it.startsWith("debug.enabled in config.yml fell back to its default") })
    }

    @Test
    fun `the whole setting survives a round trip through the serializer`() {
        listOf(
            DebugConfig(),
            DebugConfig(enabled = true),
            DebugConfig(enabled = true, categories = setOf(DebugCategory.VELOCITY, DebugCategory.PROTOCOL)),
            DebugConfig(enabled = false, categories = setOf(DebugCategory.CHAT)),
        ).forEach { debug ->
            val document =
                com.charleskorn.kaml.Yaml.default
                    .encodeToString(LunaticChatConfiguration.serializer(), LunaticChatConfiguration(debug = debug))

            assertEquals(debug, debugFrom(document), document)
        }
    }
}
