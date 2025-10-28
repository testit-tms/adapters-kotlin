import org.gradle.api.tasks.testing.logging.TestLogEvent

group = "ru.testit"
version = "0.7.2"

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
}


java {
    withJavadocJar()
    withSourcesJar()
}

val slf4jVersion = "1.7.2"
val junitVersion = "5.8.2"
val junitLauncherVersion = "1.9.0"

dependencies {
    implementation(kotlin("reflect"))
    implementation(libs.kotest.framework.api)
    implementation(libs.kotest.framework.engine)
    implementation(libs.bundles.jaxb)
    implementation(project(":testit-kotlin-commons"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.moshi:moshi-adapters:1.15.1")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
    implementation("ch.qos.logback:logback-core:1.5.8")
    implementation("io.kotest:kotest-framework-datatest:5.9.1")

    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.jackson.module.kotlin)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.PASSED)
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
    }
    systemProperties(systemProperties)
    environment(environment)
}


kotlin {
    jvmToolchain(11)
}


tasks.jar {
    manifest {
        attributes(mapOf(
            "Automatic-Module-Name" to "ru.testit.testit-adapter-kotest"
        ))
    }
}
publishing.publications.named<MavenPublication>("maven") {
    pom {
        from(components["java"])
    }
}
