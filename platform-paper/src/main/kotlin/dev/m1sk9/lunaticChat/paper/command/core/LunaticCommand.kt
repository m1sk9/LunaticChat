package dev.m1sk9.lunaticChat.paper.command.core

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.annotation.Command
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * Abstract base class for all LunaticChat commands.
 * Provides common functionality and enforces consistent command structure.
 */
abstract class LunaticCommand(
    // Reference to the main plugin instance
    // DO NOT REMOVE - needed for command registration
    protected val plugin: LunaticChat,
) {
    private val commandAnnotation: Command by lazy {
        this::class.annotations.filterIsInstance<Command>().firstOrNull()
            ?: throw IllegalStateException("Command class must be annotated with @Command")
    }

    private val permissionAnnotation: Permission? by lazy {
        this::class.annotations.filterIsInstance<Permission>().firstOrNull()
    }

    private val isPlayerOnly: Boolean by lazy {
        this::class.annotations.any { it is PlayerOnly }
    }

    /** The primary command name */
    val name: String get() = commandAnnotation.name

    /** Command aliases */
    val aliases: List<String> get() = commandAnnotation.aliases.toList()

    /** Command description for help text */
    val description: String get() = commandAnnotation.description

    /** Required permission node, if any */
    val permission: String? get() = permissionAnnotation?.value?.objectInstance?.permissionNode

    /**
     * Build the Brigadier command tree.
     * Subclasses implement this to define arguments and execution logic.
     *
     * @return The command builder with all arguments and executors attached
     */
    abstract fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack>

    /**
     * Wraps the command builder with permission checks.
     * Called by CommandRegistry during registration.
     */
    fun buildWithChecks(): LiteralArgumentBuilder<CommandSourceStack> {
        var builder = buildCommand()
        permission?.let { perm ->
            builder =
                builder.requires { source ->
                    source.sender.hasPermission(perm)
                }
        }

        return builder
    }

    /**
     * Helper method for checking player-only restriction.
     * Called at the beginning of execute methods.
     */
    protected fun checkPlayerOnly(ctx: CommandContext): CommandResult? {
        if (isPlayerOnly && !ctx.isPlayer) {
            return CommandResult.Failure(
                Component
                    .text("This command can only be executed by a player.")
                    .color(NamedTextColor.RED),
            )
        }

        return null
    }

    /**
     * Utility to wrap Brigadier context into LunaticChat CommandContext.
     */
    protected fun wrapContext(ctx: com.mojang.brigadier.context.CommandContext<CommandSourceStack>): CommandContext =
        CommandContext(ctx.source)

    /**
     * Helper for handling command results and sending appropriate messages.
     */
    protected fun handleResult(
        ctx: CommandContext,
        result: CommandResult,
    ): Int {
        when (result) {
            is CommandResult.Success -> {}
            is CommandResult.SuccessWithMessage -> ctx.reply(result.message)
            is CommandResult.Failure -> ctx.reply(result.message)
            is CommandResult.InvalidUsage ->
                ctx.reply(
                    Component
                        .text("Usage: ${result.usageHint}")
                        .color(NamedTextColor.RED),
                )
        }
        return result.toBrigadierResult()
    }
}
