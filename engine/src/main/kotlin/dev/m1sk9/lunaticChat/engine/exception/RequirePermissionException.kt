package dev.m1sk9.lunaticChat.engine.exception

import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode

/**
 * Exception thrown when required permissions are missing.
 *
 * @param permissions The list of missing permissions.
 */
class RequirePermissionException(
    permissions: List<LunaticChatPermissionNode>,
) : Exception("Missing required permissions: ${permissions.joinToString { it.permissionNode }}")
