package dev.m1sk9.lunaticChat.paper.config.key

import kotlinx.serialization.Serializable

@Serializable
data class FeaturesConfig(
    val quickReplies: QuickRepliesFeatureConfig = QuickRepliesFeatureConfig(),
    val japaneseConversion: JapaneseConversionFeatureConfig = JapaneseConversionFeatureConfig(),
    val channelChat: ChannelChatFeatureConfig = ChannelChatFeatureConfig(),
    val velocityIntegration: VelocityIntegrationConfig = VelocityIntegrationConfig(),
)
