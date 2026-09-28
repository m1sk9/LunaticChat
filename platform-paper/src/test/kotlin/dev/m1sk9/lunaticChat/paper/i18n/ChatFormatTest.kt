package dev.m1sk9.lunaticChat.paper.i18n

import dev.m1sk9.lunaticChat.paper.config.key.MessageFormatConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatFormatTest {
    private val defaults = MessageFormatConfig()
    private val placeholderPattern = Regex("""\{([A-Za-z]+)}""")

    private fun Component.legacy(): String = LegacyComponentSerializer.legacySection().serialize(this)

    private fun MessageFormatConfig.renderAll(): List<String> =
        listOf(
            directMessage("Alice", "Bob", "hi"),
            channelMessage("Alice", "general", "hi"),
            crossServerGlobalChat("lobby", "Alice", "hi"),
        ).map { it.legacy() }

    @Test
    fun `the default direct message format renders sender, recipient and message`() {
        assertEquals("§7[§eAlice §7>> §eBob§7] §fhi", defaults.directMessage("Alice", "Bob", "hi").legacy())
    }

    @Test
    fun `the default channel message format renders channel, sender and message`() {
        assertEquals("§7[§b#general§7] §eAlice: §fhi", defaults.channelMessage("Alice", "general", "hi").legacy())
    }

    @Test
    fun `the default cross-server global chat format renders server, sender and message`() {
        assertEquals("§7[§6lobby§7] §eAlice: §fhi", defaults.crossServerGlobalChat("lobby", "Alice", "hi").legacy())
    }

    @Test
    fun `unknown placeholders and ones the format does not accept are left as written`() {
        val formats = MessageFormatConfig(directMessageFormat = "{foo} {channel} {message}")

        assertEquals("{foo} {channel} hi", formats.directMessage("Alice", "Bob", "hi").legacy())
    }

    @Test
    fun `a value containing a placeholder is inserted literally`() {
        val formats = MessageFormatConfig(channelMessageFormat = "<{channel}> {message}")

        assertEquals("<{message}> hi", formats.channelMessage("Alice", "{message}", "hi").legacy())
    }

    @Test
    fun `dollar signs and backslashes in a value are inserted literally`() {
        val formats = MessageFormatConfig(directMessageFormat = "{message}")

        assertEquals("$1 \\ {sender}", formats.directMessage("Alice", "Bob", "$1 \\ {sender}").legacy())
    }

    @Test
    fun `section sign codes become styles rather than text`() {
        val rendered = defaults.directMessage("Alice", "Bob", "hi")

        assertTrue(rendered.children().isNotEmpty())
        assertEquals(NamedTextColor.GRAY, rendered.children().first().color())
    }

    @Test
    fun `every placeholder in the default formats is a known one`() {
        MessageFormatConfig::class.memberProperties.forEach { property ->
            val format = property.get(defaults) as String
            placeholderPattern.findAll(format).forEach {
                val name = it.groupValues[1]
                assertTrue(ChatPlaceholder.fromKey(name) != null, "${property.name} uses unknown {$name}")
            }
        }
    }

    @Test
    fun `the default formats use only placeholders their entry point supplies`() {
        defaults.renderAll().forEach { assertFalse('{' in it, "unsubstituted placeholder in $it") }
    }

    @Test
    fun `every placeholder is supplied by at least one entry point`() {
        val probe = ChatPlaceholder.entries.joinToString(" ") { "{${it.key}}" }
        val formats = MessageFormatConfig(probe, probe, probe)

        val supplied =
            formats
                .renderAll()
                .flatMap { rendered -> ChatPlaceholder.entries.filterNot { "{${it.key}}" in rendered } }
                .toSet()

        assertEquals(ChatPlaceholder.entries.toSet(), supplied)
    }

    @Test
    fun `placeholder keys are unique and match the renderer's pattern`() {
        val keys = ChatPlaceholder.entries.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
        keys.forEach { assertTrue(Regex("[A-Za-z]+").matches(it), "$it cannot be matched by the renderer") }
    }
}
