package dev.m1sk9.lunaticChat.paper.i18n

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * Formats messages with the prefix and applies color styling.
 *
 * All messages follow this format:
 * - Prefix: `LC`, AQUA + BOLD
 * - Message: Main text in GRAY
 * - Placeholders: Text in {braces} highlighted in YELLOW
 */
object MessageFormatter {
    private val PREFIX_COLOR = NamedTextColor.LIGHT_PURPLE
    private val MESSAGE_COLOR = NamedTextColor.WHITE
    private val PLACEHOLDER_COLOR = NamedTextColor.YELLOW
    private val ERROR_COLOR = NamedTextColor.RED
    private val SUCCESS_COLOR = NamedTextColor.GREEN

    /**
     * Formats a message with the standard prefix and gray text.
     * Placeholders in {braces} are highlighted in yellow if [highlightPlaceholders] is true.
     *
     * @param message The message text to format
     * @param highlightPlaceholders Whether to highlight {placeholder} text in yellow
     * @return A formatted Component with the prefix
     */
    fun format(
        message: String,
        highlightPlaceholders: Boolean = true,
    ): Component {
        val prefix =
            Component
                .text("[LC] ")
                .color(PREFIX_COLOR)

        val messageComponent =
            if (highlightPlaceholders) {
                formatWithPlaceholders(message, MESSAGE_COLOR)
            } else {
                Component.text(message).color(MESSAGE_COLOR)
            }

        return prefix.append(messageComponent)
    }

    /**
     * Formats an error message with the prefix and red text.
     *
     * @param message The error message text
     * @return A formatted Component with red text
     */
    fun formatError(message: String): Component {
        val prefix =
            Component
                .text("[LC] ")
                .color(PREFIX_COLOR)

        val messageComponent = formatWithPlaceholders(message, ERROR_COLOR)

        return prefix.append(messageComponent)
    }

    /**
     * Formats a success message with the prefix and green text.
     *
     * @param message The success message text
     * @return A formatted Component with green text
     */
    fun formatSuccess(message: String): Component {
        val prefix =
            Component
                .text("[LC] ")
                .color(PREFIX_COLOR)

        val messageComponent = formatWithPlaceholders(message, SUCCESS_COLOR)

        return prefix.append(messageComponent)
    }

    /**
     * Parses a message and highlights placeholders in {braces} with yellow color.
     * Text outside braces uses the specified base color.
     *
     * @param message The message text to parse
     * @param baseColor The color for non-placeholder text
     * @return A Component with highlighted placeholders
     */
    private fun formatWithPlaceholders(
        message: String,
        baseColor: NamedTextColor,
    ): Component {
        val result = Component.text()
        val regex = Regex("""\{([^}]+)}""")
        var lastIndex = 0

        regex.findAll(message).forEach { match ->
            // Add text before the placeholder
            if (match.range.first > lastIndex) {
                result.append(
                    Component
                        .text(message.substring(lastIndex, match.range.first))
                        .color(baseColor),
                )
            }

            // Add the placeholder in yellow (including braces)
            result.append(
                Component
                    .text(match.value)
                    .color(PLACEHOLDER_COLOR),
            )

            lastIndex = match.range.last + 1
        }

        // Add remaining text after the last placeholder
        if (lastIndex < message.length) {
            result.append(
                Component
                    .text(message.substring(lastIndex))
                    .color(baseColor),
            )
        }

        return result.build()
    }
}
