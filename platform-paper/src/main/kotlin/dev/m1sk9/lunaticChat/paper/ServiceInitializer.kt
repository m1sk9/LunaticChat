package dev.m1sk9.lunaticChat.paper

import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.engine.debug.DebugLogger
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMessageLogger
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelStorage
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelMessageHandler
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelNotificationHandler
import dev.m1sk9.lunaticChat.paper.chat.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.config.MessageFormatHolder
import dev.m1sk9.lunaticChat.paper.converter.ConversionCache
import dev.m1sk9.lunaticChat.paper.converter.GoogleIMEClient
import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import dev.m1sk9.lunaticChat.paper.settings.YamlPlayerSettingsStorage
import dev.m1sk9.lunaticChat.paper.storage.AsyncScheduler
import dev.m1sk9.lunaticChat.paper.storage.FileStore
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
import java.util.logging.Level
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
    private val messageFormats: MessageFormatHolder,
    private val httpClient: Lazy<HttpClient>,
    private val logger: Logger,
    private val debug: DebugLogger = DebugLogger.Disabled,
) {
    private val handshakeCompleted = AtomicBoolean(false)

    private val asyncScheduler =
        AsyncScheduler { delaySeconds, task ->
            plugin.server.asyncScheduler.runDelayed(plugin, { task() }, delaySeconds, TimeUnit.SECONDS)
        }

    /** A store for [relativePath] under the plugin's data folder, with its own debounced saver. */
    private fun fileStore(relativePath: String) = FileStore(plugin.dataFolder.resolve(relativePath).toPath(), asyncScheduler, logger, debug)

    private companion object {
        /** Matches the value documented in config.yml. */
        const val DEFAULT_CACHE_SAVE_INTERVAL_SECONDS = 300L
    }

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
                messageFormats = messageFormats,
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
        //
        // Gated on velocityManager alone: it is non-null only when velocityIntegration.enabled, so
        // testing that flag again here would let the two conditions disagree.
        val crossServerManager =
            velocityManager
                ?.takeIf { configuration.features.velocityIntegration.crossServerGlobalChat }
                ?.let { initializeCrossServerChatManager(it) }

        // 8. Initialize cross-server direct message manager and presence registry (optional)
        val crossServerDirectMessage =
            velocityManager
                ?.takeIf { configuration.features.velocityIntegration.crossServerDirectMessage }
                ?.let { initializeCrossServerDirectMessage(it, directMessageHandler, languageManager) }

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
            // Ordered: player-visible state is persisted first, then the log is flushed, and the
            // proxy connection is closed last so a relay in flight still has somewhere to go.
            stoppables =
                listOfNotNull(
                    playerSettingsManager,
                    japaneseConversion?.second,
                    channelComponents?.channelManager,
                    channelComponents?.channelMessageLogger,
                    velocityManager,
                ),
        )
    }

    /**
     * Initializes the player settings manager.
     * This is always needed for features like DM notifications.
     */
    private fun initializePlayerSettingsManager(): PlayerSettingsManager {
        val storage =
            YamlPlayerSettingsStorage(
                store = fileStore(configuration.userSettingsFilePath),
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
                store = fileStore(configuration.features.japaneseConversion.cache.filePath),
                maxEntries = configuration.features.japaneseConversion.cache.maxEntries,
                logger = logger,
            )
        cache.loadFromDisk()

        // Initialize Google IME API client
        val apiClient =
            GoogleIMEClient(
                timeout = configuration.features.japaneseConversion.api.timeout.milliseconds,
                httpClient = httpClient.value,
            )

        // Initialize Romanji converter
        val converter =
            RomanjiConverter(
                cache = cache,
                apiClient = apiClient,
                logger = logger,
                debug = debug,
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
        val storage =
            ChannelStorage(
                store = fileStore("channels.json"),
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
                debug = debug,
                config = configuration.features.channelChat,
            )

        // Initialize channel message logger if enabled
        val messageLogger =
            if (configuration.features.channelChat.messageLogging.enabled) {
                val logsDir = plugin.dataFolder.resolve("logs/channelchat").toPath()
                ChannelMessageLogger(
                    logsDirectory = logsDir,
                    plugin = plugin,
                    logger = logger,
                    debug = debug,
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
                messageFormats = messageFormats,
                settingsManager = settingsManager,
                channelManager = manager,
                languageManager = languageManager,
                messageLogger = messageLogger,
                debug = debug,
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
                debug = debug,
            )
        manager.initialize()

        // Register listener for first player join
        plugin.server.pluginManager.registerEvents(
            object : Listener {
                @EventHandler
                fun onPlayerJoin(event: PlayerJoinEvent) {
                    // Only perform handshake once
                    if (!handshakeCompleted.getAndSet(true)) {
                        debug.log(DebugCategory.VELOCITY) {
                            "First player joined (${event.player.name}); scheduling the handshake in 1 second"
                        }
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
                debug = debug,
                configuration = configuration,
                messageFormats = messageFormats,
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
                debug = debug,
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
                        debug.log(DebugCategory.VELOCITY) { "Handshake failed while the connection was ${manager.getState()}" }
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
            // The periodic task is the only writer besides shutdown, so a non-positive interval
            // would both be rejected by runAtFixedRate and leave the cache unsaved until the
            // server stopped. Fall back to the documented default rather than to one second,
            // which would rewrite the whole cache file every tick anyone chatted.
            val configuredInterval = configuration.features.japaneseConversion.cache.saveIntervalSeconds
            val intervalSeconds =
                if (configuredInterval > 0) {
                    configuredInterval.toLong()
                } else {
                    logger.warning(
                        "features.japaneseConversion.cache.saveIntervalSeconds must be positive; " +
                            "using $DEFAULT_CACHE_SAVE_INTERVAL_SECONDS seconds instead of $configuredInterval",
                    )
                    DEFAULT_CACHE_SAVE_INTERVAL_SECONDS
                }
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
        // The services are independent, so one that throws must not skip the ones after it - which is
        // what an exception escaping onDisable would do, leaving the log flusher and the Velocity
        // connection to be torn down by the server instead.
        services.stoppables.forEach { service ->
            try {
                service.stop()
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed to stop ${service::class.simpleName} during shutdown", e)
            }
        }
    }
}
