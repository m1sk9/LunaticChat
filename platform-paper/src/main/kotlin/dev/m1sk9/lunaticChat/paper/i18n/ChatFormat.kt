package dev.m1sk9.lunaticChat.paper.i18n

import dev.m1sk9.lunaticChat.paper.config.key.MessageFormatConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
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
    world: String,
): Component =
    directMessageFormat.render(
        mapOf(
            ChatPlaceholder.SENDER to sender,
            ChatPlaceholder.RECIPIENT to recipient,
            ChatPlaceholder.MESSAGE to message,
            ChatPlaceholder.WORLD to world,
        ),
    )

fun MessageFormatConfig.channelMessage(
    sender: String,
    channel: String,
    channelId: String,
    displayName: Component,
    message: String,
    world: String,
    role: () -> String,
): Component =
    channelMessageFormat.render(
        mapOf(
            ChatPlaceholder.SENDER to sender,
            ChatPlaceholder.CHANNEL to channel,
            ChatPlaceholder.CHANNEL_ID to channelId,
            ChatPlaceholder.MESSAGE to message,
            ChatPlaceholder.WORLD to world,
        ),
        deferred = mapOf(ChatPlaceholder.ROLE to role),
        components = mapOf(ChatPlaceholder.DISPLAY_NAME to displayName),
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

internal val CHAT_PLACEHOLDER_PATTERN = Regex("""\{([A-Za-z_]+)}""")

// One pass over the format rather than one replace per placeholder, so a value that itself
// contains "{message}" (a channel named that way, say) is inserted literally instead of being
// substituted again by the next replace.
//
// Component values are spliced in after parsing instead of being serialized into the legacy text:
// legacy codes have no closing tag, so a colored display name would recolor everything after it,
// and legacy text cannot hold hex colors or hover and click events at all.
//
// Deferred values are computed only when the format uses them, since the default formats leave
// out the ones that cost a lookup on every message.
private fun String.render(
    values: Map<ChatPlaceholder, String>,
    deferred: Map<ChatPlaceholder, () -> String> = emptyMap(),
    components: Map<ChatPlaceholder, Component> = emptyMap(),
): Component {
    val spliced = mutableSetOf<ChatPlaceholder>()
    val resolved = mutableMapOf<ChatPlaceholder, String>()
    val text =
        replace(CHAT_PLACEHOLDER_PATTERN) { match ->
            when (val placeholder = ChatPlaceholder.fromKey(match.groupValues[1])) {
                null -> match.value
                in components -> placeholder.marker.also { spliced += placeholder }
                in deferred -> resolved.getOrPut(placeholder) { deferred.getValue(placeholder)() }
                else -> values[placeholder] ?: match.value
            }
        }
    val parsed: Component = LegacyComponentSerializer.legacySection().deserialize(text)
    return spliced.fold(parsed) { rendered, placeholder ->
        rendered.replaceText(
            TextReplacementConfig
                .builder()
                .matchLiteral(placeholder.marker)
                .replacement(components.getValue(placeholder))
                .build(),
        )
    }
}

// NUL rather than a printable token: the client refuses to send control characters, so no value
// substituted as text - a message, a channel name - can contain the marker and be replaced by it.
private val ChatPlaceholder.marker: String get() = "\u0000$key\u0000"
