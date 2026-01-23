package dev.m1sk9.lunaticChat.paper.i18n

import com.charleskorn.kaml.Yaml
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

/**
 * Manages language files and provides message retrieval with type safety.
 *
 * This class handles:
 * - Loading language files from resources/languages/
 * - Caching language configurations in memory
 * - Providing message retrieval with placeholder substitution
 * - Fallback to English if the selected language is unavailable
 *
 * @property plugin The JavaPlugin instance
 * @property logger The logger for this manager
 * @property selectedLanguage The currently selected language
 */
class LanguageManager(
    private val plugin: JavaPlugin,
    private val logger: Logger,
    private val selectedLanguage: Language,
) {
    private val languageCache = mutableMapOf<Language, LanguageConfig>()

    /**
     * Initializes the language manager by loading all language files.
     * This should be called during plugin initialization.
     *
     * @throws IllegalStateException if the English fallback file is missing or cannot be loaded
     */
    fun initialize() {
        Language.entries.forEach { lang ->
            try {
                val config = loadLanguageFile(lang)
                languageCache[lang] = config
                logger.info("Loaded language file: ${lang.fileName}")
            } catch (e: Exception) {
                logger.warning("Failed to load ${lang.fileName}: ${e.message}")
                if (lang == Language.EN) {
                    throw IllegalStateException("English fallback missing", e)
                }
            }
        }
    }

    /**
     * Loads a language file from resources/languages/.
     *
     * @param language The language to load
     * @return The parsed LanguageConfig
     * @throws IllegalStateException if the language file is not found
     */
    private fun loadLanguageFile(language: Language): LanguageConfig {
        val stream =
            plugin.getResource("languages/${language.fileName}")
                ?: throw IllegalStateException("Language file not found: languages/${language.fileName}")

        val yamlContent = stream.bufferedReader().use { it.readText() }
        return Yaml.default.decodeFromString(LanguageConfig.serializer(), yamlContent)
    }

    /**
     * Retrieves a message for the given message key.
     * If the selected language is unavailable, falls back to English.
     *
     * @param key The message key to retrieve
     * @return The formatted message with placeholders substituted
     */
    fun getMessage(key: MessageKey): String {
        val config = languageCache[selectedLanguage] ?: languageCache[Language.EN]!!

        return when (key) {
            MessageKey.CommandDescriptionTell -> config.commandDescription.tell
            MessageKey.CommandDescriptionReply -> config.commandDescription.reply
            MessageKey.CommandDescriptionJp -> config.commandDescription.jp
            MessageKey.CommandDescriptionNotice -> config.commandDescription.notice
            is MessageKey.DirectMessageNoticeStatus ->
                config.directMessageNoticeStatus.replace("{toggle}", key.toggle)
            is MessageKey.DirectMessageNoticeToggle ->
                config.directMessageNoticeToggle.replace("{toggle}", key.toggle)
            is MessageKey.RomajiConversionStatus ->
                config.romajiConversionStatus.replace("{toggle}", key.toggle)
            is MessageKey.RomajiConversionToggle ->
                config.romajiConversionToggle.replace("{toggle}", key.toggle)
            MessageKey.ReplyTargetNotFound -> config.replyTargetNotFound
            is MessageKey.TellTargetOffline ->
                config.tellTargetOffline.replace("{target}", key.targetName)
            MessageKey.TellYourself -> config.tellYourself
            MessageKey.ToggleOn -> config.toggle.on
            MessageKey.ToggleOff -> config.toggle.off
            MessageKey.PlayerOnlyCommand -> "This command can only be executed by players."
        }
    }

    /**
     * Gets the translated text for a toggle state (enabled/disabled).
     *
     * @param enabled The toggle state
     * @return The translated "on" or "off" text
     */
    fun getToggleText(enabled: Boolean): String = getMessage(if (enabled) MessageKey.ToggleOn else MessageKey.ToggleOff)
}
