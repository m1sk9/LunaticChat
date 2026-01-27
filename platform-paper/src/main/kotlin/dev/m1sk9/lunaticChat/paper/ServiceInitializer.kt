package dev.m1sk9.lunaticChat.paper

import dev.m1sk9.lunaticChat.engine.converter.GoogleIMEClient
import dev.m1sk9.lunaticChat.paper.chat.ChatModeManager
import dev.m1sk9.lunaticChat.paper.chat.ChatModeStorage
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelStorage
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelMessageHandler
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelNotificationHandler
import dev.m1sk9.lunaticChat.paper.chat.handler.DirectMessageHandler
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
 * Container for channel-related components.
 */
private data class ChannelComponents(
    val channelManager: ChannelManager,
    val channelMembershipManager: ChannelMembershipManager,
    val chatModeManager: ChatModeManager,
    val channelMessageHandler: ChannelMessageHandler,
    val channelNotificationHandler: ChannelNotificationHandler,
)

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
    private var channelManager: ChannelManager? = null
    private var channelMembershipManager: ChannelMembershipManager? = null
    private var chatModeManager: ChatModeManager? = null
    private var channelMessageHandler: ChannelMessageHandler? = null
    private var channelNotificationHandler: ChannelNotificationHandler? = null

    /**
     * Initializes all services in dependency order.
     *
     * Initialization order:
     * 1. LanguageManager (required by all features)
     * 2. PlayerSettingsManager (required for DM notifications)
     * 3. Japanese Conversion (optional, config-dependent)
     * 4. ChannelStorage
     * 5. DirectMessageHandler (depends on settings manager and romaji converter)
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

        // 4. Initialize channel manager, membership manager, chat mode manager, channel message handler, and notification handler
        val channelComponents =
            if (configuration.features.channelChat.enabled) {
                initializeChannelManager(playerSettingsManager, romajiConverter, languageManager)
            } else {
                null
            }
        val channelManager = channelComponents?.channelManager
        val channelMembershipManager = channelComponents?.channelMembershipManager
        val chatModeManager = channelComponents?.chatModeManager
        val channelMessageHandler = channelComponents?.channelMessageHandler
        val channelNotificationHandler = channelComponents?.channelNotificationHandler

        // 5. Initialize handlers
        val directMessageHandler =
            DirectMessageHandler(
                configuration = configuration,
                settingsManager = playerSettingsManager,
                romanjiConverter = romajiConverter,
                languageManager = languageManager,
            )

        return ServiceContainer(
            languageManager = languageManager,
            playerSettingsManager = playerSettingsManager,
            directMessageHandler = directMessageHandler,
            romajiConverter = romajiConverter,
            channelManager = channelManager,
            channelMembershipManager = channelMembershipManager,
            chatModeManager = chatModeManager,
            channelMessageHandler = channelMessageHandler,
            channelNotificationHandler = channelNotificationHandler,
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
     * Initializes channel manager, membership manager, chat mode manager, channel message handler, and notification handler with storage.
     */
    private fun initializeChannelManager(
        settingsManager: PlayerSettingsManager,
        romajiConverter: RomanjiConverter?,
        languageManager: LanguageManager,
    ): ChannelComponents {
        val channelsFile = plugin.dataFolder.resolve("channels.json").toPath()
        val storage =
            ChannelStorage(
                channelsFile = channelsFile,
                plugin = plugin,
                logger = logger,
            )

        val manager =
            ChannelManager(
                storage = storage,
                logger = logger,
                config = configuration.features.channelChat,
            )
        manager.initialize()
        channelManager = manager

        val membershipManager =
            ChannelMembershipManager(
                channelManager = manager,
                logger = logger,
                config = configuration.features.channelChat,
            )
        channelMembershipManager = membershipManager

        val chatModeFile = plugin.dataFolder.resolve("chatmodes.json").toPath()
        val chatModeStorage =
            ChatModeStorage(
                dataFile = chatModeFile,
                logger = logger,
            )

        val chatMode =
            ChatModeManager(
                storage = chatModeStorage,
                logger = logger,
            )
        chatMode.initialize()
        chatModeManager = chatMode

        val messageHandler =
            ChannelMessageHandler(
                configuration = configuration,
                settingsManager = settingsManager,
                channelManager = manager,
                romanjiConverter = romajiConverter,
                languageManager = languageManager,
                logger =
                    io.ktor.util.logging
                        .KtorSimpleLogger("ChannelMessageHandler"),
            )
        channelMessageHandler = messageHandler

        val notificationHandler =
            ChannelNotificationHandler(
                channelManager = manager,
                languageManager = languageManager,
            )
        channelNotificationHandler = notificationHandler

        logger.info(
            "Channel manager, membership manager, chat mode manager, " +
                "channel message handler, and notification handler initialized successfully.",
        )
        return ChannelComponents(
            channelManager = manager,
            channelMembershipManager = membershipManager,
            chatModeManager = chatMode,
            channelMessageHandler = messageHandler,
            channelNotificationHandler = notificationHandler,
        )
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
        services.channelManager?.saveToDisk()
        services.chatModeManager?.shutdown()
    }
}
