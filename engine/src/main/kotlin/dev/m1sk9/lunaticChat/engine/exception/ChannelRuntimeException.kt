package dev.m1sk9.lunaticChat.engine.exception

class ChannelRuntimeException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
