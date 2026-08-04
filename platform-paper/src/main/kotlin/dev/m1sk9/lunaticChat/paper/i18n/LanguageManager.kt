package dev.m1sk9.lunaticChat.paper.i18n

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

/**
 * Manages language files and provides message retrieval with string-based keys.
 *
 * This class handles:
 * - Loading language files from resources/languages/
 * - Caching flattened language messages in memory
 * - Providing message retrieval with placeholder substitution
 * - Fallback to English if the selected language is unavailable
 *
 * @property plugin The JavaPlugin instance (required unless resourceLoader is provided)
 * @property logger The logger for this manager
 * @property selectedLanguage The currently selected language
 * @property resourceLoader Optional resource loader function for testing (overrides plugin.getResource)
 */
class LanguageManager(
    private val plugin: JavaPlugin? = null,
    private val logger: Logger,
    private val selectedLanguage: Language,
    private val resourceLoader: ((String) -> java.io.InputStream?)? = null,
) {
    init {
        require(plugin != null || resourceLoader != null) {
            "Either plugin or resourceLoader must be provided"
        }
    }

    private val languageCache = mutableMapOf<Language, Map<String, String>>()

    /**
     * Initializes the language manager by loading the languages [getMessage] can read: the
     * selected one and the English fallback. Other bundled languages are never consulted, so
     * parsing and flattening them at startup would be wasted work.
     *
     * This should be called during plugin initialization.
     *
     * @throws IllegalStateException if the English fallback file is missing or cannot be loaded
     */
    fun initialize() {
        linkedSetOf(Language.EN, selectedLanguage).forEach { lang ->
            try {
                val messages = loadLanguageFile(lang)
                languageCache[lang] = messages
                logger.info("Loaded language file: ${lang.fileName} (${messages.size} keys)")
            } catch (e: Exception) {
                logger.warning("Failed to load ${lang.fileName}: ${e.message}")
                if (lang == Language.EN) {
                    throw IllegalStateException("English fallback missing", e)
                }
            }
        }
    }

    /**
     * Loads a language file from resources/languages/ and flattens it to a key-value map.
     *
     * @param language The language to load
     * @return A flattened map of message keys to their values
     * @throws IllegalStateException if the language file is not found
     */
    private fun loadLanguageFile(language: Language): Map<String, String> {
        val resourcePath = "languages/${language.fileName}"
        val stream =
            (resourceLoader?.invoke(resourcePath) ?: plugin?.getResource(resourcePath))
                ?: throw IllegalStateException("Language file not found: $resourcePath")

        val yamlContent = stream.bufferedReader().use { it.readText() }
        val root = Yaml.default.parseToYamlNode(yamlContent)
        check(root is YamlMap) { "Root YAML node must be a map" }

        return buildMap { flattenInto(root, prefix = "", into = this) }
    }

    /**
     * Flattens a YAML tree into dot-notation keys.
     * Example: {"toggle": {"on": "有効"}} -> {"toggle.on": "有効"}
     */
    private fun flattenInto(
        node: YamlNode,
        prefix: String,
        into: MutableMap<String, String>,
    ) {
        when (node) {
            is YamlMap ->
                node.entries.forEach { (key, value) ->
                    val fullKey = if (prefix.isEmpty()) key.content else "$prefix.${key.content}"
                    flattenInto(value, fullKey, into)
                }
            // Lists are converted to comma-separated strings for simplicity
            is YamlList -> into[prefix] = node.items.joinToString(", ") { scalarText(it) }
            else -> into[prefix] = scalarText(node)
        }
    }

    private fun scalarText(node: YamlNode): String = (node as? YamlScalar)?.content ?: node.contentToString()

    /**
     * Retrieves a message for the given string key with optional placeholder substitution.
     * If the selected language is unavailable, falls back to English.
     *
     * @param key The message key to retrieve (e.g., "commandDescription.tell" or "toggle.on")
     * @param placeholders Optional map of placeholder names to their replacement values
     * @return The formatted message with placeholders substituted, or the key itself if not found
     */
    fun getMessage(
        key: String,
        placeholders: Map<String, String> = emptyMap(),
    ): String {
        val messages = languageCache[selectedLanguage] ?: languageCache[Language.EN]!!
        var message =
            messages[key] ?: run {
                logger.warning("Message key not found: $key")
                return key
            }

        // Replace placeholders
        placeholders.forEach { (placeholder, value) ->
            message = message.replace("{$placeholder}", value)
        }

        return message
    }

    /**
     * Gets the translated text for a toggle state (enabled/disabled).
     *
     * @param enabled The toggle state
     * @return The translated "on" or "off" text
     */
    fun getToggleText(enabled: Boolean): String = getMessage(if (enabled) "toggle.on" else "toggle.off")
}
