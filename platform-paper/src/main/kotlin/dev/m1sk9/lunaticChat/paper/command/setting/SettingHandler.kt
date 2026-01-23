package dev.m1sk9.lunaticChat.paper.command.setting

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext

/**
 * Interface for handling individual setting operations.
 * Each setting (japanese, notice, etc.) implements this interface to provide
 * its own logic for getting, setting, and displaying status.
 */
interface SettingHandler {
    /**
     * The setting key this handler manages.
     */
    val key: SettingKey

    /**
     * Enables or disables the setting for a player.
     *
     * @param ctx The command context containing player information
     * @param enable True to enable, false to disable
     * @return Command result indicating success or failure
     */
    fun execute(
        ctx: CommandContext,
        enable: Boolean,
    ): CommandResult

    /**
     * Shows the current status of the setting for a player.
     *
     * @param ctx The command context containing player information
     * @return Command result indicating success or failure
     */
    fun showStatus(ctx: CommandContext): CommandResult
}
