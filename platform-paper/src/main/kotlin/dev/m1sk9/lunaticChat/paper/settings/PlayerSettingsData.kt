package dev.m1sk9.lunaticChat.paper.settings

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Root data structure for player settings stored in YAML format.
 * All player settings are stored in a single YAML file with this structure.
 *
 * Example YAML format:
 * ```yaml
 * version: 1
 * japaneseConversion:
 *   ceaea267-39dd-3bac-931c-761ada671ebe: true
 *   another-uuid-here: false
 * ```
 *
 * @property version The schema version for future compatibility
 * @property japaneseConversion Map of player UUIDs to their Japanese conversion enabled status
 */
@Serializable
data class PlayerSettingsData(
    val version: Int = 1,
    val japaneseConversion: Map<
        @Serializable(with = UUIDASStringSerializer::class)
        UUID,
        Boolean,
    > = emptyMap(),
)
