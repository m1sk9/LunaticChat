package dev.m1sk9.lunaticChat.paper.command.core

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import io.papermc.paper.command.brigadier.CommandSourceStack

/**
 * A command node attached under a parent literal rather than registered with the server.
 *
 * The permission gate is a declared property instead of an annotation read by reflection, so a
 * subcommand that forgets it fails to compile rather than silently accepting everyone.
 */
abstract class LunaticSubCommand(
    plugin: LunaticChat,
) : LunaticCommandBase(plugin) {
    /**
     * The literal this subcommand is typed as. Parents use it to look up help text, so it must be
     * the same string [build] passes to `Commands.literal`.
     */
    abstract val literal: String

    /** Permission required to see and run this subcommand, or null to inherit the parent's gate. */
    protected abstract val permissionNode: LunaticChatPermissionNode?

    /** Extra literals registered next to the primary node, sharing its arguments and executor. */
    protected open val aliases: List<String> = emptyList()

    /**
     * Builds the primary subcommand node, without the permission gate.
     */
    abstract fun build(): LiteralArgumentBuilder<CommandSourceStack>

    /**
     * Builds the primary node and its aliases, each gated on [permissionNode].
     */
    fun buildAll(): List<LiteralArgumentBuilder<CommandSourceStack>> {
        val primary = build()
        permissionNode?.let { node ->
            // Deliberately not instrumented under the `command` debug category. Brigadier evaluates
            // this predicate when it builds a command tree to send to a player - on every join and
            // every updateCommands() - not when someone is refused, so a line here would put one
            // entry per gated node per player in the log and bury the results the category exists
            // to show.
            primary.requires { source -> source.sender.hasPermission(node.permissionNode) }
        }
        return withAliases(primary, aliases)
    }
}
