package dev.m1sk9.lunaticChat.paper.command.annotation

/**
 * Restricts command execution to players only.
 * Console execution will be rejected with an appropriate message.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PlayerOnly
