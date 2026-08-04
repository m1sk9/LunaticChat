package dev.m1sk9.lunaticChat.paper

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
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
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
    // Read by commands, which reach the plugin instance but not the container.
    val directMessageHandler: DirectMessageHandler get() = services.directMessageHandler
    val languageManager: LanguageManager get() = services.languageManager
    val channelManager: ChannelManager? get() = services.channelManager
    val channelMembershipManager: ChannelMembershipManager? get() = services.channelMembershipManager
    val channelNotificationHandler: ChannelNotificationHandler? get() = services.channelNotificationHandler
    val velocityConnectionManager: VelocityConnectionManager? get() = services.velocityConnectionManager

    private lateinit var services: ServiceContainer
    private lateinit var configuration: LunaticChatConfiguration
    private lateinit var serviceInitializer: ServiceInitializer

    // Read by commands that must not block the tick thread.
    lateinit var pluginScope: PluginCoroutineScope
        private set

    /** Serializes each player's outgoing messages so they arrive in the order they were sent. */
    lateinit var deliveryQueue: PerPlayerWorkQueue
        private set
    private var updateChecker: UpdateChecker? = null

    private val updateAvailable = AtomicBoolean(false)

    // Only the Japanese conversion and update-check features make HTTP calls, and both default
    // to off, so a stock install should not pay for a CIO engine and its thread pool.
    private val httpClient = lazy { HttpClient(CIO) }

    override fun onEnable() {
        saveDefaultConfig()
        configuration = ConfigManager(logger).loadConfiguration(readConfigFile())

        if (configuration.debug) {
            logger.warning("LunaticChat is running in debug mode.")
            logger.info("Debug: $configuration")
        }

        // Initialize plugin coroutine scope
        pluginScope = PluginCoroutineScope(logger)
        deliveryQueue = PerPlayerWorkQueue(pluginScope.scope, logger)

        // Initialize all services
        serviceInitializer =
            ServiceInitializer(
                plugin = this,
                configuration = configuration,
                httpClient = httpClient,
                logger = logger,
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
     * Reads config.yml, or an empty document when it cannot be read.
     *
     * [saveDefaultConfig] only logs when it fails to write the file, so the read can still find
     * nothing there. Handing the parser an empty document starts the plugin on its defaults
     * instead of throwing out of [onEnable] and disabling it outright.
     */
    private fun readConfigFile(): String {
        val file = dataFolder.resolve("config.yml")
        return runCatching { file.readText() }.getOrElse { e ->
            logger.severe("Could not read ${file.path}, falling back to defaults: ${e.message}")
            ""
        }
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
            LunaticChatCommand(this, settingHandlerRegistry, services.languageManager, configuration),
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
        EventListenerRegistry.registerAll(this, services, configuration, updateAvailable)
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
