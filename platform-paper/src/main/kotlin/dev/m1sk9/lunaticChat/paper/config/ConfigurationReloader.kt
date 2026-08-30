package dev.m1sk9.lunaticChat.paper.config

import dev.m1sk9.lunaticChat.paper.config.key.MessageFormatConfig
import java.util.logging.Logger

/** What a reload did to the running configuration. */
sealed interface ReloadResult {
    /**
     * config.yml was read in full.
     *
     * @property applied settings now in effect that were not before.
     * @property restartRequired settings the file changed that the running server cannot pick up.
     */
    data class Applied(
        val applied: List<String>,
        val restartRequired: List<String>,
    ) : ReloadResult

    data class InvalidSettings(
        val fallbacks: List<SettingFallback>,
    ) : ReloadResult

    data class InvalidDocument(
        val reason: String,
    ) : ReloadResult

    data class Unreadable(
        val reason: String,
    ) : ReloadResult
}

/**
 * Re-reads config.yml and applies what can be applied without restarting the server.
 *
 * Only [MessageFormatConfig] is applied. Everything else in config.yml decides something that is
 * settled once, at startup: which services exist, which commands and listeners are registered,
 * which files are opened. Paper registers commands only through its `COMMANDS` lifecycle event,
 * Bukkit offers no way to unregister a listener that was registered for a plugin, and the cache
 * saver retains no task handle to cancel - so rebuilding the services would leave the old ones
 * still wired up. Rather than pretend, a changed setting the server cannot pick up is reported
 * back to the operator as needing a restart.
 */
class ConfigurationReloader(
    private val configManager: ConfigManager,
    private val startupConfiguration: LunaticChatConfiguration,
    private val messageFormatHolder: MessageFormatHolder,
    private val logger: Logger,
    private val readConfigFile: () -> String,
) {
    internal companion object {
        /**
         * The settings a reload applies, by leaf.
         *
         * Leaves rather than the whole block, because the operator wants to see which format they
         * just changed. [MessageFormatConfig] is a closed set of three strings and a test fails if
         * it grows without this table growing with it.
         */
        internal val APPLIED: List<Pair<String, (MessageFormatConfig) -> Any?>> =
            listOf(
                "messageFormat.directMessageFormat" to { it: MessageFormatConfig -> it.directMessageFormat },
                "messageFormat.channelMessageFormat" to { it: MessageFormatConfig -> it.channelMessageFormat },
                "messageFormat.crossServerGlobalChatFormat" to { it: MessageFormatConfig -> it.crossServerGlobalChatFormat },
            )

        /**
         * The settings a reload cannot apply, by block.
         *
         * Blocks rather than leaves: the comparison rides on the data class equals, so a leaf added
         * to any of these is covered without touching this table.
         */
        internal val RESTART_REQUIRED: List<Pair<String, (LunaticChatConfiguration) -> Any?>> =
            listOf(
                "features.quickReplies" to { it: LunaticChatConfiguration -> it.features.quickReplies },
                "features.japaneseConversion" to { it: LunaticChatConfiguration -> it.features.japaneseConversion },
                "features.channelChat" to { it: LunaticChatConfiguration -> it.features.channelChat },
                "features.velocityIntegration" to { it: LunaticChatConfiguration -> it.features.velocityIntegration },
                "debug" to { it: LunaticChatConfiguration -> it.debug },
                "userSettingsFilePath" to { it: LunaticChatConfiguration -> it.userSettingsFilePath },
                "checkForUpdates" to { it: LunaticChatConfiguration -> it.checkForUpdates },
                "language" to { it: LunaticChatConfiguration -> it.language },
            )
    }

    /**
     * Reads config.yml and applies the message formats, leaving the running configuration untouched
     * when the file cannot be read in full.
     */
    fun reload(): ReloadResult {
        // saveDefaultConfig() is deliberately not called first: a config.yml that has been deleted
        // or is momentarily absent would be rewritten with the defaults and then applied, which is
        // the one outcome an operator running a reload cannot have meant.
        val contents =
            runCatching(readConfigFile).getOrElse { e ->
                logger.warning("/lc reload could not read config.yml: ${e.message}")
                return ReloadResult.Unreadable(e.message ?: e.toString())
            }

        return when (val loaded = configManager.loadStrictly(contents)) {
            is ConfigLoadResult.InvalidSettings -> {
                logger.warning(
                    "/lc reload rejected config.yml; these settings could not be read: " +
                        loaded.fallbacks.joinToString(", ") { it.settingKey },
                )
                ReloadResult.InvalidSettings(loaded.fallbacks)
            }

            is ConfigLoadResult.InvalidDocument -> {
                logger.warning("/lc reload rejected config.yml: ${loaded.reason}")
                ReloadResult.InvalidDocument(loaded.reason)
            }

            is ConfigLoadResult.Success -> apply(loaded.configuration)
        }
    }

    private fun apply(incoming: LunaticChatConfiguration): ReloadResult {
        // The two lists are measured against different baselines on purpose. What a reload applies
        // is new relative to the formats currently in effect, which an earlier reload may already
        // have moved; what needs a restart is new relative to what the server actually started on,
        // which no reload can move.
        val applied = APPLIED.filter { (_, read) -> read(messageFormatHolder.current) != read(incoming.messageFormat) }.map { it.first }
        val restartRequired = RESTART_REQUIRED.filter { (_, read) -> read(startupConfiguration) != read(incoming) }.map { it.first }

        messageFormatHolder.replace(incoming.messageFormat)

        // Logged as well as reported to the sender, so a format change can still be dated from the
        // server log long after the chat message that announced it has scrolled away.
        logger.info(
            "Reloaded config.yml. Applied: ${applied.ifEmpty { listOf("(nothing)") }.joinToString(", ")}. " +
                "Needs a restart: ${restartRequired.ifEmpty { listOf("(nothing)") }.joinToString(", ")}",
        )
        return ReloadResult.Applied(applied, restartRequired)
    }
}
