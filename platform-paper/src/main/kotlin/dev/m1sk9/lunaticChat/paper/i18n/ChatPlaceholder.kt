package dev.m1sk9.lunaticChat.paper.i18n

/**
 * Every `{name}` a chat format in config.yml may contain.
 *
 * [key] is the spelling operators write between the braces; the enum name is never exposed to
 * them. The renderer resolves names only through this enum, so a placeholder exists once it has
 * a constant here and an entry point in ChatFormat.kt supplies its value.
 */
enum class ChatPlaceholder(
    val key: String,
) {
    SENDER("sender"),
    RECIPIENT("recipient"),
    MESSAGE("message"),
    CHANNEL("channel"),
    CHANNEL_ID("channel_id"),
    DISPLAY_NAME("display_name"),
    SERVER("server"),
    WORLD("world"),
    ROLE("role"),
    ;

    companion object {
        private val byKey = entries.associateBy { it.key }

        /** The placeholder [key] names, or null when no placeholder is spelled that way. */
        fun fromKey(key: String): ChatPlaceholder? = byKey[key]
    }
}
