package dev.m1sk9.lunaticChat.engine.exception

/**
 * Exception thrown when there is an error loading channel storage.
 *
 * @param message The detail message for the exception.
 * @param cause The cause of the exception, if any.
 */
class ChannelStorageLoadException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
