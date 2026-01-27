package dev.m1sk9.lunaticChat.paper.listener

import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.ServiceContainer
import dev.m1sk9.lunaticChat.paper.common.SpyPermissionManager
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
     * @param updateAvailable Atomic flag for update availability
     */
    fun registerAll(
        plugin: LunaticChat,
        services: ServiceContainer,
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
                chatModeManager = services.chatModeManager,
                channelManager = services.channelManager,
            ),
            plugin,
        )

        // Conditionally register chat listener when all required components are available
        if (services.chatModeManager != null &&
            services.channelManager != null &&
            services.channelMessageHandler != null &&
            services.romajiConverter != null
        ) {
            pluginManager.registerEvents(
                PlayerChatListener(
                    chatModeManager = services.chatModeManager,
                    channelManager = services.channelManager,
                    channelMessageHandler = services.channelMessageHandler,
                    romajiConverter = services.romajiConverter,
                    settingsManager = services.playerSettingsManager,
                    languageManager = services.languageManager,
                ),
                plugin,
            )
        }
    }
}
