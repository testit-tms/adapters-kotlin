rootProject.name = "adapters-kotlin"


include("testit-kotlin-commons")
include("testit-adapter-kotest")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version "1.6.21"
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"

    }
}

