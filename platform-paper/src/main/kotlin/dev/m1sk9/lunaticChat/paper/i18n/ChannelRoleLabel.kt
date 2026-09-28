package dev.m1sk9.lunaticChat.paper.i18n

import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole

/** The name of [role] in the configured language, as both `{role}` and `/lc channel status` show it. */
fun LanguageManager.channelRoleLabel(role: ChannelRole): String =
    getMessage(
        when (role) {
            ChannelRole.OWNER -> "channel.role.owner"
            ChannelRole.MODERATOR -> "channel.role.moderator"
            ChannelRole.MEMBER -> "channel.role.member"
        },
    )
