package dev.m1sk9.lunaticChat.paper.i18n

import dev.m1sk9.lunaticChat.paper.config.key.MessageFormatConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * Renders the chat formats of config.yml.
 *
 * Each entry point names, in its parameter list, exactly the placeholders that format accepts:
 * adding one means adding a [ChatPlaceholder] constant, a parameter here, and letting the compiler
 * point at the callers that must supply it. Handlers never spell a placeholder name.
 */
fun MessageFormatConfig.directMessage(
    sender: String,
    recipient: String,
    message: String,
): Component =
    directMessageFormat.render(
        mapOf(
            ChatPlaceholder.SENDER to sender,
            ChatPlaceholder.RECIPIENT to recipient,
            ChatPlaceholder.MESSAGE to message,
        ),
    )

fun MessageFormatConfig.channelMessage(
    sender: String,
    channel: String,
    message: String,
): Component =
    channelMessageFormat.render(
        mapOf(
            ChatPlaceholder.SENDER to sender,
            ChatPlaceholder.CHANNEL to channel,
            ChatPlaceholder.MESSAGE to message,
        ),
    )

fun MessageFormatConfig.crossServerGlobalChat(
    server: String,
    sender: String,
    message: String,
): Component =
    crossServerGlobalChatFormat.render(
        mapOf(
            ChatPlaceholder.SERVER to server,
            ChatPlaceholder.SENDER to sender,
            ChatPlaceholder.MESSAGE to message,
        ),
    )

private val PLACEHOLDER = Regex("""\{([A-Za-z]+)}""")

// One pass over the format rather than one replace per placeholder, so a value that itself
// contains "{message}" (a channel named that way, say) is inserted literally instead of being
// substituted again by the next replace.
private fun String.render(values: Map<ChatPlaceholder, String>): Component {
    val text =
        replace(PLACEHOLDER) { match ->
            ChatPlaceholder.fromKey(match.groupValues[1])?.let(values::get) ?: match.value
        }
    return LegacyComponentSerializer.legacySection().deserialize(text)
}
