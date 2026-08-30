package dev.m1sk9.lunaticChat.engine.debug

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugCategoriesTest {
    @Test
    fun `a true spelling switches every category on`() {
        listOf("true", "TRUE", "yes", "on", "all", " true ").forEach { raw ->
            val parsed = DebugCategories.parse(raw)

            assertTrue(parsed.enabled, raw)
            assertEquals(DebugCategory.entries.toSet(), parsed.active, raw)
        }
    }

    @Test
    fun `a false spelling switches every category off`() {
        listOf("false", "FALSE", "no", "off", "none", "", "   ").forEach { raw ->
            val parsed = DebugCategories.parse(raw)

            assertFalse(parsed.enabled, raw)
            assertEquals(emptySet(), parsed.active, raw)
        }
    }

    @Test
    fun `a comma separated list switches on exactly the categories it names`() {
        val parsed = DebugCategories.parse("velocity,protocol")

        assertTrue(parsed.enabled)
        assertEquals(setOf(DebugCategory.VELOCITY, DebugCategory.PROTOCOL), parsed.active)
        assertEquals(emptyList(), parsed.unknown)
    }

    @Test
    fun `surrounding whitespace, casing and empty entries are ignored`() {
        val parsed = DebugCategories.parse(" Velocity , , PROTOCOL ,")

        assertEquals(setOf(DebugCategory.VELOCITY, DebugCategory.PROTOCOL), parsed.active)
        assertEquals(emptyList(), parsed.unknown)
    }

    @Test
    fun `an unknown name is reported without discarding the categories beside it`() {
        val parsed = DebugCategories.parse("velocity,proxy")

        assertEquals(setOf(DebugCategory.VELOCITY), parsed.active)
        assertEquals(listOf("proxy"), parsed.unknown)
    }

    @Test
    fun `a switch naming only unknown categories logs nothing`() {
        val parsed = DebugCategories.parse("maybe")

        assertEquals(emptySet(), parsed.active)
        assertEquals(listOf("maybe"), parsed.unknown)
    }

    @Test
    fun `resolve reads a list the way parse reads the same names spelled with commas`() {
        assertEquals(
            DebugCategories.parse("velocity,protocol"),
            DebugCategories.resolve(listOf("velocity", "protocol")),
        )
    }

    @Test
    fun `all names every category wherever a category may be named`() {
        // The same word works as `debug: all`, as `categories: [all]` and as `/lc debug all on`;
        // reading it as a category name in only two of the three would log nothing for the third.
        assertEquals(DebugCategory.entries.toSet(), DebugCategories.parse("all").active)
        assertEquals(DebugCategory.entries.toSet(), DebugCategories.resolve(listOf("all")).active)
        assertEquals(emptyList(), DebugCategories.resolve(listOf("ALL")).unknown)
    }

    @Test
    fun `all beside a named category still names every category`() {
        val parsed = DebugCategories.resolve(listOf("velocity", "all"))

        assertEquals(DebugCategory.entries.toSet(), parsed.active)
        assertEquals(emptyList(), parsed.unknown)
    }

    @Test
    fun `every category is reachable by the key operators write`() {
        DebugCategory.entries.forEach { category ->
            assertEquals(category, DebugCategory.fromKey(category.key))
        }
    }
}
