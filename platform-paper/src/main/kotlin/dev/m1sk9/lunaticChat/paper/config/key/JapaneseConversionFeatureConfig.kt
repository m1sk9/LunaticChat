package dev.m1sk9.lunaticChat.paper.config.key

import kotlinx.serialization.Serializable

/**
 * @property enabled Whether romaji-to-Japanese conversion is offered to players
 * @property cache Conversion cache tuning
 * @property api Google IME request tuning
 */
@Serializable
data class JapaneseConversionFeatureConfig(
    val enabled: Boolean = false,
    val cache: ConversionCacheConfig = ConversionCacheConfig(),
    val api: ConversionApiConfig = ConversionApiConfig(),
)

/**
 * @property maxEntries Upper bound on cached word conversions
 * @property saveIntervalSeconds How often the cache is flushed to disk
 * @property filePath Cache file, relative to the plugin data folder
 */
@Serializable
data class ConversionCacheConfig(
    val maxEntries: Int = 500,
    val saveIntervalSeconds: Int = 300,
    val filePath: String = "conversion_cache.json",
)

/**
 * @property timeout Per-request timeout in milliseconds
 */
@Serializable
data class ConversionApiConfig(
    val timeout: Long = 3000,
)
