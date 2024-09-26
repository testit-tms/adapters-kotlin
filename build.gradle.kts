plugins {
    kotlin("jvm") version "2.0.20"
}

group = "ru.testit"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
//    implementation(files("testit-api-kotlin-client-1.0.0.jar"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}