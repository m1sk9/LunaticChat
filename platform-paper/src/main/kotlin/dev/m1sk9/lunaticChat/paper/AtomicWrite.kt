package dev.m1sk9.lunaticChat.paper

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

/**
 * Replaces the file at this path with [content], so nothing ever reads a half-written file.
 *
 * Bukkit runs onDisable before cancelling scheduler tasks, so a shutdown save and a still-pending
 * debounced save can reach the same file at once. The temporary file gets a unique name for that
 * reason: a fixed sibling would only move the interleaving from the destination to the temporary
 * file, and the losing move would then fail with it already gone.
 */
fun Path.writeTextAtomically(content: String) {
    val temporaryFile = Files.createTempFile(parent, fileName.toString(), ".tmp")
    try {
        temporaryFile.writeText(content)
        try {
            Files.move(temporaryFile, this, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            // Network-mounted data directories (NFS, SMB) can refuse an atomic rename. A plain
            // replace is still better than writing the destination in place, since the content is
            // already complete by the time anything lands on top of it.
            Files.move(temporaryFile, this, StandardCopyOption.REPLACE_EXISTING)
        }
    } catch (e: Throwable) {
        temporaryFile.deleteIfExists()
        throw e
    }
}
