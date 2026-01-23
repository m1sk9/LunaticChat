plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Core dependencies (exposed to platform modules via api())
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    api("io.ktor:ktor-client-core:3.4.0")
    api("io.ktor:ktor-client-cio:3.4.0")

    // Adventure API (provided by platform implementations)
    compileOnly("net.kyori:adventure-api:4.26.1")
}
