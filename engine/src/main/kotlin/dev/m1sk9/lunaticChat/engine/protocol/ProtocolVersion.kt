package dev.m1sk9.lunaticChat.engine.protocol

/**
 * LunaticChat protocol version definition
 *
 * Manages version of communication protocol between Paper and Velocity.
 *
 * ## Version Bump Rules
 *
 * **PATCH** (e.g., 1.0.0 -> 1.0.1):
 * - Add optional fields with default values to existing message types.
 * - Add new sub-channels that peers can safely ignore.
 * - No deployment coordination required.
 *
 * **MINOR** (e.g., 1.0.x -> 1.1.0):
 * - Add required fields to existing messages.
 * - Add sub-channels whose absence degrades functionality.
 * - Deployment order: update Velocity first, then Paper servers.
 * - Set [MIN_SUPPORTED_MINOR] to control the deprecation window.
 *
 * **MAJOR** (e.g., 1.x.x -> 2.0.0):
 * - Remove or rename existing sub-channels or fields.
 * - Change wire format or encoding.
 * - Requires simultaneous deployment of all components.
 *
 * ## Adding a New Message Type (sub-channel)
 * 1. Add the data class to [PluginMessage].
 * 2. Add a sub-channel constant to [PluginMessageCodec.SubChannel].
 * 3. Add encode/decode branches in [PluginMessageCodec].
 * 4. Add a backward compatibility snapshot to ProtocolBackwardCompatibilityTest.
 * 5. Bump PATCH if the new sub-channel is optional, MINOR if it is required.
 */
object ProtocolVersion {
    const val MAJOR = 1
    const val MINOR = 0
    const val PATCH = 1

    /**
     * Minimum MINOR version this build can interoperate with (same MAJOR).
     *
     * When bumping MINOR, set this to the oldest MINOR version that should still
     * be accepted. This allows a controlled deprecation window for rolling updates.
     */
    const val MIN_SUPPORTED_MINOR = 0

    val version: String = "$MAJOR.$MINOR.$PATCH"

    /**
     * Checks if specified protocol version is compatible
     *
     * Compatibility rules:
     * - MAJOR must match exactly.
     * - Remote MINOR must be >= [MIN_SUPPORTED_MINOR] and <= [MINOR].
     * - PATCH is always ignored.
     *
     * @param major Major version
     * @param minor Minor version
     * @return true if compatible
     */
    fun isCompatible(
        major: Int,
        minor: Int,
    ): Boolean = MAJOR == major && minor in MIN_SUPPORTED_MINOR..MINOR

    /**
     * Checks compatibility from version string
     *
     * @param version Version string in "MAJOR.MINOR.PATCH" format
     * @return true if compatible
     */
    fun isCompatible(version: String): Boolean {
        val parts = version.split(".")
        if (parts.size != 3) return false

        val major = parts[0].toIntOrNull() ?: return false
        val minor = parts[1].toIntOrNull() ?: return false

        return isCompatible(major, minor)
    }
}
