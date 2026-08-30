package dev.m1sk9.lunaticChat.engine.debug

/**
 * Reads a debug switch written as text.
 *
 * Shared by both platforms so that `debug: velocity,protocol` in config.yml and
 * `-Dlunaticchat.debug=velocity,protocol` on the proxy mean the same thing. A grammar owned by one
 * side would drift from the other the first time a category was added.
 */
object DebugCategories {
    /** What a debug switch asked for. */
    data class Parsed(
        val enabled: Boolean,
        val categories: Set<DebugCategory>,
        /** Names that match no category, reported to the operator rather than thrown. */
        val unknown: List<String>,
    ) {
        /** The categories that actually log. Empty while [enabled] is false, whatever was listed. */
        val active: Set<DebugCategory> get() = if (enabled) categories else emptySet()
    }

    private val allWords = setOf("true", "yes", "on", "y", "all")
    private val noneWords = setOf("false", "no", "off", "n", "none")

    /**
     * Reads a whole switch: a boolean spelling, or a comma-separated list of category keys.
     *
     * The boolean spellings match the ones config.yml already accepts elsewhere: `debug: yes`
     * worked while Bukkit read the file as YAML 1.1, and reading it as a category name instead
     * would silently switch debugging off for a file that had it on.
     */
    fun parse(raw: String): Parsed {
        val trimmed = raw.trim()
        val word = trimmed.lowercase()
        return when {
            trimmed.isEmpty() || word in noneWords -> Parsed(false, DebugCategory.entries.toSet(), emptyList())
            word in allWords -> Parsed(true, DebugCategory.entries.toSet(), emptyList())
            else -> resolve(trimmed.split(','))
        }
    }

    /** Reads category keys that arrived already separated, as config.yml's `categories:` list does. */
    fun resolve(names: Iterable<String>): Parsed {
        val categories = LinkedHashSet<DebugCategory>()
        val unknown = mutableListOf<String>()

        names
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { name ->
                val category = DebugCategory.fromKey(name)
                if (category == null) unknown += name else categories += category
            }

        // A switch that named nothing this build knows is off rather than on-with-no-categories:
        // reporting it as enabled would put "debug is on" in /lc status for a typo that logs nothing.
        return Parsed(enabled = categories.isNotEmpty(), categories = categories, unknown = unknown)
    }
}
