package dev.m1sk9.lunaticChat.paper.converter

import dev.m1sk9.lunaticChat.paper.StoppableService
import dev.m1sk9.lunaticChat.paper.storage.FileStore
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

class ConversionCache(
    private val store: FileStore,
    val maxEntries: Int = 500,
    private val logger: Logger,
) : StoppableService {
    private val conversionMemoryCache = ConcurrentHashMap<String, String>()
    private val dirty = AtomicBoolean(false)

    /** How full the cache is, for `/lc dump`. Words themselves never leave this class. */
    val entryCount: Int get() = conversionMemoryCache.size

    companion object {
        private const val CACHE_VERSION = "1"
    }

    /**
     * Loads the conversion cache from disk into memory.
     * If the cache file does not exist or version is incompatible, initializes it with an empty cache.
     */
    fun loadFromDisk() {
        val jsonBuffer =
            store.read() ?: run {
                logger.info("Cache file not found, initializing new cache file at: ${store.name}")
                initializeEmptyCache()
                return
            }

        try {
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
        store.write(Json.encodeToString(CacheData.serializer(), emptyData))
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
        dirty.set(true)
    }

    /**
     * Writes the cache to disk if anything changed since the last write.
     *
     * Called from the periodic task and at shutdown. Skipping a clean cache matters because the
     * task fires on a fixed interval whether or not anyone chatted.
     *
     * A failed write leaves the previous file untouched: a cache that does not parse is discarded
     * wholesale on the next boot, so a torn file costs every entry accumulated so far.
     */
    fun saveToDisk() {
        if (!dirty.getAndSet(false)) return

        try {
            val data =
                CacheData(
                    version = CACHE_VERSION,
                    entries = conversionMemoryCache.toMap(),
                )
            store.write(Json.encodeToString(CacheData.serializer(), data))
            logger.info("Saved ${conversionMemoryCache.size} cache entries to disk.")
        } catch (e: Exception) {
            dirty.set(true)
            logger.severe("Failed to save conversion cache to disk: ${e.message}")
        }
    }

    /** Flushes on the shutdown path; the periodic task calls [saveToDisk] directly. */
    override fun stop() = saveToDisk()

    // FIXME: ConcurrentHashMap keys are unordered, so evicting "oldest" entries
    // actually evicts random entries. Consider using LinkedHashMap with access-order
    // or implement proper LRU cache with timestamp tracking.
    private fun evictOldestEntry() {
        val toRemove = conversionMemoryCache.size / 10
        conversionMemoryCache.keys.take(toRemove).forEach {
            conversionMemoryCache.remove(it)
        }
    }
}
