package dev.m1sk9.lunaticChat.paper.i18n

import dev.m1sk9.lunaticChat.paper.config.key.MessageFormatConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatFormatTest {
    private val defaults = MessageFormatConfig()
    private val queen = Component.text("Queen")

    private fun Component.legacy(): String = LegacyComponentSerializer.legacySection().serialize(this)

    private fun Component.leafContaining(text: String): Component =
        generateSequence(listOf(this)) { level -> level.flatMap { it.children() }.ifEmpty { null } }
            .flatten()
            .first { it is TextComponent && text in it.content() }

    private fun MessageFormatConfig.renderAll(): List<String> =
        listOf(
            directMessage("Alice", "Bob", "hi", "world"),
            channelMessage("Alice", "general", "general", queen, "hi", "world", { "Owner" }),
            crossServerGlobalChat("lobby", "Alice", "hi"),
        ).map { it.legacy() }

    @Test
    fun `the default direct message format renders sender, recipient and message`() {
        assertEquals("§7[§eAlice §7>> §eBob§7] §fhi", defaults.directMessage("Alice", "Bob", "hi", "world").legacy())
    }

    @Test
    fun `the default channel message format renders channel, sender and message`() {
        assertEquals(
            "§7[§b#general§7] §eAlice: §fhi",
            defaults.channelMessage("Alice", "general", "general", queen, "hi", "world", { "Owner" }).legacy(),
        )
    }

    @Test
    fun `the default cross-server global chat format renders server, sender and message`() {
        assertEquals("§7[§6lobby§7] §eAlice: §fhi", defaults.crossServerGlobalChat("lobby", "Alice", "hi").legacy())
    }

    @Test
    fun `the direct message format renders the sender's world`() {
        val formats = MessageFormatConfig(directMessageFormat = "{sender} ({world}): {message}")

        assertEquals("Alice (world_nether): hi", formats.directMessage("Alice", "Bob", "hi", "world_nether").legacy())
    }

    @Test
    fun `the channel format renders channel ID, display name, world and role`() {
        val formats = MessageFormatConfig(channelMessageFormat = "{channel_id} {display_name} {world} {role}")

        assertEquals(
            "general-1 Queen world_nether Owner",
            formats.channelMessage("Alice", "General", "general-1", queen, "hi", "world_nether", { "Owner" }).legacy(),
        )
    }

    @Test
    fun `a display name keeps its own color without recoloring the rest of the format`() {
        val formats = MessageFormatConfig(channelMessageFormat = "{display_name}: {message}")
        val displayName = Component.text("Queen", NamedTextColor.RED)

        val rendered = formats.channelMessage("Alice", "general", "general", displayName, "hi", "world", { "Owner" })

        assertEquals("Queen: hi", PlainTextComponentSerializer.plainText().serialize(rendered))
        assertEquals(NamedTextColor.RED, rendered.leafContaining("Queen").color())
        assertNull(rendered.leafContaining(": hi").color())
    }

    @Test
    fun `a display name without a color of its own takes the format's color`() {
        val formats = MessageFormatConfig(channelMessageFormat = "§e{display_name}")

        assertEquals("§eQueen", formats.channelMessage("Alice", "general", "general", queen, "hi", "world", { "Owner" }).legacy())
    }

    @Test
    fun `a display name keeps hex colors and hover events`() {
        val formats = MessageFormatConfig(channelMessageFormat = "{display_name}")
        val hover = HoverEvent.showText(Component.text("Level 42"))
        val displayName = Component.text("Queen", TextColor.color(0x12AB34)).hoverEvent(hover)

        val rendered = formats.channelMessage("Alice", "general", "general", displayName, "hi", "world", { "Owner" })

        val queenLeaf = rendered.leafContaining("Queen")
        assertEquals(TextColor.color(0x12AB34), queenLeaf.color())
        assertEquals(hover, queenLeaf.hoverEvent())
    }

    @Test
    fun `the role is not computed when the format does not use it`() {
        var computed = false

        defaults.channelMessage("Alice", "general", "general", queen, "hi", "world") { "Owner".also { computed = true } }

        assertFalse(computed)
    }

    @Test
    fun `unknown placeholders and ones the format does not accept are left as written`() {
        val formats = MessageFormatConfig(directMessageFormat = "{foo} {channel} {message}")

        assertEquals("{foo} {channel} hi", formats.directMessage("Alice", "Bob", "hi", "world").legacy())
    }

    @Test
    fun `a value containing a placeholder is inserted literally`() {
        val formats = MessageFormatConfig(channelMessageFormat = "<{channel}> {message}")

        assertEquals("<{message}> hi", formats.channelMessage("Alice", "{message}", "general", queen, "hi", "world", { "Owner" }).legacy())
    }

    @Test
    fun `dollar signs and backslashes in a value are inserted literally`() {
        val formats = MessageFormatConfig(directMessageFormat = "{message}")

        assertEquals("$1 \\ {sender}", formats.directMessage("Alice", "Bob", "$1 \\ {sender}", "world").legacy())
    }

    @Test
    fun `section sign codes become styles rather than text`() {
        val rendered = defaults.directMessage("Alice", "Bob", "hi", "world")

        assertTrue(rendered.children().isNotEmpty())
        assertEquals(NamedTextColor.GRAY, rendered.children().first().color())
    }

    @Test
    fun `every placeholder in the default formats is a known one`() {
        MessageFormatConfig::class.memberProperties.forEach { property ->
            val format = property.get(defaults) as String
            CHAT_PLACEHOLDER_PATTERN.findAll(format).forEach {
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
        keys.forEach { assertTrue(CHAT_PLACEHOLDER_PATTERN.matches("{$it}"), "$it cannot be matched by the renderer") }
    }
}
