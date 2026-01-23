package dev.m1sk9.lunaticChat.paper.command.setting

/**
 * Registry for all setting handlers.
 * Provides centralized access to setting handlers and ensures type safety.
 */
class SettingHandlerRegistry {
    private val handlers = mutableMapOf<SettingKey, SettingHandler>()

    /**
     * Registers a setting handler.
     *
     * @param handler The handler to register
     * @return This registry for method chaining
     */
    fun register(handler: SettingHandler): SettingHandlerRegistry {
        handlers[handler.key] = handler
        return this
    }

    /**
     * Registers multiple setting handlers at once.
     *
     * @param handlers The handlers to register
     * @return This registry for method chaining
     */
    fun registerAll(vararg handlers: SettingHandler): SettingHandlerRegistry {
        handlers.forEach { register(it) }
        return this
    }

    /**
     * Retrieves a handler for a specific setting key.
     *
     * @param key The setting key to look up
     * @return The handler if found, null otherwise
     */
    fun getHandler(key: SettingKey): SettingHandler? = handlers[key]

    /**
     * Returns all registered setting keys.
     * Useful for tab completion.
     */
    fun getAvailableKeys(): List<String> = handlers.keys.map { it.key }
}
