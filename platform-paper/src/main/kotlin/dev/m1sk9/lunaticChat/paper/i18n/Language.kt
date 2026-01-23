package dev.m1sk9.lunaticChat.paper.i18n

/**
 * Represents a supported language in LunaticChat.
 *
 * @property code The language code (e.g., "en", "ja")
 * @property fileName The name of the language file (e.g., "en.yml", "ja.yml")
 */
enum class Language(
    val code: String,
    val fileName: String,
) {
    EN("en", "en.yml"),
    JA("ja", "ja.yml"),
    ;

    companion object {
        /**
         * Converts a language code string to a [Language] enum.
         * If the code is not recognized, returns [EN] as a fallback.
         *
         * @param code The language code to convert
         * @return The corresponding [Language] enum, or [EN] if not found
         */
        fun fromCode(code: String): Language = entries.find { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}
