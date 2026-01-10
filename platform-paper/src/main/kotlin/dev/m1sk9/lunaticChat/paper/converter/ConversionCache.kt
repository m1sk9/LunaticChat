package dev.m1sk9.lunaticChat.paper.converter

import dev.m1sk9.lunaticChat.engine.exception.ConversionCacheFileNotFoundException
import kotlinx.serialization.json.Json
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists
import kotlin.io.path.writeText

class ConversionCache(
    private val cacheFile: Path,
    private val maxEntries: Int = 500,
    private val plugin: JavaPlugin,
    private val logger: Logger,
) {
    private val conversionMemoryCache = ConcurrentHashMap<String, String>()
    private val conversionSaveQueue = AtomicBoolean(false)

    companion object {
        private const val CACHE_VERSION = "1"
    }

    /**
     * Loads the conversion cache from disk into memory.
     * If the cache file does not exist or version is incompatible, initializes it with an empty cache.
     */
    fun loadFromDisk() {
        if (!cacheFile.exists()) {
            logger.info("Cache file not found, initializing new cache file at: $cacheFile")
            initializeEmptyCache()
            return
        }

        try {
            val jsonBuffer = cacheFile.bufferedReader().use { it.readText() }
            val cacheData = Json.decodeFromString<CacheData>(jsonBuffer)

            if (cacheData.version != CACHE_VERSION) {
                logger.warning("Cache version mismatch (expected: $CACHE_VERSION, found: ${cacheData.version}). Reinitializing cache.")
                initializeEmptyCache()
                return
            }

            conversionMemoryCache.putAll(cacheData.entries)
            logger.info("Loaded ${conversionMemoryCache.size} cache entries from disk.")
        } catch (e: Exception) {
            logger.severe("Failed to load conversion cache from disk: ${e.message}")
            logger.info("Reinitializing cache due to error.")
            initializeEmptyCache()
        }
    }

    private fun initializeEmptyCache() {
        val emptyData = CacheData(version = CACHE_VERSION, entries = emptyMap())
        val jsonBuffer = Json.encodeToString(CacheData.serializer(), emptyData)
        cacheFile.writeText(jsonBuffer)
    }

    /**
     * Retrieves a cached conversion result by key.
     *
     * @param key The key for the cached conversion.
     * @return The cached conversion result, or null if not found.
     */
    fun get(key: String): String? = conversionMemoryCache[key]

    /**
     * Stores a conversion result in the cache.
     *
     * @param key The key for the conversion.
     * @param value The conversion result to cache.
     */
    fun put(
        key: String,
        value: String,
    ) {
        if (conversionMemoryCache.size >= maxEntries) {
            evictOldestEntry()
        }

        conversionMemoryCache[key] = value
        queueSaveToDisk()
    }

    /**
     * Saves the conversion cache from memory to disk.
     * This operation is performed asynchronously.
     *
     * @throws Exception if an error occurs during the save operation.
     */
    fun saveToDisk() {
        try {
            val data =
                CacheData(
                    version = CACHE_VERSION,
                    entries = conversionMemoryCache.toMap(),
                )
            val jsonBuffer = Json.encodeToString(CacheData.serializer(), data)
            cacheFile.writeText(jsonBuffer)
            logger.info("Saved ${conversionMemoryCache.size} cache entries to disk.")
        } catch (e: Exception) {
            logger.severe("Failed to save conversion cache to disk: ${e.message}")
        }
    }

    private fun queueSaveToDisk() {
        if (conversionSaveQueue.compareAndSet(false, true)) {
            Bukkit.getScheduler().runTaskAsynchronously(
                plugin,
                Runnable {
                    Thread.sleep(5000) // 5 seconds delay to batch multiple save requests
                    conversionSaveQueue.set(true)
                    saveToDisk()
                },
            )
        }
    }

    private fun evictOldestEntry() {
        val toRemove = conversionMemoryCache.size / 10
        conversionMemoryCache.keys.take(toRemove).forEach {
            conversionMemoryCache.remove(it)
        }
    }
}
