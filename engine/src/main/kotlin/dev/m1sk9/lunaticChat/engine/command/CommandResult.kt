package dev.m1sk9.lunaticChat.engine.command

/**
 * Represents the result of a command execution.
 * Uses Kotlin sealed classes for type-safe result handling.
 *
 * Messages travel as plain text; turning them into styled output is the platform's job, so this
 * stays free of any Minecraft or Adventure type.
 */
sealed class CommandResult {
    /** Command executed successfully */
    data object Success : CommandResult()

    /** Command executed successfully with a message to display */
    data class SuccessWithMessage(
        val message: String,
    ) : CommandResult()

    /** Command failed with an error message */
    data class Failure(
        val message: String,
    ) : CommandResult()

    /** Command failed due to invalid usage */
    data class InvalidUsage(
        val usageHint: String,
    ) : CommandResult()

    /**
     * Converts result to Brigadier return value.
     * @return 1 for success, 0 for failure (Brigadier convention)
     */
    fun toBrigadierResult(): Int =
        when (this) {
            is Success, is SuccessWithMessage -> 1
            is Failure, is InvalidUsage -> 0
        }
}
