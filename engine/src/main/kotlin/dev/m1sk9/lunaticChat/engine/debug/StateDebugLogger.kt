package dev.m1sk9.lunaticChat.engine.debug

/**
 * A [DebugLogger] that consults [state] and hands each enabled line, prefixed with its category,
 * to [sink]. Platforms supply only the sink, since the log API is the only part that differs.
 */
class StateDebugLogger(
    private val state: DebugState,
    private val sink: (String) -> Unit,
) : DebugLogger {
    override fun isEnabled(category: DebugCategory): Boolean = state.isEnabled(category)

    override fun log(
        category: DebugCategory,
        message: () -> String,
    ) {
        if (!state.isEnabled(category)) return
        sink("[LC/${category.key}] ${message()}")
    }
}
