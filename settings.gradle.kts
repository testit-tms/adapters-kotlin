plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "adapters-kotlin"
include("testit-kotlin-commons")
include("testit-adapter-kotest")
