package dev.m1sk9.lunaticChat.paper

import dev.m1sk9.lunaticChat.engine.converter.GoogleIMEClient
import dev.m1sk9.lunaticChat.paper.command.core.CommandRegistry
import dev.m1sk9.lunaticChat.paper.command.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.command.impl.DirectMessageNoticeToggleCommand
import dev.m1sk9.lunaticChat.paper.command.impl.ReplyCommand
import dev.m1sk9.lunaticChat.paper.command.impl.RomajiConvertToggleCommand
import dev.m1sk9.lunaticChat.paper.command.impl.TellCommand
import dev.m1sk9.lunaticChat.paper.common.SpyPermissionManager
import dev.m1sk9.lunaticChat.paper.common.UpdateCheckResult
import dev.m1sk9.lunaticChat.paper.common.UpdateChecker
import dev.m1sk9.lunaticChat.paper.config.ConfigManager
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.converter.ConversionCache
import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import dev.m1sk9.lunaticChat.paper.listener.PlayerChatListener
import dev.m1sk9.lunaticChat.paper.listener.PlayerPresenceListener
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import dev.m1sk9.lunaticChat.paper.settings.YamlPlayerSettingsStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class LunaticChat :
    JavaPlugin(),
    Listener {
    lateinit var directMessageHandler: DirectMessageHandler

    private lateinit var commandRegistry: CommandRegistry
    private var updateChecker: UpdateChecker? = null
    private var romajiConverter: RomanjiConverter? = null
    private var playerSettingsManager: PlayerSettingsManager? = null

    private val updateAvailable = AtomicBoolean(false)

    override fun onEnable() {
        saveDefaultConfig()
        val configuration = ConfigManager.loadConfiguration(config)

        if (configuration.debug) {
            logger.warning("LunaticChat is running in debug mode.")
            logger.info("Debug: $configuration")
        }

        val httpClient = HttpClient(CIO)

        // Initialize player settings manager (always needed for DM notifications)
        initializePlayerSettingsManager(configuration)

        // Initialize features
        if (configuration.features.japaneseConversion.enabled) {
            initializeJapaneseConversionFeature(configuration, httpClient)
        }

        // Initialize handlers
        directMessageHandler =
            DirectMessageHandler(
                settingsManager = playerSettingsManager,
                romanjiConverter = romajiConverter,
            )

        // Register commands and listeners
        registerCommands(configuration)
        registerEventListeners()

        // Check for updates
        if (configuration.checkForUpdates) {
            updateChecker =
                UpdateChecker(
                    currentVersion = pluginMeta.version,
                    logger = logger,
                    httpClient = httpClient,
                )
            server.scheduler.runTaskAsynchronously(
                this,
                Runnable {
                    runBlocking {
                        checkUpdates()
                    }
                },
            )
        }

        logger.info("LunaticChat enabled.")
    }

    override fun onDisable() {
        playerSettingsManager?.saveToDisk()
        logger.info("LunaticChat disabled.")
    }

    /**
     * Initializes the player settings manager.
     * This is always needed for features like DM notifications.
     */
    private fun initializePlayerSettingsManager(configuration: LunaticChatConfiguration) {
        val settingsFile = dataFolder.resolve(configuration.userSettingsFilePath).toPath()
        val storage =
            YamlPlayerSettingsStorage(
                settingsFile = settingsFile,
                plugin = this,
                logger = logger,
            )

        playerSettingsManager =
            PlayerSettingsManager(
                storage = storage,
                logger = logger,
            )
        playerSettingsManager!!.initialize()
    }

    /**
     * Initializes the Japanese conversion feature including:
     * - Conversion cache
     * - Google IME API client
     * - Romanji converter
     * - Periodic cache saving task
     */
    private fun initializeJapaneseConversionFeature(
        configuration: LunaticChatConfiguration,
        httpClient: HttpClient,
    ) {
        // Initialize conversion cache
        val cache =
            ConversionCache(
                cacheFile = dataFolder.resolve(configuration.features.japaneseConversion.cacheFilePath).toPath(),
                maxEntries = configuration.features.japaneseConversion.cacheMaxEntries,
                plugin = this,
                logger = logger,
            )
        cache.loadFromDisk()

        // Initialize Google IME API client
        val apiClient =
            GoogleIMEClient(
                timeout = configuration.features.japaneseConversion.apiTimeout.milliseconds,
                httpClient = httpClient,
            )

        // Initialize Romanji converter
        romajiConverter =
            RomanjiConverter(
                cache = cache,
                apiClient = apiClient,
                logger = logger,
                debugMode = configuration.debug,
            )

        // Schedule periodic cache saving
        val saveInterval = configuration.features.japaneseConversion.cacheSaveIntervalSeconds * 20L
        server.scheduler.runTaskTimerAsynchronously(
            this,
            Runnable {
                cache.saveToDisk()
            },
            saveInterval,
            saveInterval,
        )

        // Register event listener for Japanese conversion
        server.pluginManager.registerEvents(PlayerChatListener(romajiConverter!!, playerSettingsManager!!), this)

        logger.info("Japanese conversion feature enabled.")
    }

    /**
     * Registers all commands based on enabled features.
     */
    private fun registerCommands(configuration: LunaticChatConfiguration) {
        commandRegistry = CommandRegistry(this)

        commandRegistry.registerAll(
            TellCommand(this, directMessageHandler),
            DirectMessageNoticeToggleCommand(this, playerSettingsManager!!),
        )

        // Register /reply command if quick replies are enabled
        if (configuration.features.quickRepliesEnabled.enabled) {
            commandRegistry.registerAll(
                ReplyCommand(this, directMessageHandler),
            )
        }

        // Register /jp command if Japanese conversion is enabled
        if (configuration.features.japaneseConversion.enabled) {
            commandRegistry.registerAll(
                RomajiConvertToggleCommand(this, playerSettingsManager!!),
            )
        }

        commandRegistry.initialize()
    }

    /**
     * Registers all event listeners.
     */
    private fun registerEventListeners() {
        server.pluginManager.registerEvents(SpyPermissionManager, this)
        server.pluginManager.registerEvents(PlayerPresenceListener(this, updateAvailable), this)
    }

    private suspend fun checkUpdates() {
        val result = updateChecker?.checkForUpdates()
        when (result) {
            is UpdateCheckResult.ExistUpdate -> {
                logger.info("A new version of LunaticChat is available!")
                logger.info("You can download the latest build from GitHub or Modrinth.")
                logger.info("   GitHub: https://github.com/m1sk9/LunaticChat/releases/latest")
                logger.info("   Modrinth: https://modrinth.com/plugin/lunaticchat/version/latest")
                updateAvailable.set(true)
            }
            is UpdateCheckResult.NotUpdate -> {
                logger.info("LunaticChat is up to date.")
            }
            // Include failed case for completeness
            else -> {
                logger.warning("Failed to check for updates.")
            }
        }
    }
}
