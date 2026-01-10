package dev.m1sk9.lunaticChat.engine.exception

import java.nio.file.Path

class ConversionCacheFileNotFoundException(
    cacheFilePath: Path,
) : Exception("Conversion cache file not found at path: $cacheFilePath")
