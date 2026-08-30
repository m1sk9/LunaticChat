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

/** A setting that could not be read, and so fell back to the default on the data class. */
data class SettingFallback(
    val settingKey: String,
    val reason: String,
)

/** The outcome of reading config.yml through [ConfigManager.loadStrictly]. */
sealed interface ConfigLoadResult {
    data class Success(
        val configuration: LunaticChatConfiguration,
    ) : ConfigLoadResult

    /** Every setting that could not be read, so the operator fixes them in one pass. */
    data class InvalidSettings(
        val fallbacks: List<SettingFallback>,
    ) : ConfigLoadResult

    /** The failure cannot be pinned on a setting: not YAML, empty, or not a map at the top level. */
    data class InvalidDocument(
        val reason: String,
    ) : ConfigLoadResult
}

/**
 * Reads config.yml into [LunaticChatConfiguration].
 *
 * The file is deserialized directly rather than copied key by key, so a default lives only on the
 * data class. The hand-written mapper it replaced repeated every default in a second place, and
 * they had already drifted - checkForUpdates disagreed with both config.yml and the data class,
 * and the whole messageLogging block was documented but never read.
 *
 * Two readings are offered because startup and reload want opposite things from a bad file:
 * [loadConfiguration] keeps the server coming up, [loadStrictly] keeps a running server on the
 * configuration it already has.
 */
class ConfigManager(
    private val logger: Logger,
) {
    private companion object {
        const val UNREADABLE = "config.yml could not be read"
        const val NOT_YAML = "config.yml is not valid YAML"
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
    fun loadConfiguration(contents: String): LunaticChatConfiguration =
        when (val outcome = parse(contents)) {
            // A file that only holds comments is a valid way of saying "use the defaults", so it
            // is not reported as a failure the operator has to act on.
            is ParseOutcome.Empty -> LunaticChatConfiguration()
            is ParseOutcome.Undecodable -> allDefaults(outcome.what, outcome.cause)
            is ParseOutcome.Decoded -> {
                outcome.fallbacks.forEach {
                    logger.warning("${it.settingKey} in config.yml fell back to its default: ${it.reason}")
                }
                outcome.configuration
            }
        }

    /**
     * Parses [contents] as config.yml, refusing anything it cannot read in full.
     *
     * Reload has an option startup does not: leaving the running configuration alone. Falling a
     * single setting back to its default here would silently undo a value the operator had set and
     * report success, so the whole file is rejected and every unreadable setting is named at once.
     */
    fun loadStrictly(contents: String): ConfigLoadResult =
        when (val outcome = parse(contents)) {
            // Not treated as "use the defaults" like the lenient path does: a reload cannot tell a
            // comments-only file apart from one caught mid-write by an editor or an SFTP upload,
            // and resetting every message format over that is the worst of the two mistakes.
            is ParseOutcome.Empty -> ConfigLoadResult.InvalidDocument("config.yml holds no settings")
            is ParseOutcome.Undecodable -> ConfigLoadResult.InvalidDocument("${outcome.what}: ${outcome.cause.message}")
            is ParseOutcome.Decoded ->
                if (outcome.fallbacks.isEmpty()) {
                    ConfigLoadResult.Success(outcome.configuration)
                } else {
                    ConfigLoadResult.InvalidSettings(outcome.fallbacks)
                }
        }

    private sealed interface ParseOutcome {
        data class Decoded(
            val configuration: LunaticChatConfiguration,
            val fallbacks: List<SettingFallback>,
        ) : ParseOutcome

        data class Undecodable(
            val what: String,
            val cause: Exception,
        ) : ParseOutcome

        /** Valid YAML that carries no settings. */
        data object Empty : ParseOutcome
    }

    private fun parse(contents: String): ParseOutcome {
        var document =
            try {
                // Editors that write a UTF-8 BOM would otherwise leave it on the first key, which
                // strictMode = false then drops as an unknown setting without a word.
                yaml.parseToYamlNode(contents.removePrefix("\uFEFF"))
            } catch (_: EmptyYamlDocumentException) {
                return ParseOutcome.Empty
            } catch (e: Exception) {
                // Not YamlException: a file the reader rejects before it is YAML at all - one saved
                // as UTF-16, or truncated with NUL padding - fails inside the scanner, and letting
                // that out of onEnable would disable the plugin over a config file.
                return ParseOutcome.Undecodable(NOT_YAML, e)
            }

        val fallbacks = mutableListOf<SettingFallback>()

        // Each pass drops exactly one setting, so this terminates: the document strictly shrinks
        // until it decodes or there is nothing left to drop.
        while (true) {
            try {
                return ParseOutcome.Decoded(yaml.decodeFromYamlNode(LunaticChatConfiguration.serializer(), document), fallbacks)
            } catch (e: YamlException) {
                // kaml rejects the document as a whole, so without this one unreadable value would
                // lose every other setting in the file - a regression against the hand-written
                // mapper, which defaulted per key. Pruning also lets the strict reading report
                // every bad setting at once instead of one per attempt.
                val setting = e.path.settingKeys()
                // An empty path means the failure is not under a key we can drop - the document is
                // not a map at the top level - and pruning nothing would spin forever.
                val remaining =
                    document.without(setting)
                        ?: return ParseOutcome.Undecodable(UNREADABLE, e)
                fallbacks += SettingFallback(setting.joinToString("."), e.message ?: e.toString())
                document = remaining
            } catch (e: Exception) {
                // A serializer can fail without kaml turning it into a YamlException, and there is
                // no path to prune a single setting by without one. No config.yml reaches this -
                // only a serializer bug does - so it is deliberately left without a test.
                return ParseOutcome.Undecodable(UNREADABLE, e)
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
