package dev.m1sk9.lunaticChat.engine.permission

sealed class LunaticChatPermissionNode(
    val permissionNode: String,
) {
    object Tell : LunaticChatPermissionNode("lunaticchat.command.tell")

    object Reply : LunaticChatPermissionNode("lunaticchat.command.reply")

    object JapaneseToggle : LunaticChatPermissionNode("lunaticchat.command.jp")

    object Spy : LunaticChatPermissionNode("lunaticchat.spy")
}
