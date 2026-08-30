package dev.m1sk9.lunaticChat.paper.command.impl.lc

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticSubCommand
import dev.m1sk9.lunaticChat.paper.debug.DiagnosticsReport
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

/**
 * Writes a diagnostics report and tells the sender where it landed.
 *
 * Like `/lc reload` this is an operator's command and is left reachable from the console, so it
 * never calls [CommandContext.requirePlayer].
 */
class DumpCommand(
    plugin: LunaticChat,
    override val languageManager: LanguageManager,
    private val report: DiagnosticsReport,
) : LunaticSubCommand(plugin) {
    override val literal = "dump"
    override val permissionNode = LunaticChatPermissionNode.Dump

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands.literal(literal).executes { ctx ->
            val context = wrapContext(ctx)
            // The report reads every store and writes a file under plugins/, which is a network
            // mount often enough that it cannot happen on the tick thread.
            plugin.server.asyncScheduler.runNow(plugin) {
                handleResult(context, execute())
            }
            Command.SINGLE_SUCCESS
        }

    internal fun execute(): CommandResult =
        runCatching { report.write() }
            .fold(
                onSuccess = { file ->
                    // Where it landed goes to the server log and no further. /lc dump can be
                    // granted to someone with no filesystem access, and to them a path is not
                    // something they can act on - it only says where the server is installed.
                    plugin.logger.info("Wrote a diagnostics report to ${file.toAbsolutePath()}")
                    ok("dump.success")
                },
                onFailure = { failure ->
                    // The reason goes to the server log rather than into the chat message: it is
                    // an IO error phrased by the JDK, which cannot be translated and may carry
                    // braces of its own for the next placeholder substitution to mangle.
                    plugin.logger.warning("/lc dump could not write the report: ${failure.message}")
                    fail("dump.failed")
                },
            )
}
