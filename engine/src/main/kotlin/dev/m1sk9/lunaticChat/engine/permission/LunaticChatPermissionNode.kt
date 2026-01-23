package dev.m1sk9.lunaticChat.engine.permission

sealed class LunaticChatPermissionNode(
    val permissionNode: String,
) {
    object Lc : LunaticChatPermissionNode("lunaticchat.command.lc")

    object Tell : LunaticChatPermissionNode("lunaticchat.command.tell")

    object Reply : LunaticChatPermissionNode("lunaticchat.command.reply")

    object Settings : LunaticChatPermissionNode("lunaticchat.command.lc.settings")

    object Spy : LunaticChatPermissionNode("lunaticchat.spy")

    object NoticeUpdate : LunaticChatPermissionNode("lunaticchat.noticeUpdate")
}
