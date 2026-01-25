package dev.m1sk9.lunaticChat.engine.exception

/**
 * Exception thrown when there is an error saving channel storage.
 *
 * @param message The detail message for the exception.
 * @param cause The cause of the exception, if any.
 */
class ChannelStorageSaveException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
