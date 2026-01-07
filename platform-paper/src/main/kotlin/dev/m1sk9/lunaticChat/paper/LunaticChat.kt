package dev.m1sk9.lunaticChat.paper

import dev.m1sk9.lunaticChat.paper.command.core.CommandRegistry
import dev.m1sk9.lunaticChat.paper.command.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.command.impl.ReplyCommand
import dev.m1sk9.lunaticChat.paper.command.impl.TellCommand
import dev.m1sk9.lunaticChat.paper.config.ConfigManager
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class LunaticChat :
    JavaPlugin(),
    Listener {
    lateinit var directMessageHandler: DirectMessageHandler

    private lateinit var commandRegistry: CommandRegistry

    override fun onEnable() {
        saveDefaultConfig()
        val lunaticChatConfiguration = ConfigManager.loadConfiguration(config)

        if (lunaticChatConfiguration.debug) {
            logger.warning("LunaticChat is running in debug mode.")
            logger.info("Debug: $lunaticChatConfiguration")
        }

        directMessageHandler = DirectMessageHandler()

        commandRegistry =
            CommandRegistry(this)
                .registerAll(
                    TellCommand(this, directMessageHandler),
                    ReplyCommand(this, directMessageHandler),
                )
        commandRegistry.initialize()

        server.pluginManager.registerEvents(this, this)

        logger.info("LunaticChat enabled.")
    }

    override fun onDisable() {
        logger.info("LunaticChat disabled.")
    }
}
