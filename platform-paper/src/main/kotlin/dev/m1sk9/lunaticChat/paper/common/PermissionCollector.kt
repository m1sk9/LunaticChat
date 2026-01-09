package dev.m1sk9.lunaticChat.paper.common

import dev.m1sk9.lunaticChat.engine.exception.RequirePermissionException
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import org.bukkit.entity.Player

@DslMarker
annotation class PermissionDsl

@PermissionDsl
class PermissionCollector {
    internal val permissions = mutableListOf<LunaticChatPermissionNode>()

    operator fun LunaticChatPermissionNode.unaryPlus() {
        permissions.add(this)
    }
}

fun LunaticChatPermissionNode.has(player: Player): Boolean = player.hasPermission(this.permissionNode)

/**
 * Checks if the player has at least one of the specified permissions.
 *
 * @param block A lambda with receiver to collect permissions.
 * @return `true` if the player has at least one of the specified permissions.
 */
fun Player.hasAnyPermission(block: PermissionCollector.() -> Unit): Boolean =
    PermissionCollector()
        .apply(block)
        .permissions
        .any { it.has(this) }

/**
 * Checks if the player has all the specified permissions.
 *
 * @param block A lambda with receiver to collect permissions.
 * @return `true` if the player has all the specified permissions.
 */
fun Player.hasAllPermission(block: PermissionCollector.() -> Unit): Boolean =
    PermissionCollector()
        .apply(block)
        .permissions
        .all { it.has(this) }

/**
 * Requires the player to have at least one of the specified permissions.
 *
 * @param block A lambda with receiver to collect permissions.
 * @return Throws [RequirePermissionException] if the player lacks all specified permissions.
 * @throws RequirePermissionException if the player lacks all specified permissions.
 */
fun Player.requirePermission(block: PermissionCollector.() -> Unit) {
    val result =
        PermissionCollector()
            .apply(block)
            .permissions
    if (!result.any { it.has(this) }) {
        throw RequirePermissionException(result)
    }
}
