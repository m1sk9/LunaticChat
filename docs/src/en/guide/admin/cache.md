# Cache System

LunaticChat includes a system that automatically caches phrases from Japanese romanization conversion to both memory and disk.

For information on Japanese romanization conversion, see [here](../player/japanese-romanization.md).

## Memory Cache

LunaticChat caches converted phrases in memory, allowing for fast responses when the same phrase is requested again.

This cache is temporary and is saved to disk cache at server restart or at configured intervals.

## Disk Cache

LunaticChat periodically saves the contents of the memory cache to disk. This allows the cache contents to be retained even after server restarts.

The file used for disk cache can be [changed in the configuration](configuration.md#cachefilepath).

## Cache Release

Memory cache is automatically released by the JVM's garbage collection after being cached to disk.

Disk cache can be released by manually deleting the file. LunaticChat will recreate the cache file on restart.

::: warning About Purge Functionality

LunaticChat does not implement a cache purge feature.

This is because allowing players to manipulate the host's file system is not desirable from a security perspective.

:::

## Cache Version

The file used for disk cache includes a `version` field, which handles changes to the cache format due to LunaticChat version updates.

If the version does not match, LunaticChat recognizes the cache file as **an old format cache**, ignores its contents, and recreates it in the new format.

```json
{"version":"1","entries":{}}
```
