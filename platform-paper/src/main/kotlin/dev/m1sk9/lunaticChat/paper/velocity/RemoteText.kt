package dev.m1sk9.lunaticChat.paper.velocity

// The code character goes with the sign: dropping only the sign would leave "§cAdmin" reading
// as "cAdmin".
private val LEGACY_CODE = Regex("§.?", RegexOption.DOT_MATCHES_ALL)

/**
 * Strips what a relayed value must not carry into rendering or logging: legacy formatting codes
 * and control characters.
 *
 * Applied where a value arrives from the proxy rather than inside the chat formats, because the
 * formats parse the whole string as legacy text and cannot tell a code the operator wrote from
 * one smuggled in a value. Typed chat never needs this - the client refuses both - but a relayed
 * field only passed through another server, or came from a client that never typed it at all.
 */
internal fun String.neutralized(): String = replace(LEGACY_CODE, "").filterNot { it.isISOControl() }
