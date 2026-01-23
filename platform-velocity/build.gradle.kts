plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

dependencies {
    // Engine module (provides serialization, coroutines, ktor)
    api(project(":engine"))
}

tasks {
    shadowJar {
        archiveClassifier.set("all")
        archiveBaseName.set("LunaticChat-Velocity")
    }

    build {
        dependsOn(shadowJar)
    }
}
