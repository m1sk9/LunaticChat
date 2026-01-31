package dev.m1sk9.lunaticChat.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import dev.m1sk9.lunaticChat.velocity.messaging.PluginMessageHandler
import org.slf4j.Logger

/**
 * LunaticChat Velocity plugin
 *
 * Handles plugin messaging in coordination with Paper servers.
 */
@Plugin(
    id = "lunaticchat",
    name = "LunaticChat",
    version = "0.8.0",
    description = "Next-generation channel chat plugin for Paper/Velocity",
    url = "https://lc.m1sk9.dev",
    authors = ["m1sk9"],
)
class LunaticChat
    @Inject
    constructor(
        private val server: ProxyServer,
        private val logger: Logger,
    ) {
        private var messageHandler: PluginMessageHandler? = null

        @Subscribe
        fun onProxyInitialization(event: ProxyInitializeEvent) {
            logger.info("Initializing LunaticChat Velocity plugin")

            // Initialize plugin message handler
            messageHandler =
                PluginMessageHandler(
                    plugin = this@LunaticChat,
                    server = server,
                    logger = logger,
                    pluginVersion = "0.8.0",
                )
            messageHandler?.initialize()

            logger.info("LunaticChat Velocity plugin initialized successfully")
        }

        @Subscribe
        fun onProxyShutdown(event: ProxyShutdownEvent) {
            logger.info("Shutting down LunaticChat Velocity plugin")
            messageHandler?.shutdown()
        }
    }
