package dev.m1sk9.lunaticChat.paper.config.key

data class JapaneseConversionFeatureConfig(
    val enabled: Boolean,
    val cacheMaxEntries: Int,
    val cacheSaveIntervalSeconds: Int,
    val cacheFilePath: String,
    val apiTimeout: Long,
    val apiRetryAttempts: Int,
    val settingsDirectory: String = "settings",
)
