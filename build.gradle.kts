import java.time.Duration


plugins {
    kotlin("jvm") version "2.0.20"
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

group = "ru.testit"

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}

val sonaUsername = providers.gradleProperty("sonatypeAccessToken")
val sonaPassword = providers.gradleProperty("sonatypeAccessPassword")


nexusPublishing {
    connectTimeout.set(Duration.ofMinutes(7))
    clientTimeout.set(Duration.ofMinutes(7))

    transitionCheckOptions {
        maxRetries.set(100)
        delayBetween.set(Duration.ofSeconds(10))
    }

    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(sonaUsername.get())
            password.set(sonaPassword.get())
        }
    }
}

configure(subprojects) {
    group = "ru.testit"
    version = version

    apply(plugin = "signing")
    apply(plugin = "maven-publish")
    // apply(plugin = "java")
    repositories {
        maven {
            val releasesUrl = uri("https://s01.oss.sonatype.org/content/repositories/releases")
            val snapshotsUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
            url = if (version.toString().toLowerCase().contains("snapshot")) snapshotsUrl else releasesUrl

            credentials {
                username = sonaUsername.get()
                password = sonaPassword.get()
            }
        }
        mavenLocal()
        mavenCentral()
    }

    publishing {
        
        publications {
            create<MavenPublication>("maven") {
                versionMapping {
                    allVariants {
                        fromResolutionResult()
                    }
                }
                pom {
                    name.set(project.name)
                    description.set("Module ${project.name} of TestIT Framework.")
                    url.set("https://github.com/testit-tms/adapters-kotlin")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("integration")
                            name.set("Integration team")
                            email.set("integrations@testit.software")
                        }
                    }
                    scm {
                        developerConnection.set("scm:git:git://github.com/testit-tms/adapters-kotlin")
                        connection.set("scm:git:git://github.com/testit-tms/adapters-kotlin")
                        url.set("https://github.com/testit-tms/adapters-kotlin")
                    }
                    issueManagement {
                        system.set("GitHub Issues")
                        url.set("https://github.com/testit-tms/adapters-kotlin/issues")
                    }
                }
            }
        }
    }

    signing {
        // val signingKeyId: String? by project
        // val signingKey: String? by project
        // val signingPassword: String? by project
        // useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)

        sign(publishing.publications["maven"])
    }

    tasks.withType<Sign>().configureEach {
        if (System.getProperty("disableSign") == "true") {
            enabled = false;
        }

        onlyIf { !project.version.toString().endsWith("-SNAPSHOT") }
    }

    
}
tasks.jar {
    manifest {
        attributes(mapOf(
            "Specification-Title" to project.name,
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        ))
    }
}
