package dev.m1sk9.lunaticChat.paper.config.key

data class MessageFormatConfig(
    val directMessageFormat: String = "§7[§e{sender} §7>> §e{recipient}§7] §f{message}",
    val channelMessageFormat: String = "§7[§b#{channel}§7] §e{sender}: §f{message}",
    val crossServerGlobalChatFormat: String = "§7[§6{server}§7] §e{sender}: §f{message}",
)
