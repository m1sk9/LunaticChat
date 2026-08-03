package dev.m1sk9.lunaticChat.paper.command.core

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * Functionality shared by every command node, whether it is registered with the server
 * ([LunaticCommand]) or attached under a parent literal ([LunaticSubCommand]).
 */
abstract class LunaticCommandBase(
    // Reference to the main plugin instance
    // DO NOT REMOVE - needed for command registration
    protected val plugin: LunaticChat,
) {
    private val isPlayerOnly: Boolean by lazy {
        this::class.annotations.any { it is PlayerOnly }
    }

    /** Source of localized text for [fail] and [ok]. */
    protected open val languageManager: LanguageManager get() = plugin.languageManager

    /**
     * A failed result carrying the localized message at [key], formatted as an error.
     */
    protected fun fail(
        key: String,
        args: Map<String, String> = emptyMap(),
    ): CommandResult = CommandResult.Failure(languageManager.getMessage(key, args))

    /**
     * A successful result carrying the localized message at [key].
     */
    protected fun ok(
        key: String,
        args: Map<String, String> = emptyMap(),
    ): CommandResult = CommandResult.SuccessWithMessage(languageManager.getMessage(key, args))

    /**
     * Helper method for checking player-only restriction.
     * Called at the beginning of execute methods.
     */
    protected fun checkPlayerOnly(ctx: CommandContext): CommandResult? {
        if (isPlayerOnly && !ctx.isPlayer) {
            return fail("general.playerOnlyCommand")
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
            is CommandResult.SuccessWithMessage -> ctx.reply(MessageFormatter.format(result.message))
            is CommandResult.Failure -> ctx.reply(MessageFormatter.formatError(result.message))
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
     * Creates alias nodes for a subcommand.
     * Each alias gets the same children, executor, and permission requirement as the primary node.
     * Brigadier automatically provides tab completion for all registered literal nodes.
     *
     * @param primary The primary subcommand builder
     * @param aliases The alias names for the subcommand
     * @return A list containing the primary builder followed by alias builders
     */
    protected fun withAliases(
        primary: LiteralArgumentBuilder<CommandSourceStack>,
        aliases: List<String>,
    ): List<LiteralArgumentBuilder<CommandSourceStack>> {
        if (aliases.isEmpty()) return listOf(primary)
        return listOf(primary) +
            aliases.map { alias ->
                val aliasBuilder = Commands.literal(alias)
                primary.arguments.forEach { aliasBuilder.then(it) }
                primary.command?.let { aliasBuilder.executes(it) }
                aliasBuilder.requires(primary.requirement)
                aliasBuilder
            }
    }
}
