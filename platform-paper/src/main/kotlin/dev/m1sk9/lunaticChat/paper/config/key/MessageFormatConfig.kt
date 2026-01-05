package dev.m1sk9.lunaticChat.paper.config.key

data class MessageFormatConfig(
    val directMessageFormat: String = "&7[&e{sender} &7>>] &f{message}",
    val crossServerMessageFormat: String = "&7[&e{sender} &7(&b{server}&7)] &f{message}",
    val crossServerDirectMessageFormat: String = "&7[&e{sender}@&7(&b{server}&7) >>] &f{message}",
)
