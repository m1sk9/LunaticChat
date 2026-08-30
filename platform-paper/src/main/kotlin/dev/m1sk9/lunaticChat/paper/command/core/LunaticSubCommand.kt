package dev.m1sk9.lunaticChat.paper.command.core

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
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
            primary.requires { source ->
                val allowed = source.sender.hasPermission(node.permissionNode)
                // Reported here rather than at the executor: a denied node is hidden from Brigadier
                // entirely, so the sender is told the command does not exist and the server log
                // otherwise says nothing at all about the permission that hid it.
                if (!allowed) {
                    plugin.debug.log(DebugCategory.COMMAND) {
                        "Hid /$literal from ${source.sender.name}: missing ${node.permissionNode}"
                    }
                }
                allowed
            }
        }
        return withAliases(primary, aliases)
    }
}
