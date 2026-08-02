package dev.m1sk9.lunaticChat.engine.chat.channel

/**
 * A player's active channel together with its member list.
 */
data class ChannelContext(
    val channel: Channel,
    val members: List<ChannelMember>,
) {
    /** Shorthand for the channel's id, which callers ask for far more often than the channel. */
    val channelId: String get() = channel.id
}
