import org.gradle.launcher.Main

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
}

group = "ru.testit"
version = "0.1.1"

val slf4jVersion = "1.7.2"

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation("ru.testit:testit-api-client-kotlin:0.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
    implementation("ch.qos.logback:logback-core:1.5.8")
    testImplementation("ch.qos.logback:logback-classic:1.5.8")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}

publishing.publications.named<MavenPublication>("maven") {
    pom {
        from(components["java"])
    }
}

tasks.jar {
    manifest {
        attributes(mapOf(
            "Automatic-Module-Name" to "ru.testit.testit-kotlin-commons"
        ))
    }
}