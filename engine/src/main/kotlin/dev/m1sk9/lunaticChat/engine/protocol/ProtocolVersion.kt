package dev.m1sk9.lunaticChat.engine.protocol

/**
 * LunaticChat protocol version definition
 *
 * Manages version of communication protocol between Paper and Velocity.
 * MAJOR.MINOR must match; differences in PATCH are compatible.
 */
object ProtocolVersion {
    const val MAJOR = 1
    const val MINOR = 0
    const val PATCH = 0

    val version: String = "$MAJOR.$MINOR.$PATCH"

    /**
     * Checks if specified protocol version is compatible
     *
     * @param major Major version
     * @param minor Minor version
     * @return true if MAJOR and MINOR match
     */
    fun isCompatible(
        major: Int,
        minor: Int,
    ): Boolean = MAJOR == major && MINOR == minor

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
