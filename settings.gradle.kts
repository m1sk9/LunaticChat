rootProject.name = "LunaticChat"

include(
    "engine",
    "platform-paper",
    "platform-velocity",
    "dokka"
)

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
