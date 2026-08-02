package dev.m1sk9.lunaticChat.paper.command.core

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.annotation.Command
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import io.papermc.paper.command.brigadier.CommandSourceStack

/**
 * A command registered with the server under its own name.
 *
 * Subcommands attached under a parent literal extend [LunaticSubCommand] instead: they have no
 * `@Command` annotation, so [name], [aliases] and [description] would have nothing to read.
 */
abstract class LunaticCommand(
    plugin: LunaticChat,
) : LunaticCommandBase(plugin) {
    private val commandAnnotation: Command by lazy {
        this::class.annotations.filterIsInstance<Command>().firstOrNull()
            ?: throw IllegalStateException("Command class must be annotated with @Command")
    }

    private val permissionAnnotation: Permission? by lazy {
        this::class.annotations.filterIsInstance<Permission>().firstOrNull()
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
        var builder = buildCommand()
        permission?.let { perm ->
            builder =
                builder.requires { source ->
                    source.sender.hasPermission(perm)
                }
        }

        return builder
    }
}
