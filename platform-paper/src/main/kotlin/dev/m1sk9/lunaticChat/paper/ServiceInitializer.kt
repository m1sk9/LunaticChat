package dev.m1sk9.lunaticChat.paper

import dev.m1sk9.lunaticChat.engine.converter.GoogleIMEClient
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMessageLogger
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
import dev.m1sk9.lunaticChat.paper.velocity.CrossServerChatManager
import dev.m1sk9.lunaticChat.paper.velocity.CrossServerDirectMessageManager
import dev.m1sk9.lunaticChat.paper.velocity.RemotePlayerRegistry
import dev.m1sk9.lunaticChat.paper.velocity.VelocityConnectionManager
import io.ktor.client.HttpClient
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger
import kotlin.time.Duration.Companion.milliseconds

/**
 * Container for channel-related components.
 */
private data class ChannelComponents(
    val channelManager: ChannelManager,
    val channelMembershipManager: ChannelMembershipManager,
    val channelMessageHandler: ChannelMessageHandler,
    val channelNotificationHandler: ChannelNotificationHandler,
    val channelMessageLogger: ChannelMessageLogger?,
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
    private val httpClient: Lazy<HttpClient>,
    private val logger: Logger,
) {
    private val handshakeCompleted = AtomicBoolean(false)

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
        val japaneseConversion =
            if (configuration.features.japaneseConversion.enabled) {
                initializeJapaneseConversion()
            } else {
                null
            }
        val romajiConverter = japaneseConversion?.first

        // 4. Initialize channel manager, membership manager, channel message handler, and notification handler
        val channelComponents =
            if (configuration.features.channelChat.enabled) {
                initializeChannelManager(playerSettingsManager, languageManager)
            } else {
                null
            }
        // 5. Initialize handlers
        val directMessageHandler =
            DirectMessageHandler(
                configuration = configuration,
                settingsManager = playerSettingsManager,
                romanjiConverter = romajiConverter,
                languageManager = languageManager,
            )

        // 6. Initialize Velocity integration (optional)
        val velocityManager =
            if (configuration.features.velocityIntegration.enabled) {
                initializeVelocityIntegration()
            } else {
                null
            }

        // 7. Initialize cross-server chat manager (optional)
        val crossServerManager =
            if (configuration.features.velocityIntegration.enabled &&
                configuration.features.velocityIntegration.crossServerGlobalChat &&
                velocityManager != null
            ) {
                initializeCrossServerChatManager(velocityManager)
            } else {
                null
            }

        // 8. Initialize cross-server direct message manager and presence registry (optional)
        val crossServerDirectMessage =
            if (configuration.features.velocityIntegration.enabled &&
                configuration.features.velocityIntegration.crossServerDirectMessage &&
                velocityManager != null
            ) {
                initializeCrossServerDirectMessage(velocityManager, directMessageHandler, languageManager)
            } else {
                null
            }

        return ServiceContainer(
            languageManager = languageManager,
            playerSettingsManager = playerSettingsManager,
            directMessageHandler = directMessageHandler,
            romajiConverter = romajiConverter,
            conversionCache = japaneseConversion?.second,
            channelManager = channelComponents?.channelManager,
            channelMembershipManager = channelComponents?.channelMembershipManager,
            channelMessageLogger = channelComponents?.channelMessageLogger,
            channelMessageHandler = channelComponents?.channelMessageHandler,
            channelNotificationHandler = channelComponents?.channelNotificationHandler,
            velocityConnectionManager = velocityManager,
            crossServerChatManager = crossServerManager,
            crossServerDirectMessageManager = crossServerDirectMessage?.first,
            remotePlayerRegistry = crossServerDirectMessage?.second,
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
                saver = DebouncedSaver(plugin),
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
    private fun initializeJapaneseConversion(): Pair<RomanjiConverter, ConversionCache> {
        // Initialize conversion cache
        val cache =
            ConversionCache(
                cacheFile = plugin.dataFolder.resolve(configuration.features.japaneseConversion.cacheFilePath).toPath(),
                maxEntries = configuration.features.japaneseConversion.cacheMaxEntries,
                logger = logger,
            )
        cache.loadFromDisk()

        // Initialize Google IME API client
        val apiClient =
            GoogleIMEClient(
                timeout = configuration.features.japaneseConversion.apiTimeout.milliseconds,
                httpClient = httpClient.value,
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
        return converter to cache
    }

    /**
     * Initializes channel manager, membership manager, channel message handler, and notification handler with storage.
     */
    private fun initializeChannelManager(
        settingsManager: PlayerSettingsManager,
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

        val membershipManager =
            ChannelMembershipManager(
                channelManager = manager,
                logger = logger,
                config = configuration.features.channelChat,
            )

        // Initialize channel message logger if enabled
        val messageLogger =
            if (configuration.features.channelChat.messageLogging.enabled) {
                val logsDir = plugin.dataFolder.resolve("logs/channelchat").toPath()
                ChannelMessageLogger(
                    logsDirectory = logsDir,
                    plugin = plugin,
                    logger =
                        io.ktor.util.logging
                            .KtorSimpleLogger("ChannelMessageLogger"),
                    maxFileSizeBytes = configuration.features.channelChat.messageLogging.maxFileSizeMB * 1024L * 1024L,
                    retentionDays = configuration.features.channelChat.messageLogging.retentionDays,
                ).also {
                    logger.info(
                        "Channel message logging enabled (retention: ${configuration.features.channelChat.messageLogging.retentionDays} days)",
                    )
                }
            } else {
                null
            }

        val messageHandler =
            ChannelMessageHandler(
                configuration = configuration,
                settingsManager = settingsManager,
                channelManager = manager,
                languageManager = languageManager,
                messageLogger = messageLogger,
                logger =
                    io.ktor.util.logging
                        .KtorSimpleLogger("ChannelMessageHandler"),
            )

        val notificationHandler =
            ChannelNotificationHandler(
                channelManager = manager,
                languageManager = languageManager,
            )

        logger.info(
            "Channel manager, membership manager, " +
                "channel message handler, and notification handler initialized successfully.",
        )
        return ChannelComponents(
            channelManager = manager,
            channelMembershipManager = membershipManager,
            channelMessageHandler = messageHandler,
            channelNotificationHandler = notificationHandler,
            channelMessageLogger = messageLogger,
        )
    }

    /**
     * Initializes Velocity integration with handshake on first player join.
     */
    private fun initializeVelocityIntegration(): VelocityConnectionManager {
        val pluginVersion = plugin.pluginMeta.version
        val manager =
            VelocityConnectionManager(
                plugin = plugin,
                pluginVersion = pluginVersion,
                logger = logger,
            )
        manager.initialize()

        // Register listener for first player join
        plugin.server.pluginManager.registerEvents(
            object : Listener {
                @EventHandler
                fun onPlayerJoin(event: PlayerJoinEvent) {
                    // Only perform handshake once
                    if (!handshakeCompleted.getAndSet(true)) {
                        // Schedule handshake 1 second after first player joins
                        plugin.server.asyncScheduler.runDelayed(
                            plugin,
                            {
                                performVelocityHandshake(event.player, manager)
                            },
                            1,
                            TimeUnit.SECONDS,
                        )
                    }
                }
            },
            plugin,
        )

        logger.info("Velocity integration initialized. Waiting for first player join to perform handshake.")
        return manager
    }

    /**
     * Initializes cross-server chat manager.
     *
     * @param velocityManager The Velocity connection manager
     * @return The initialized CrossServerChatManager
     */
    private fun initializeCrossServerChatManager(velocityManager: VelocityConnectionManager): CrossServerChatManager {
        val manager =
            CrossServerChatManager(
                plugin = plugin,
                logger = logger,
                configuration = configuration,
                cacheSize = configuration.features.velocityIntegration.messageDeduplicationCacheSize,
            )

        // Set the manager in VelocityConnectionManager to handle incoming messages
        velocityManager.setCrossServerChatManager(manager)

        logger.info(
            "Cross-server global chat initialized (cache size: ${configuration.features.velocityIntegration.messageDeduplicationCacheSize})",
        )
        return manager
    }

    /**
     * Initializes cross-server direct messaging: the presence registry and the
     * direct message manager, wiring them into the Velocity connection manager
     * and the direct message handler.
     */
    private fun initializeCrossServerDirectMessage(
        velocityManager: VelocityConnectionManager,
        directMessageHandler: DirectMessageHandler,
        languageManager: LanguageManager,
    ): Pair<CrossServerDirectMessageManager, RemotePlayerRegistry> {
        val registry = RemotePlayerRegistry(configuration.features.velocityIntegration.serverName)
        directMessageHandler.remotePlayerRegistry = registry

        val manager =
            CrossServerDirectMessageManager(
                plugin = plugin,
                logger = logger,
                configuration = configuration,
                directMessageHandler = directMessageHandler,
                languageManager = languageManager,
                cacheSize = configuration.features.velocityIntegration.messageDeduplicationCacheSize,
            )

        velocityManager.setCrossServerDirectMessageManager(manager, registry)

        logger.info("Cross-server direct messages initialized")
        return manager to registry
    }

    /**
     * Performs handshake with Velocity proxy.
     */
    private fun performVelocityHandshake(
        player: org.bukkit.entity.Player,
        manager: VelocityConnectionManager,
    ) {
        manager
            .performHandshake(player)
            .thenAccept { result ->
                when (result) {
                    is VelocityConnectionManager.HandshakeResult.Success -> {
                        logger.info("Velocity handshake successful with version ${result.velocityVersion}")
                    }
                    is VelocityConnectionManager.HandshakeResult.Error -> {
                        logger.severe("Velocity handshake failed: ${result.message}")
                        logger.severe("Velocity integration is disabled. Use /lcv status to check the status.")
                    }
                }
            }.exceptionally { throwable ->
                logger.severe("Velocity handshake exception: ${throwable.message}")
                logger.severe("Velocity integration is disabled. Use /lcv status to check the status.")
                throwable.printStackTrace()
                null
            }
    }

    /**
     * Schedules periodic tasks such as cache saving.
     * Uses Folia-compatible AsyncScheduler API.
     */
    fun schedulePeriodicTasks(services: ServiceContainer) {
        val conversionCache = services.conversionCache
        if (conversionCache != null) {
            val intervalSeconds =
                configuration.features.japaneseConversion
                    .cacheSaveIntervalSeconds
                    .toLong()
            plugin.server.asyncScheduler.runAtFixedRate(
                plugin,
                { conversionCache.saveToDisk() },
                intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS,
            )
        }
    }

    /**
     * Performs shutdown tasks, including saving all caches to disk.
     */
    fun shutdown(services: ServiceContainer) {
        services.playerSettingsManager.saveToDisk()
        services.conversionCache?.saveToDisk()
        services.channelManager?.saveToDisk()
        services.channelMessageLogger?.shutdown()
        services.velocityConnectionManager?.shutdown()
    }
}
