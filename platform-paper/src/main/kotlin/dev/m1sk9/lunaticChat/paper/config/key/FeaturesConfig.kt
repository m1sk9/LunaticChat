package dev.m1sk9.lunaticChat.paper.config.key

data class FeaturesConfig(
    val quickReplies: QuickRepliesFeatureConfig,
    val japaneseConversion: JapaneseConversionFeatureConfig,
    val channelChat: ChannelChatFeatureConfig,
    val velocityIntegration: VelocityIntegrationConfig,
)
