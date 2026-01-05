package dev.m1sk9.lunaticChat.paper

import dev.m1sk9.lunaticChat.paper.config.ConfigManager
import org.bukkit.plugin.java.JavaPlugin

class LunaticChat : JavaPlugin() {
    override fun onEnable() {
        saveDefaultConfig()
        val lunaticChatConfiguration = ConfigManager.loadConfiguration(config)

        if (lunaticChatConfiguration.debug) {
            logger.warning("LunaticChat is running in debug mode.")
            logger.info("Debug: $lunaticChatConfiguration")
            // TODO: Enable debug features
        }

        logger.info("LunaticChat enabled.")
    }

    override fun onDisable() {
        logger.info("LunaticChat disabled.")
    }
}
