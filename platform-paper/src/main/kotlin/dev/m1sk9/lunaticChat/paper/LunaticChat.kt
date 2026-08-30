package dev.m1sk9.lunaticChat.paper

import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.engine.debug.DebugLogger
import dev.m1sk9.lunaticChat.engine.debug.DebugState
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelNotificationHandler
import dev.m1sk9.lunaticChat.paper.chat.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.command.core.CommandRegistry
import dev.m1sk9.lunaticChat.paper.command.impl.ReplyCommand
import dev.m1sk9.lunaticChat.paper.command.impl.TellCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.LunaticChatCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lcv.VelocityStatusCommand
import dev.m1sk9.lunaticChat.paper.command.setting.SettingHandler
import dev.m1sk9.lunaticChat.paper.command.setting.SettingHandlerRegistry
import dev.m1sk9.lunaticChat.paper.command.setting.SettingKey
import dev.m1sk9.lunaticChat.paper.common.UpdateCheckResult
import dev.m1sk9.lunaticChat.paper.common.UpdateChecker
import dev.m1sk9.lunaticChat.paper.config.ConfigManager
import dev.m1sk9.lunaticChat.paper.config.ConfigurationReloader
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.config.MessageFormatHolder
import dev.m1sk9.lunaticChat.paper.debug.DiagnosticsReport
import dev.m1sk9.lunaticChat.paper.debug.JulDebugLogger
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.listener.EventListenerRegistry
import dev.m1sk9.lunaticChat.paper.velocity.VelocityConnectionManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.launch
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.atomic.AtomicBoolean

class LunaticChat :
    JavaPlugin(),
    Listener {
    private companion object {
        val KNOWN_DEBUG_CATEGORIES = DebugCategory.entries.joinToString(", ") { it.key }
    }

    // Read by commands, which reach the plugin instance but not the container.
    val directMessageHandler: DirectMessageHandler get() = services.directMessageHandler
    val languageManager: LanguageManager get() = services.languageManager
    val channelManager: ChannelManager? get() = services.channelManager
    val channelMembershipManager: ChannelMembershipManager? get() = services.channelMembershipManager
    val channelNotificationHandler: ChannelNotificationHandler? get() = services.channelNotificationHandler
    val velocityConnectionManager: VelocityConnectionManager? get() = services.velocityConnectionManager

    /** Read by the command layer, which reports permission denials and results under `command`. */
    val debug: DebugLogger get() = debugLogger

    private lateinit var services: ServiceContainer
    private lateinit var debugState: DebugState
    private lateinit var debugLogger: DebugLogger
    private lateinit var configuration: LunaticChatConfiguration
    private lateinit var serviceInitializer: ServiceInitializer
    private lateinit var messageFormats: MessageFormatHolder
    private lateinit var configurationReloader: ConfigurationReloader

    private lateinit var pluginScope: PluginCoroutineScope

    /**
     * Serializes each player's outgoing messages so they arrive in the order they were sent.
     *
     * Commands submit delivery here rather than running it inline: romaji conversion can reach the
     * Google IME API, and a command executor runs on the tick thread.
     */
    lateinit var deliveryQueue: PerPlayerWorkQueue
        private set
    private var updateChecker: UpdateChecker? = null

    private val updateAvailable = AtomicBoolean(false)

    // Only the Japanese conversion and update-check features make HTTP calls, and both default
    // to off, so a stock install should not pay for a CIO engine and its thread pool.
    private val httpClient = lazy { HttpClient(CIO) }

    override fun onEnable() {
        saveDefaultConfig()
        val configManager = ConfigManager(logger)
        configuration = configManager.loadConfiguration(readConfigFile())

        messageFormats = MessageFormatHolder(configuration.messageFormat)
        debugState = DebugState(configuration.debug.activeCategories)
        debugLogger = JulDebugLogger(logger, debugState)
        configurationReloader =
            ConfigurationReloader(
                configManager = configManager,
                startupConfiguration = configuration,
                messageFormatHolder = messageFormats,
                debugState = debugState,
                logger = logger,
                // Throws rather than falling back to an empty document like startup does: a reload
                // that cannot read the file must leave the running configuration alone.
                readConfigFile = { configFile.readText() },
            )

        reportDebugSwitch()

        // Initialize plugin coroutine scope
        pluginScope = PluginCoroutineScope(logger)
        deliveryQueue = PerPlayerWorkQueue(pluginScope.scope, logger)

        // Initialize all services
        serviceInitializer =
            ServiceInitializer(
                plugin = this,
                configuration = configuration,
                messageFormats = messageFormats,
                httpClient = httpClient,
                logger = logger,
                debug = debugLogger,
            )
        services = serviceInitializer.initialize()

        // Schedule periodic tasks
        serviceInitializer.schedulePeriodicTasks(services)

        // Register commands and listeners
        registerCommands()
        registerEventListeners()

        // Check for updates
        if (configuration.checkForUpdates) {
            initializeUpdateChecker(httpClient.value)
        }

        logger.info("LunaticChat enabled.")
    }

    override fun onDisable() {
        pluginScope.cancel()
        serviceInitializer.shutdown(services)
        if (httpClient.isInitialized()) httpClient.value.close()
        logger.info("LunaticChat disabled.")
    }

    /**
     * Announces which debug categories are on, and names any config.yml asked for that do not exist.
     */
    private fun reportDebugSwitch() {
        configuration.debug.unknownCategories.forEach { name ->
            logger.warning("Unknown debug category in config.yml: '$name'. Known categories: $KNOWN_DEBUG_CATEGORIES")
        }
        if (debugState.enabled.isEmpty()) return

        logger.warning("LunaticChat is running in debug mode (${debugState.enabled.joinToString(", ") { it.key }}).")
        debugLogger.log(DebugCategory.CONFIG) { "Startup configuration: $configuration" }
    }

    private val configFile get() = dataFolder.resolve("config.yml")

    /**
     * Reads config.yml, or an empty document when it cannot be read.
     *
     * [saveDefaultConfig] only logs when it fails to write the file, so the read can still find
     * nothing there. Handing the parser an empty document starts the plugin on its defaults
     * instead of throwing out of [onEnable] and disabling it outright.
     */
    private fun readConfigFile(): String =
        runCatching { configFile.readText() }.getOrElse { e ->
            logger.severe("Could not read ${configFile.path}, falling back to defaults: ${e.message}")
            ""
        }

    /**
     * Registers all commands based on enabled features.
     */
    private fun registerCommands() {
        val commandRegistry = CommandRegistry(this)
        val settingHandlerRegistry = SettingHandlerRegistry()

        // DM notification is always available; the other two follow their feature
        val enabledSettings =
            buildList {
                add(SettingKey.Notice)
                if (services.channelManager != null) add(SettingKey.ChNotice)
                if (services.romajiConverter != null) add(SettingKey.Japanese)
            }
        enabledSettings.forEach { key ->
            settingHandlerRegistry.register(
                SettingHandler(key, services.playerSettingsManager, services.languageManager),
            )
        }

        // Register core commands
        commandRegistry.registerAll(
            TellCommand(
                this,
                services.directMessageHandler,
                services.languageManager,
                services.crossServerDirectMessageManager,
                services.remotePlayerRegistry,
                configuration.features.velocityIntegration.serverName,
            ),
            LunaticChatCommand(
                this,
                settingHandlerRegistry,
                services.languageManager,
                configuration,
                configurationReloader,
                debugState,
                DiagnosticsReport(this, configuration, services, debugState),
            ),
        )

        // Conditionally register /reply command if quick replies are enabled
        if (configuration.features.quickReplies.enabled) {
            commandRegistry.registerAll(
                ReplyCommand(
                    this,
                    services.directMessageHandler,
                    services.languageManager,
                    services.crossServerDirectMessageManager,
                ),
            )
        }

        // Conditionally register /lcv command if Velocity integration is enabled
        services.velocityConnectionManager?.let { velocityManager ->
            commandRegistry.registerAll(
                VelocityStatusCommand(this, velocityManager, services.languageManager),
            )
        }

        commandRegistry.initialize()
    }

    /**
     * Registers all event listeners.
     */
    private fun registerEventListeners() {
        EventListenerRegistry.registerAll(this, services, configuration, updateAvailable, debugLogger)
    }

    /**
     * Initializes the update checker.
     * Uses plugin coroutine scope instead of runBlocking for non-blocking async execution.
     */
    private fun initializeUpdateChecker(httpClient: HttpClient) {
        updateChecker =
            UpdateChecker(
                currentVersion = pluginMeta.version,
                logger = logger,
                httpClient = httpClient,
            )
        pluginScope.scope.launch {
            checkUpdates()
        }
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
