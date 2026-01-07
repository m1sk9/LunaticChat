package dev.m1sk9.lunaticChat.paper.command.annotation

import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import kotlin.reflect.KClass

/**
 * Restricts command execution to senders with the specified permission.
 *
 * @param value The permission node required to execute this command
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Permission(
    val value: KClass<out LunaticChatPermissionNode>,
)
