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
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import java.nio.file.Path

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
                handleResult(context, execute(context))
            }
            Command.SINGLE_SUCCESS
        }

    internal fun execute(ctx: CommandContext): CommandResult =
        runCatching { report.write() }
            .fold(
                onSuccess = { file ->
                    plugin.logger.info("Wrote a diagnostics report to ${file.toAbsolutePath()}")
                    ctx.reply(pathLine(displayPath(file)))
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

    /**
     * Where the report landed, as an operator would type it: relative to the server directory.
     *
     * The absolute path is written to the server log and goes no further. `/lc dump` is reachable by
     * anyone a permission plugin grants it to, who need not have filesystem access at all, and to
     * them the full path is not something they can act on - it only says where the server is
     * installed. Falls back to the bare file name when the report is not under the server directory,
     * which a relative path could only express as a chain of `..` segments.
     */
    private fun displayPath(file: Path): String {
        val serverDirectory = Path.of("").toAbsolutePath()
        val relative = runCatching { serverDirectory.relativize(file.toAbsolutePath()) }.getOrNull()
        return if (relative == null || relative.startsWith("..")) file.fileName.toString() else relative.toString()
    }

    /**
     * The report's path, click-to-copy.
     *
     * Built as a component rather than a placeholder in the success message: a path can hold braces
     * of its own, which the next placeholder substitution would mangle.
     */
    private fun pathLine(path: String): Component =
        Component
            .text("  • ", NamedTextColor.GRAY)
            .append(Component.text(path, NamedTextColor.AQUA))
            .hoverEvent(
                HoverEvent.showText(
                    Component.text(languageManager.getMessage("dump.clickToCopy"), NamedTextColor.GRAY),
                ),
            ).clickEvent(ClickEvent.copyToClipboard(path))
}
