package dev.m1sk9.lunaticChat.paper

import dev.m1sk9.lunaticChat.paper.command.core.CommandRegistry
import dev.m1sk9.lunaticChat.paper.command.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.command.impl.ReplyCommand
import dev.m1sk9.lunaticChat.paper.command.impl.TellCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.LunaticChatCommand
import dev.m1sk9.lunaticChat.paper.command.setting.SettingHandlerRegistry
import dev.m1sk9.lunaticChat.paper.command.setting.handler.DirectMessageNoticeSettingHandler
import dev.m1sk9.lunaticChat.paper.command.setting.handler.JapaneseConversionSettingHandler
import dev.m1sk9.lunaticChat.paper.common.UpdateCheckResult
import dev.m1sk9.lunaticChat.paper.common.UpdateChecker
import dev.m1sk9.lunaticChat.paper.config.ConfigManager
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.listener.EventListenerRegistry
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.atomic.AtomicBoolean

class LunaticChat :
    JavaPlugin(),
    Listener {
    // Public API - accessed by commands (maintain backward compatibility)
    lateinit var directMessageHandler: DirectMessageHandler
    lateinit var languageManager: LanguageManager

    // Private services
    private lateinit var services: ServiceContainer
    private lateinit var configuration: LunaticChatConfiguration
    private lateinit var serviceInitializer: ServiceInitializer
    private var updateChecker: UpdateChecker? = null

    private val updateAvailable = AtomicBoolean(false)

    override fun onEnable() {
        saveDefaultConfig()
        configuration = ConfigManager.loadConfiguration(config)

        if (configuration.debug) {
            logger.warning("LunaticChat is running in debug mode.")
            logger.info("Debug: $configuration")
        }

        val httpClient = HttpClient(CIO)

        // Initialize all services
        serviceInitializer =
            ServiceInitializer(
                plugin = this,
                configuration = configuration,
                httpClient = httpClient,
                logger = logger,
            )
        services = serviceInitializer.initialize()

        // Set public API properties (for command access)
        directMessageHandler = services.directMessageHandler
        languageManager = services.languageManager

        // Schedule periodic tasks
        serviceInitializer.schedulePeriodicTasks()

        // Register commands and listeners
        registerCommands()
        registerEventListeners()

        // Check for updates
        if (configuration.checkForUpdates) {
            initializeUpdateChecker(httpClient)
        }

        logger.info("LunaticChat enabled.")
    }

    override fun onDisable() {
        serviceInitializer.shutdown(services)
        logger.info("LunaticChat disabled.")
    }

    /**
     * Registers all commands based on enabled features.
     */
    private fun registerCommands() {
        val commandRegistry = CommandRegistry(this)
        val settingHandlerRegistry = SettingHandlerRegistry()

        // Always register DM notification setting
        settingHandlerRegistry.register(
            DirectMessageNoticeSettingHandler(
                services.playerSettingsManager,
                services.languageManager,
            ),
        )

        // Conditionally register Japanese conversion setting
        if (services.romajiConverter != null) {
            settingHandlerRegistry.register(
                JapaneseConversionSettingHandler(
                    services.playerSettingsManager,
                    services.languageManager,
                ),
            )
        }

        // Register core commands
        commandRegistry.registerAll(
            TellCommand(this, services.directMessageHandler, services.languageManager),
            LunaticChatCommand(this, settingHandlerRegistry, services.languageManager),
        )

        // Conditionally register /reply command if quick replies are enabled
        if (configuration.features.quickReplies.enabled) {
            commandRegistry.registerAll(
                ReplyCommand(this, services.directMessageHandler, services.languageManager),
            )
        }

        commandRegistry.initialize()
    }

    /**
     * Registers all event listeners.
     */
    private fun registerEventListeners() {
        EventListenerRegistry.registerAll(this, services, updateAvailable)
    }

    /**
     * Initializes the update checker.
     */
    private fun initializeUpdateChecker(httpClient: HttpClient) {
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
