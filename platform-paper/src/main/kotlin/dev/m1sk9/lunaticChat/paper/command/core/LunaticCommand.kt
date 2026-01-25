package dev.m1sk9.lunaticChat.paper.command.core

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.annotation.Command
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions

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

    private val deprecatedAnnotation: Deprecated? by lazy {
        this::class.annotations.filterIsInstance<Deprecated>().firstOrNull()
    }

    private val isPlayerOnly: Boolean by lazy {
        this::class.annotations.any { it is PlayerOnly }
    }

    /** The primary command name */
    val name: String get() = commandAnnotation.name

    /** Command aliases */
    val aliases: List<String> get() = commandAnnotation.aliases.toList()

    /** Command description for help text - can be overridden for i18n */
    open val description: String get() = commandAnnotation.description

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
        // If command is deprecated, replace with error message handler
        deprecatedAnnotation?.let { deprecated ->
            return Commands
                .literal(name)
                .executes { ctx ->
                    val context = wrapContext(ctx)
                    val result =
                        CommandResult.Failure(
                            MessageFormatter.formatError(deprecated.message),
                        )
                    handleResult(context, result)
                }
        }

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
                MessageFormatter.formatError(
                    plugin.languageManager.getMessage("general.playerOnlyCommand"),
                ),
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

    /**
     * Applies permission checks to a subcommand builder based on method-level @Permission annotation.
     * Used for subcommands that use build() instead of buildCommand().
     *
     * @param methodName The name of the method to check for @Permission annotation
     * @param builder The subcommand builder to wrap
     * @return The builder with permission checks applied if annotation is present
     */
    protected fun applyMethodPermission(
        methodName: String,
        builder: LiteralArgumentBuilder<CommandSourceStack>,
    ): LiteralArgumentBuilder<CommandSourceStack> {
        val method = this::class.memberFunctions.find { it.name == methodName } ?: return builder
        val permissionAnnotation = method.findAnnotation<Permission>() ?: return builder
        val permissionNode = permissionAnnotation.value.objectInstance?.permissionNode ?: return builder

        return builder.requires { source ->
            source.sender.hasPermission(permissionNode)
        }
    }
}
