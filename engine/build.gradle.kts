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
    // Pinned to the Paper-bundled version. Velocity (velocity-api 3.5.1) still ships Adventure 4.26.1,
    // so engine must stay within the API surface both versions share -- currently just the Component type.
    // Once platform-velocity moves to velocity-api 4.x (Adventure 5.2.0), this constraint goes away.
    compileOnly("net.kyori:adventure-api:5.2.0")

    testImplementation("net.kyori:adventure-api:5.2.0")
}
