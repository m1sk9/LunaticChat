package dev.m1sk9.lunaticChat.paper.i18n

import com.charleskorn.kaml.Yaml
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

/**
 * Represents a value in a YAML structure.
 */
private sealed class YamlValue {
    data class StringValue(
        val value: String,
    ) : YamlValue()

    data class MapValue(
        val value: Map<String, YamlValue>,
    ) : YamlValue()

    data class ListValue(
        val value: List<YamlValue>,
    ) : YamlValue()
}

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
     * Initializes the language manager by loading all language files.
     * This should be called during plugin initialization.
     *
     * @throws IllegalStateException if the English fallback file is missing or cannot be loaded
     */
    fun initialize() {
        Language.entries.forEach { lang ->
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
        val yamlNode = Yaml.default.parseToYamlNode(yamlContent)

        val rootMap = yamlNodeToMap(yamlNode)
        return flattenYaml(rootMap)
    }

    /**
     * Converts a YamlNode to a type-safe YamlValue structure.
     */
    private fun yamlNodeToMap(node: com.charleskorn.kaml.YamlNode): Map<String, YamlValue> =
        when (node) {
            is com.charleskorn.kaml.YamlMap -> {
                val result = mutableMapOf<String, YamlValue>()
                node.entries.forEach { entry ->
                    val key = entry.key.content
                    val value = yamlNodeToValue(entry.value)
                    result[key] = value
                }
                result
            }
            else -> throw IllegalStateException("Root YAML node must be a map")
        }

    /**
     * Converts a YamlNode to a type-safe YamlValue.
     */
    private fun yamlNodeToValue(node: com.charleskorn.kaml.YamlNode): YamlValue =
        when (node) {
            is com.charleskorn.kaml.YamlMap -> {
                val result = mutableMapOf<String, YamlValue>()
                node.entries.forEach { entry ->
                    val key = entry.key.content
                    val value = yamlNodeToValue(entry.value)
                    result[key] = value
                }
                YamlValue.MapValue(result)
            }
            is com.charleskorn.kaml.YamlList -> {
                YamlValue.ListValue(node.items.map { yamlNodeToValue(it) })
            }
            is com.charleskorn.kaml.YamlScalar -> YamlValue.StringValue(node.content)
            else -> YamlValue.StringValue(node.contentToString())
        }

    /**
     * Flattens a nested map into dot-notation keys.
     * Example: {"toggle": {"on": "有効"}} -> {"toggle.on": "有効"}
     */
    private fun flattenYaml(
        map: Map<String, YamlValue>,
        prefix: String = "",
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()

        map.forEach { (key, value) ->
            val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"

            when (value) {
                is YamlValue.MapValue -> {
                    result.putAll(flattenYaml(value.value, fullKey))
                }
                is YamlValue.StringValue -> result[fullKey] = value.value
                is YamlValue.ListValue -> {
                    // Lists are converted to comma-separated strings for simplicity
                    result[fullKey] = value.value.joinToString(", ") { yamlValueToString(it) }
                }
            }
        }

        return result
    }

    /**
     * Converts a YamlValue to String for flattening purposes.
     */
    private fun yamlValueToString(value: YamlValue): String =
        when (value) {
            is YamlValue.StringValue -> value.value
            is YamlValue.MapValue -> value.value.toString()
            is YamlValue.ListValue -> value.value.toString()
        }

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
