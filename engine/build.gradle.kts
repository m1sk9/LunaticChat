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
    // Every module now compiles against 5.2.0, but LunaticChat still supports Velocity 3.5.x proxies,
    // which ship Adventure 4.26.1 at runtime. engine must therefore stay within the API surface both
    // versions share -- currently just the Component type -- and the compiler cannot check that for us.
    // Dropping Velocity 3.5.x support is what actually lifts this constraint.
    compileOnly("net.kyori:adventure-api:5.2.0")

    testImplementation("net.kyori:adventure-api:5.2.0")
}
