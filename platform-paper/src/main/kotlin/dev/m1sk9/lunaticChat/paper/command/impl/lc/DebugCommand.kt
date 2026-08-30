package dev.m1sk9.lunaticChat.paper.command.impl.lc

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.debug.DebugCategories
import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.engine.debug.DebugState
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.core.LunaticSubCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

/**
 * Switches debug categories on and off while the server runs.
 *
 * The change is deliberately volatile: config.yml is never rewritten, so a restart or `/lc reload`
 * puts the operator's file back in charge. Writing it back would mean re-emitting a commented YAML
 * document from a parsed tree, which loses every comment and every bit of formatting the operator
 * put there - a high price for saving them one edit.
 */
class DebugCommand(
    plugin: LunaticChat,
    override val languageManager: LanguageManager,
    private val debugState: DebugState,
) : LunaticSubCommand(plugin) {
    override val literal = "debug"
    override val permissionNode = LunaticChatPermissionNode.Debug

    private companion object {
        /**
         * The word that stands for every category at once, in both the argument and the replies.
         *
         * Taken from [DebugCategories] rather than spelled again here, so the command and the two
         * config.yml spellings cannot drift apart on what "everything" is called.
         */
        const val ALL = DebugCategories.ALL
    }

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal(literal)
            .then(
                Commands
                    .argument("category", StringArgumentType.word())
                    .suggests { _, builder ->
                        val typed = builder.remaining.lowercase()
                        (listOf(ALL) + DebugCategory.entries.map { it.key })
                            .filter { it.startsWith(typed) }
                            .forEach(builder::suggest)
                        builder.buildFuture()
                    }.then(stateNode("on", on = true))
                    .then(stateNode("off", on = false)),
            ).executes { ctx ->
                val context = wrapContext(ctx)
                handleResult(context, list())
            }

    private fun stateNode(
        state: String,
        on: Boolean,
    ): LiteralArgumentBuilder<CommandSourceStack> =
        Commands.literal(state).executes { ctx ->
            val context = wrapContext(ctx)
            handleResult(context, toggle(StringArgumentType.getString(ctx, "category"), on))
        }

    /** The categories currently logging. */
    internal fun list(): CommandResult =
        ok("debug.list", mapOf("categories" to debugState.enabled.joinToString(", ") { it.key }.ifEmpty { noneLabel() }))

    /** Switches [categoryName], or every category when it is `all`. */
    internal fun toggle(
        categoryName: String,
        on: Boolean,
    ): CommandResult {
        if (categoryName.equals(ALL, ignoreCase = true)) {
            debugState.replace(if (on) DebugCategory.entries.toSet() else emptySet())
            return ok(resultKey(on), mapOf("category" to ALL))
        }

        val category =
            DebugCategory.fromKey(categoryName)
                ?: return fail(
                    "debug.unknownCategory",
                    mapOf("category" to categoryName, "categories" to DebugCategory.keyList),
                )

        debugState.set(category, on)
        return ok(resultKey(on), mapOf("category" to category.key))
    }

    private fun resultKey(on: Boolean) = if (on) "debug.enabled" else "debug.disabled"

    private fun noneLabel() = languageManager.getMessage("debug.none")
}
