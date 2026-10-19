package dev.m1sk9.lunaticChat.velocity.messaging

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageChannel

internal val PLUGIN_MESSAGE_CHANNEL: MinecraftChannelIdentifier =
    MinecraftChannelIdentifier.create(PluginMessageChannel.NAMESPACE, PluginMessageChannel.NAME)
