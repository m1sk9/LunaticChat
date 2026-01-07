package dev.m1sk9.lunaticChat.paper.command.core

import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Wrapper for Brigadier command context providing type-safe access
 * to command sender and arguments.
 */
class CommandContext(
    private val sourceStack: CommandSourceStack,
) {
    /** The raw command sender (player, console, or other) */
    val sender: CommandSender
        get() = sourceStack.sender

    /** The player if sender is a player, null otherwise */
    val player: Player?
        get() = sender as? Player

    /** Whether the sender is a player */
    val isPlayer: Boolean
        get() = sender is Player

    /**
     * Requires the sender to be a player.
     *
     * @return The player
     * @throws IllegalStateException if sender is not a player
     */
    fun requirePlayer(): Player = player ?: throw IllegalStateException("This command can only be executed by a player")

    /**
     * Sends a message to the command sender.
     *
     * @param message The message component to send
     */
    fun reply(message: Component) {
        sender.sendMessage(message)
    }

    /**
     * Sends a plain text message to the command sender.
     *
     * @param message The plain text message to send
     */
    fun replyPlain(message: String) {
        sender.sendPlainMessage(message)
    }
}
