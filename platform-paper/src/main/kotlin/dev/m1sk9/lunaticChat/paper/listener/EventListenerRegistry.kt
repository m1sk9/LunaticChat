package dev.m1sk9.lunaticChat.paper.listener

import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.ServiceContainer
import dev.m1sk9.lunaticChat.paper.common.SpyPermissionManager
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Centralizes event listener registration.
 *
 * This class handles conditional registration of listeners based on
 * available services and enabled features.
 */
object EventListenerRegistry {
    /**
     * Registers all event listeners based on available services.
     *
     * @param plugin The plugin instance
     * @param services The initialized services
     * @param configuration The plugin configuration
     * @param updateAvailable Atomic flag for update availability
     */
    fun registerAll(
        plugin: LunaticChat,
        services: ServiceContainer,
        configuration: LunaticChatConfiguration,
        updateAvailable: AtomicBoolean,
    ) {
        val pluginManager = plugin.server.pluginManager

        // Always register these listeners
        pluginManager.registerEvents(SpyPermissionManager, plugin)
        pluginManager.registerEvents(
            PlayerPresenceListener(
                lunaticChat = plugin,
                languageManager = services.languageManager,
                updateCheckerFlag = updateAvailable,
                playerSettingsManager = services.playerSettingsManager,
                channelManager = services.channelManager,
            ),
            plugin,
        )

        // Register chat listener when channel chat, velocity cross-server chat, or Japanese conversion is enabled
        val shouldRegisterChatListener =
            (
                services.channelManager != null &&
                    services.channelMessageHandler != null
            ) ||
                (
                    configuration.features.velocityIntegration.enabled &&
                        configuration.features.velocityIntegration.crossServerGlobalChat
                ) ||
                services.romajiConverter != null

        if (shouldRegisterChatListener) {
            pluginManager.registerEvents(
                PlayerChatListener(
                    channelManager = services.channelManager,
                    channelMessageHandler = services.channelMessageHandler,
                    romajiConverter = services.romajiConverter,
                    settingsManager = services.playerSettingsManager,
                    configuration = configuration,
                    crossServerChatManager = services.crossServerChatManager,
                ),
                plugin,
            )
        }
    }
}
