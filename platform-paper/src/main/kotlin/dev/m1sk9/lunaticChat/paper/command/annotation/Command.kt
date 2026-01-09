package dev.m1sk9.lunaticChat.paper.command.annotation

/**
 * Marks a class as a LunaticChat command.
 *
 * @param name The primary command name (without leading slash)
 * @param aliases List of alternative command names
 * @param description Human-readable description for help text
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Command(
    val name: String,
    val aliases: Array<String> = [],
    val description: String = "",
)
