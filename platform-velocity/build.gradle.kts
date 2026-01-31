plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    // Engine module (provides serialization, coroutines, ktor)
    api(project(":engine"))

    // Velocity-specific dependencies
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    implementation("com.charleskorn.kaml:kaml:0.104.0") // YAML configuration

    // Test dependencies
    testImplementation("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks {
    shadowJar {
        archiveClassifier.set("velocity")
        archiveBaseName.set("LunaticChat")
    }

    jar {
        enabled = false
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("velocity-plugin.json") {
            expand(props)
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
