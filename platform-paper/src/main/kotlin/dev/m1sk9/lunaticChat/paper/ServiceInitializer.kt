package dev.m1sk9.lunaticChat.paper

import dev.m1sk9.lunaticChat.engine.converter.GoogleIMEClient
import dev.m1sk9.lunaticChat.paper.command.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.converter.ConversionCache
import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import dev.m1sk9.lunaticChat.paper.settings.YamlPlayerSettingsStorage
import io.ktor.client.HttpClient
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger
import kotlin.time.Duration.Companion.milliseconds

/**
 * Handles initialization and shutdown of all plugin services.
 *
 * This class centralizes service initialization logic, ensuring proper
 * dependency order and eliminating the need for null-assertion operators.
 */
class ServiceInitializer(
    private val plugin: JavaPlugin,
    private val configuration: LunaticChatConfiguration,
    private val httpClient: HttpClient,
    private val logger: Logger,
) {
    private var conversionCache: ConversionCache? = null

    /**
     * Initializes all services in dependency order.
     *
     * Initialization order:
     * 1. LanguageManager (required by all features)
     * 2. PlayerSettingsManager (required for DM notifications)
     * 3. Japanese Conversion (optional, config-dependent)
     * 4. DirectMessageHandler (depends on settings manager and romaji converter)
     *
     * @return ServiceContainer with all initialized services
     */
    fun initialize(): ServiceContainer {
        // 1. Initialize language manager (BEFORE commands)
        val languageManager =
            LanguageManager(
                plugin = plugin,
                logger = logger,
                selectedLanguage = configuration.language,
            )
        languageManager.initialize()
        logger.info("Language system initialized: ${configuration.language.code}")

        // 2. Initialize player settings manager (always needed for DM notifications)
        val playerSettingsManager = initializePlayerSettingsManager()

        // 3. Initialize Japanese conversion (optional)
        val romajiConverter =
            if (configuration.features.japaneseConversion.enabled) {
                initializeJapaneseConversion()
            } else {
                null
            }

        // 4. Initialize handlers
        val directMessageHandler =
            DirectMessageHandler(
                settingsManager = playerSettingsManager,
                romanjiConverter = romajiConverter,
            )

        return ServiceContainer(
            languageManager = languageManager,
            playerSettingsManager = playerSettingsManager,
            directMessageHandler = directMessageHandler,
            romajiConverter = romajiConverter,
        )
    }

    /**
     * Initializes the player settings manager.
     * This is always needed for features like DM notifications.
     */
    private fun initializePlayerSettingsManager(): PlayerSettingsManager {
        val settingsFile = plugin.dataFolder.resolve(configuration.userSettingsFilePath).toPath()
        val storage =
            YamlPlayerSettingsStorage(
                settingsFile = settingsFile,
                plugin = plugin,
                logger = logger,
            )

        val playerSettingsManager =
            PlayerSettingsManager(
                storage = storage,
                logger = logger,
            )
        playerSettingsManager.initialize()
        return playerSettingsManager
    }

    /**
     * Initializes the Japanese conversion feature including:
     * - Conversion cache
     * - Google IME API client
     * - Romanji converter
     */
    private fun initializeJapaneseConversion(): RomanjiConverter {
        // Initialize conversion cache
        val cache =
            ConversionCache(
                cacheFile = plugin.dataFolder.resolve(configuration.features.japaneseConversion.cacheFilePath).toPath(),
                maxEntries = configuration.features.japaneseConversion.cacheMaxEntries,
                plugin = plugin,
                logger = logger,
            )
        cache.loadFromDisk()
        conversionCache = cache

        // Initialize Google IME API client
        val apiClient =
            GoogleIMEClient(
                timeout = configuration.features.japaneseConversion.apiTimeout.milliseconds,
                httpClient = httpClient,
            )

        // Initialize Romanji converter
        val converter =
            RomanjiConverter(
                cache = cache,
                apiClient = apiClient,
                logger = logger,
                debugMode = configuration.debug,
            )

        logger.info("Japanese conversion feature enabled.")
        return converter
    }

    /**
     * Schedules periodic tasks such as cache saving.
     */
    fun schedulePeriodicTasks() {
        if (configuration.features.japaneseConversion.enabled && conversionCache != null) {
            val saveInterval = configuration.features.japaneseConversion.cacheSaveIntervalSeconds * 20L
            plugin.server.scheduler.runTaskTimerAsynchronously(
                plugin,
                Runnable {
                    conversionCache?.saveToDisk()
                },
                saveInterval,
                saveInterval,
            )
        }
    }

    /**
     * Performs shutdown tasks, including saving all caches to disk.
     */
    fun shutdown(services: ServiceContainer) {
        services.playerSettingsManager.saveToDisk()
        conversionCache?.saveToDisk()
    }
}
