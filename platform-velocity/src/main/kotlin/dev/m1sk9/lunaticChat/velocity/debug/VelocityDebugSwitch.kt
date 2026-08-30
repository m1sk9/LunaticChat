package dev.m1sk9.lunaticChat.velocity.debug

import dev.m1sk9.lunaticChat.engine.debug.DebugCategories
import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.engine.debug.DebugState
import org.slf4j.Logger

/**
 * Reads which debug categories the proxy should log.
 *
 * The proxy has no configuration file of its own and is not getting one: a file means a parser, a
 * parser means a dependency, and `platform-velocity` deliberately ships without so much as Ktor.
 * A system property and an environment variable cost nothing and are both reachable from the
 * command line and from a compose file.
 *
 * The proxy is also shared by every backend, so this is deliberately not carried over the handshake:
 * one Paper server's `debug: true` must not decide how much the proxy logs for everyone else.
 */
object VelocityDebugSwitch {
    const val SYSTEM_PROPERTY = "lunaticchat.debug"
    const val ENVIRONMENT_VARIABLE = "LUNATICCHAT_DEBUG"

    /**
     * The categories asked for, reporting any name that does not exist through [logger].
     *
     * @param property how the system property is read, injected so this is testable without
     *   mutating the JVM's own properties.
     */
    fun read(
        logger: Logger,
        property: (String) -> String? = System::getProperty,
        environment: (String) -> String? = System::getenv,
    ): DebugState {
        // The property wins so that a proxy started from a compose file carrying the environment
        // variable can still be overridden for one run without editing the file.
        val raw = property(SYSTEM_PROPERTY) ?: environment(ENVIRONMENT_VARIABLE) ?: ""
        val parsed = DebugCategories.parse(raw)

        parsed.unknown.forEach { name ->
            logger.warn(
                "Unknown LunaticChat debug category '{}'. Known categories: {}",
                name,
                DebugCategory.entries.joinToString(", ") { it.key },
            )
        }
        if (parsed.active.isNotEmpty()) {
            logger.info("LunaticChat debug logging is on for: {}", parsed.active.joinToString(", ") { it.key })
        }

        return DebugState(parsed.active)
    }
}
