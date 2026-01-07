plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
    kotlin("kapt")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
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
