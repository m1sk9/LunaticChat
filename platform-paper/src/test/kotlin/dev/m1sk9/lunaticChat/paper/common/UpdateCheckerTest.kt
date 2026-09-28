package dev.m1sk9.lunaticChat.paper.common

import dev.m1sk9.lunaticChat.paper.common.UpdateChecker.Companion.isNewer
import dev.m1sk9.lunaticChat.paper.common.UpdateChecker.Companion.latestPaperVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateCheckerTest {
    @Test
    fun `a Velocity release published after a Paper release does not hide the Paper version`() {
        val releases =
            listOf(
                GitHubRelease("velocity/v1.4.0"),
                GitHubRelease("v1.5.0"),
            )

        assertEquals("1.5.0", latestPaperVersion(releases))
    }

    @Test
    fun `the highest Paper version wins regardless of listing order`() {
        val releases =
            listOf(
                GitHubRelease("v1.4.0"),
                GitHubRelease("v1.10.0"),
                GitHubRelease("v1.9.2"),
            )

        assertEquals("1.10.0", latestPaperVersion(releases))
    }

    @Test
    fun `retired paper-prefixed tags still count as Paper releases`() {
        assertEquals("1.2.2", latestPaperVersion(listOf(GitHubRelease("paper/v1.2.2"))))
    }

    @Test
    fun `drafts and prereleases are not offered as updates`() {
        val releases =
            listOf(
                GitHubRelease("v2.0.0", draft = true),
                GitHubRelease("v1.9.0", prerelease = true),
                GitHubRelease("v1.5.0"),
            )

        assertEquals("1.5.0", latestPaperVersion(releases))
    }

    @Test
    fun `non-release tags such as nightly are ignored`() {
        assertNull(latestPaperVersion(listOf(GitHubRelease("nightly"), GitHubRelease("velocity/v1.3.0"))))
    }

    @Test
    fun `a higher minor or patch is newer`() {
        assertTrue(isNewer("1.5.0", "1.4.0"))
        assertTrue(isNewer("1.4.1", "1.4.0"))
        assertTrue(isNewer("1.10.0", "1.9.0"))
    }

    @Test
    fun `the same or an older version is not newer`() {
        assertFalse(isNewer("1.4.0", "1.4.0"))
        assertFalse(isNewer("1.3.9", "1.4.0"))
    }
}
