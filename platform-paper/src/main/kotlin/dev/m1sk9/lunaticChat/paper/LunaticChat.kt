package dev.m1sk9.lunaticChat.paper

import dev.m1sk9.lunaticChat.paper.command.core.CommandRegistry
import dev.m1sk9.lunaticChat.paper.command.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.command.impl.ReplyCommand
import dev.m1sk9.lunaticChat.paper.command.impl.RomajiConvertToggleCommand
import dev.m1sk9.lunaticChat.paper.command.impl.TellCommand
import dev.m1sk9.lunaticChat.paper.common.SpyPermissionManager
import dev.m1sk9.lunaticChat.paper.config.ConfigManager
import dev.m1sk9.lunaticChat.paper.converter.ConversionCache
import dev.m1sk9.lunaticChat.paper.converter.GoogleIMEClient
import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import dev.m1sk9.lunaticChat.paper.listener.PlayerChatListener
import dev.m1sk9.lunaticChat.paper.listener.PlayerPresenceListener
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import kotlin.time.Duration.Companion.milliseconds

class LunaticChat :
    JavaPlugin(),
    Listener {
    lateinit var directMessageHandler: DirectMessageHandler

    private lateinit var commandRegistry: CommandRegistry
    private var romajiConverter: RomanjiConverter? = null
    private var playerSettingsManager: PlayerSettingsManager? = null

    override fun onEnable() {
        saveDefaultConfig()
        val lunaticChatConfiguration = ConfigManager.loadConfiguration(config)

        if (lunaticChatConfiguration.debug) {
            logger.warning("LunaticChat is running in debug mode.")
            logger.info("Debug: $lunaticChatConfiguration")
        }

        if (lunaticChatConfiguration.features.japaneseConversion.enabled) {
            val settingsDir = dataFolder.resolve(lunaticChatConfiguration.features.japaneseConversion.settingsDirectory).toPath()
            playerSettingsManager =
                PlayerSettingsManager(
                    settingsDirectory = settingsDir,
                    plugin = this,
                    logger = logger,
                )
            playerSettingsManager!!.initializeDirectory()

            val cache =
                ConversionCache(
                    cacheFile = dataFolder.resolve(lunaticChatConfiguration.features.japaneseConversion.cacheFilePath).toPath(),
                    maxEntries = lunaticChatConfiguration.features.japaneseConversion.cacheMaxEntries,
                    plugin = this,
                    logger = logger,
                )
            cache.loadFromDisk()

            val httpClient = HttpClient(CIO)
            val apiClient =
                GoogleIMEClient(
                    timeout = lunaticChatConfiguration.features.japaneseConversion.apiTimeout.milliseconds,
                    httpClient = httpClient,
                )
            romajiConverter = RomanjiConverter(
                cache = cache,
                apiClient = apiClient,
                logger = logger,
                debugMode = lunaticChatConfiguration.debug
            )

            val saveInterval = lunaticChatConfiguration.features.japaneseConversion.cacheSaveIntervalSeconds * 20L
            server.scheduler.runTaskTimerAsynchronously(
                this,
                Runnable {
                    cache.saveToDisk()
                },
                saveInterval,
                saveInterval,
            )

            server.pluginManager.registerEvents(PlayerChatListener(romajiConverter!!, playerSettingsManager!!), this)
            logger.info("Japanese conversion feature enabled.")
        }

        directMessageHandler =
            DirectMessageHandler(
                settingsManager = playerSettingsManager,
                romanjiConverter = romajiConverter,
            )

        commandRegistry =
            CommandRegistry(this)
                .registerAll(
                    TellCommand(this, directMessageHandler),
                )
        if (lunaticChatConfiguration.features.quickRepliesEnabled.enabled) {
            commandRegistry.registerAll(
                ReplyCommand(this, directMessageHandler),
            )
        }
        if (lunaticChatConfiguration.features.japaneseConversion.enabled) {
            commandRegistry.registerAll(
                RomajiConvertToggleCommand(this, playerSettingsManager!!),
            )
        }
        commandRegistry.initialize()

        server.pluginManager.registerEvents(SpyPermissionManager, this)
        server.pluginManager.registerEvents(PlayerPresenceListener(this, playerSettingsManager), this)

        logger.info("LunaticChat enabled.")
    }

    override fun onDisable() {
        logger.info("LunaticChat disabled.")
    }
}
