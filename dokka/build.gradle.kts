plugins {
    kotlin("jvm") apply false
    id("org.jetbrains.dokka")
}

dependencies {
    dokka(project(":engine"))
    dokka(project(":platform-paper"))
    dokka(project(":platform-velocity"))
}

dokka {
    moduleName.set("LunaticChat")

    dokkaPublications.html {
        includes.from("README.md")
    }
}
