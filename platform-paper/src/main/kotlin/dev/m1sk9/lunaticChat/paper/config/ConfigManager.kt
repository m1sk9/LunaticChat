package dev.m1sk9.lunaticChat.paper.config

import com.charleskorn.kaml.EmptyYamlDocumentException
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlException
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlPath
import com.charleskorn.kaml.YamlPathSegment
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Reads config.yml into [LunaticChatConfiguration].
 *
 * The file is deserialized directly rather than copied key by key, so a default lives only on the
 * data class. The hand-written mapper it replaced repeated every default in a second place, and
 * they had already drifted - checkForUpdates disagreed with both config.yml and the data class,
 * and the whole messageLogging block was documented but never read.
 */
class ConfigManager(
    private val logger: Logger,
) {
    private companion object {
        const val UNREADABLE = "config.yml could not be read"
    }

    private val yaml =
        Yaml(
            configuration =
                YamlConfiguration(
                    // A config.yml from a newer build, or one carrying a key we have retired,
                    // should not stop the plugin from starting.
                    strictMode = false,
                ),
        )

    /**
     * Parses [contents] as config.yml.
     *
     * A setting that cannot be read falls back to its default on its own; the rest of the file is
     * still honoured. Only a document that is not YAML at all costs the operator every setting.
     */
    fun loadConfiguration(contents: String): LunaticChatConfiguration {
        var document =
            try {
                // Editors that write a UTF-8 BOM would otherwise leave it on the first key, which
                // strictMode = false then drops as an unknown setting without a word.
                yaml.parseToYamlNode(contents.removePrefix("\uFEFF"))
            } catch (_: EmptyYamlDocumentException) {
                // A file that only holds comments is a valid way of saying "use the defaults", so it
                // is not reported as a failure the operator has to act on.
                return LunaticChatConfiguration()
            } catch (e: Exception) {
                // Not YamlException: a file the reader rejects before it is YAML at all - one saved
                // as UTF-16, or truncated with NUL padding - fails inside the scanner, and letting
                // that out of onEnable would disable the plugin over a config file.
                return allDefaults("config.yml is not valid YAML", e)
            }

        // Each pass drops exactly one setting, so this terminates: the document strictly shrinks
        // until it decodes or there is nothing left to drop.
        while (true) {
            try {
                return yaml.decodeFromYamlNode(LunaticChatConfiguration.serializer(), document)
            } catch (e: YamlException) {
                // kaml rejects the document as a whole, so without this one unreadable value would
                // lose every other setting in the file - a regression against the hand-written
                // mapper, which defaulted per key.
                val setting = e.path.settingKeys()
                val remaining =
                    document.without(setting)
                        ?: return allDefaults(UNREADABLE, e)
                logger.warning("${setting.joinToString(".")} in config.yml fell back to its default: ${e.message}")
                document = remaining
            } catch (e: Exception) {
                // A serializer can fail without kaml turning it into a YamlException, and there is
                // no path to prune a single setting by without one.
                return allDefaults(UNREADABLE, e)
            }
        }
    }

    private fun allDefaults(
        what: String,
        cause: Exception,
    ): LunaticChatConfiguration {
        logger.log(
            Level.SEVERE,
            "$what, so EVERY setting fell back to its default (fix the reported value and restart): ${cause.message}",
            cause,
        )
        return LunaticChatConfiguration()
    }

    /** The config.yml keys leading to the node this path points at, outermost first. */
    private fun YamlPath.settingKeys(): List<String> = segments.filterIsInstance<YamlPathSegment.MapElementKey>().map { it.key }

    /** A copy of this document without [keys], or null when that entry is not there to remove. */
    private fun YamlNode.without(keys: List<String>): YamlNode? {
        if (this !is YamlMap || keys.isEmpty()) return null
        val key = entries.keys.firstOrNull { it.content == keys.first() } ?: return null
        if (keys.size == 1) return YamlMap(entries - key, path)
        val remaining = entries.getValue(key).without(keys.drop(1)) ?: return null
        return YamlMap(entries + (key to remaining), path)
    }
}
