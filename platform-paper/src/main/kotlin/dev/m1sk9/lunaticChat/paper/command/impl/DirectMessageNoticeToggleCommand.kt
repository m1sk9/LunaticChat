package dev.m1sk9.lunaticChat.paper.command.impl

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.annotation.Command
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Command(
    name = "notice",
    aliases = ["dmnotice"],
    description = "Toggle direct message notification",
)
@Permission(LunaticChatPermissionNode.NoticeToggle::class)
@PlayerOnly
class DirectMessageNoticeToggleCommand(
    plugin: LunaticChat,
    private val settingsManager: PlayerSettingsManager,
) : LunaticCommand(plugin) {
    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal(name)
            .then(
                Commands
                    .literal("on")
                    .executes { ctx ->
                        val context = wrapContext(ctx)
                        checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }
                        val result = execute(context, true)
                        handleResult(context, result)
                    },
            ).then(
                Commands
                    .literal("off")
                    .executes { ctx ->
                        val context = wrapContext(ctx)
                        checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }
                        val result = execute(context, false)
                        handleResult(context, result)
                    },
            ).executes { ctx ->
                val context = wrapContext(ctx)
                checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }
                val result = showStatus(context)
                handleResult(context, result)
            }

    private fun execute(
        ctx: CommandContext,
        enable: Boolean,
    ): CommandResult {
        val player = ctx.requirePlayer()
        val currentSettings = settingsManager.getSettings(player.uniqueId)
        val updatedSettings = currentSettings.copy(directMessageNotificationEnabled = enable)
        settingsManager.updateSettings(updatedSettings)

        val statusText = if (enable) "enabled" else "disabled"
        val message =
            Component
                .text("Direct message notification has been $statusText.")
                .color(NamedTextColor.GREEN)

        player.sendMessage(message)
        return CommandResult.Success
    }

    private fun showStatus(ctx: CommandContext): CommandResult {
        val player = ctx.requirePlayer()
        val settings = settingsManager.getSettings(player.uniqueId)

        val statusText = if (settings.directMessageNotificationEnabled) "enabled" else "disabled"
        val message =
            Component
                .text("Direct message notification is currently $statusText.")
                .color(NamedTextColor.YELLOW)

        player.sendMessage(message)
        return CommandResult.Success
    }
}
