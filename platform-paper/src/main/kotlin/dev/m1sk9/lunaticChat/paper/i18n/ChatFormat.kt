package dev.m1sk9.lunaticChat.paper.i18n

/**
 * Substitutes `{name}` placeholders in one of the configurable chat formats.
 *
 * Which names a format accepts is documented alongside it in config.yml; going through this
 * function keeps every format applying them the same way.
 */
fun String.withChatPlaceholders(vararg values: Pair<String, String>): String =
    values.fold(this) { text, (name, value) -> text.replace("{$name}", value) }
