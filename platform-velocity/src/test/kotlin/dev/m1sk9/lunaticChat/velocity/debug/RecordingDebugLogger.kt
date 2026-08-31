package dev.m1sk9.lunaticChat.velocity.debug

import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.engine.debug.DebugLogger

/** A [DebugLogger] with every category on, keeping what it was told for assertions. */
class RecordingDebugLogger : DebugLogger {
    val lines = mutableListOf<String>()

    override fun isEnabled(category: DebugCategory): Boolean = true

    override fun log(
        category: DebugCategory,
        message: () -> String,
    ) {
        lines += "${category.key}: ${message()}"
    }
}
