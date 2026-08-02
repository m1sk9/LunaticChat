plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Core dependencies (exposed to platform modules via api())
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    api("io.ktor:ktor-client-core:3.5.2")
    api("io.ktor:ktor-client-cio:3.5.2")

    // Adventure API (provided by platform implementations)
    // Matches what every supported platform ships: Paper 26.2 and Velocity 4.x both bundle 5.2.0.
    compileOnly("net.kyori:adventure-api:5.2.0")

    testImplementation("net.kyori:adventure-api:5.2.0")
}
