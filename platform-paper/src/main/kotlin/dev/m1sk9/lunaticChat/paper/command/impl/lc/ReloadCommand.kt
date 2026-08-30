package dev.m1sk9.lunaticChat.paper.command.impl.lc

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticSubCommand
import dev.m1sk9.lunaticChat.paper.config.ConfigurationReloader
import dev.m1sk9.lunaticChat.paper.config.ReloadResult
import dev.m1sk9.lunaticChat.paper.config.SettingFallback
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Re-reads config.yml and applies the message formats.
 *
 * Not annotated `@PlayerOnly`, and it never calls [CommandContext.requirePlayer]: reloading is an
 * operator's job, and the console and RCON are where operators do it. The parent `/lc` carries a
 * `@PlayerOnly` that nothing enforces - the parent literal has no executor of its own - so this
 * node is reachable from the console today. Should that annotation ever be made to bite, it has to
 * gate the parent's own executor rather than its children, or this command goes with it.
 */
class ReloadCommand(
    plugin: LunaticChat,
    override val languageManager: LanguageManager,
    private val reloader: ConfigurationReloader,
) : LunaticSubCommand(plugin) {
    override val literal = "reload"
    override val permissionNode = LunaticChatPermissionNode.Reload

    /**
     * Guards against two reloads overlapping.
     *
     * The holder swap itself is a single volatile write and cannot tear; what this prevents is two
     * readers of the same file racing to decide which one wins.
     */
    private val inProgress = AtomicBoolean(false)

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands.literal(literal).executes { ctx ->
            val context = wrapContext(ctx)
            // config.yml lives under plugins/, which is a network mount often enough that even a
            // read this small cannot be allowed to happen on the tick thread.
            plugin.server.asyncScheduler.runNow(plugin) {
                handleResult(context, execute(context))
            }
            Command.SINGLE_SUCCESS
        }

    internal fun execute(ctx: CommandContext): CommandResult {
        if (!inProgress.compareAndSet(false, true)) {
            return fail("reload.inProgress")
        }
        return try {
            report(ctx, reloader.reload())
        } finally {
            inProgress.set(false)
        }
    }

    private fun report(
        ctx: CommandContext,
        result: ReloadResult,
    ): CommandResult =
        when (result) {
            // Which settings moved is left to the server log. The sender only needs to know whether
            // what they edited is in effect, and a list of dotted keys buried the one bit that
            // changes what they do next: whether they still have to restart.
            //
            // Restart-required wins even when nothing was applied, because the operator did change
            // something - calling that "no change" would send them looking for a typo they had not
            // made.
            is ReloadResult.Applied ->
                when {
                    result.restartRequired.isNotEmpty() -> ok("reload.restartRequired")
                    result.applied.isNotEmpty() -> ok("reload.success")
                    else -> ok("reload.noChanges")
                }

            is ReloadResult.InvalidSettings -> {
                ctx.reply(MessageFormatter.formatError(languageManager.getMessage("reload.invalidSettings")))
                result.fallbacks.forEach { ctx.reply(fallbackLine(it)) }
                fail("reload.previousKept")
            }

            is ReloadResult.InvalidDocument -> {
                ctx.reply(MessageFormatter.formatError(languageManager.getMessage("reload.invalidDocument")))
                ctx.reply(reasonLine(result.reason))
                fail("reload.previousKept")
            }

            is ReloadResult.Unreadable -> {
                ctx.reply(MessageFormatter.formatError(languageManager.getMessage("reload.unreadable")))
                ctx.reply(reasonLine(result.reason))
                fail("reload.previousKept")
            }
        }

    /**
     * The reason a setting was rejected, as kaml phrased it.
     *
     * Built as a component rather than passed to `getMessage` as a placeholder: the message lookup
     * substitutes placeholders one after another, so a reason carrying its own braces would be
     * mangled by the next substitution, and MessageFormatter would then colour those braces as if
     * they were placeholders of ours.
     */
    private fun fallbackLine(fallback: SettingFallback): Component =
        Component
            .text("  • ", NamedTextColor.GRAY)
            .append(Component.text("${fallback.settingKey}: ", NamedTextColor.YELLOW))
            .append(Component.text(fallback.reason, NamedTextColor.GRAY))

    private fun reasonLine(reason: String): Component =
        Component
            .text("  • ", NamedTextColor.GRAY)
            .append(Component.text(reason, NamedTextColor.GRAY))
}
