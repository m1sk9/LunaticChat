package dev.m1sk9.lunaticChat.paper

import kotlin.test.Test
import kotlin.test.assertEquals

class BuildInfoTest {
    @Test
    fun `stable version is shown with the commit hash appended`() {
        assertEquals("1.3.0 (44132f3)", BuildInfo.formatVersion("1.3.0", "44132f3"))
    }

    @Test
    fun `nightly version already carrying the commit hash is shown as is`() {
        assertEquals("1.3.0-nightly.44132f3", BuildInfo.formatVersion("1.3.0-nightly.44132f3", "44132f3"))
    }
}
