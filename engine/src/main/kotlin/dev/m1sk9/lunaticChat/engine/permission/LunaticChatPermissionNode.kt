package dev.m1sk9.lunaticChat.engine.permission

sealed class LunaticChatPermissionNode(
    val permissionNode: String,
) {
    object Lc : LunaticChatPermissionNode("lunaticchat.command.lc")

    object Tell : LunaticChatPermissionNode("lunaticchat.command.tell")

    object Reply : LunaticChatPermissionNode("lunaticchat.command.reply")

    object Settings : LunaticChatPermissionNode("lunaticchat.command.lc.settings")

    object Status : LunaticChatPermissionNode("lunaticchat.command.lc.status")

    object Channel : LunaticChatPermissionNode("lunaticchat.command.lc.channel")

    object ChannelCreate : LunaticChatPermissionNode("lunaticchat.command.lc.channel.create")

    object ChannelList : LunaticChatPermissionNode("lunaticchat.command.lc.channel.list")

    object ChannelJoin : LunaticChatPermissionNode("lunaticchat.command.lc.channel.join")

    object ChannelLeave : LunaticChatPermissionNode("lunaticchat.command.lc.channel.leave")

    object ChannelSwitch : LunaticChatPermissionNode("lunaticchat.command.lc.channel.switch")

    object ChannelStatus : LunaticChatPermissionNode("lunaticchat.command.lc.channel.status")

    object ChannelInfo : LunaticChatPermissionNode("lunaticchat.command.lc.channel.info")

    object ChannelDelete : LunaticChatPermissionNode("lunaticchat.command.lc.channel.delete")

    object ChannelInvite : LunaticChatPermissionNode("lunaticchat.command.lc.channel.invite")

    object ChannelKick : LunaticChatPermissionNode("lunaticchat.command.lc.channel.kick")

    object ChannelBan : LunaticChatPermissionNode("lunaticchat.command.lc.channel.ban")

    object ChannelUnban : LunaticChatPermissionNode("lunaticchat.command.lc.channel.unban")

    object ChannelMod : LunaticChatPermissionNode("lunaticchat.command.lc.channel.mod")

    object ChannelOwnership : LunaticChatPermissionNode("lunaticchat.command.lc.channel.ownership")

    object ChatMode : LunaticChatPermissionNode("lunaticchat.command.lc.chatmode")

    object ChatModeToggle : LunaticChatPermissionNode("lunaticchat.command.lc.chatmode.toggle")

    object VelocityStatus : LunaticChatPermissionNode("lunaticchat.command.lcv.status")

    object Spy : LunaticChatPermissionNode("lunaticchat.spy")

    object NoticeUpdate : LunaticChatPermissionNode("lunaticchat.noticeupdate")

    object ChannelBypass : LunaticChatPermissionNode("lunaticchat.channelbypass")
}
