package dev.m1sk9.lunaticChat.paper.command.core

import dev.m1sk9.lunaticChat.paper.LunaticChat
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import java.util.logging.Logger

/**
 * Central registry for all LunaticChat commands.
 * Handles registration with Paper's LifecycleEventManager.
 */
class CommandRegistry(
    private val plugin: LunaticChat,
) {
    private val logger: Logger = plugin.logger
    private val commands: MutableList<LunaticCommand> = mutableListOf()

    /**
     * Registers a command to be registered with the server.
     * Must be called before initialize().
     */
    fun register(command: LunaticCommand): CommandRegistry {
        commands.add(command)
        return this
    }

    /**
     * Registers multiple commands at once.
     */
    fun registerAll(vararg commands: LunaticCommand): CommandRegistry {
        commands.forEach { register(it) }
        return this
    }

    /**
     * Initializes command registration with Paper's LifecycleEventManager.
     * This should be called from the plugin's onEnable().
     */
    fun initialize() {
        val lifecycleManager = plugin.lifecycleManager

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val registrar = event.registrar()

            for (command in commands) {
                try {
                    val builtCommand = command.buildWithChecks().build()

                    registrar.register(
                        builtCommand,
                        command.description,
                        command.aliases,
                    )

                    logger.info(
                        "Registered command: /${command.name} " +
                            "(aliases: ${command.aliases.joinToString(", ")})",
                    )
                } catch (e: Exception) {
                    logger.severe("Failed to register command /${command.name}: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
}
