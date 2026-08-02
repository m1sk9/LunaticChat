package dev.m1sk9.lunaticChat.engine.protocol

/**
 * The plugin messaging channel Paper and Velocity exchange [PluginMessage]s over.
 *
 * Both sides must agree on this exactly. Declaring it next to the codec keeps a rename from
 * silently splitting the two halves of the protocol: a Paper server and a proxy that disagree
 * still compile and start, they just stop talking.
 */
object PluginMessageChannel {
    const val NAMESPACE = "lunaticchat"
    const val NAME = "main"

    /** The channel in Bukkit's `namespace:name` form. */
    const val ID = "$NAMESPACE:$NAME"
}
