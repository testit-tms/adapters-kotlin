import java.time.Duration


plugins {
    kotlin("jvm") version "2.0.20"
    `maven-publish`
    signing
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

configure(subprojects) {
    group = "ru.testit"
    version = version

    apply(plugin = "signing")
    apply(plugin = "maven-publish")

    repositories {
        // JReleaser staging repository
        maven {
            name = "staging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
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

// JReleaser helper tasks
tasks.register("jreleaserStage") {
    group = "publishing"
    description = "Stages all modules for JReleaser deployment"
    
    doLast {
        println("✅ All modules staged for JReleaser deployment")
        println("📁 Staging directories:")
        subprojects.forEach { project ->
            val stagingDir = project.layout.buildDirectory.dir("staging-deploy").get().asFile
            if (stagingDir.exists()) {
                println("   ${project.name}: ${stagingDir.absolutePath}")
            }
        }
        println("🚀 Run 'jreleaser deploy' to publish to Maven Central")
    }
}

// Configure dependencies after all projects are evaluated
afterEvaluate {
    subprojects.forEach { project ->
        val stagingTask = project.tasks.findByName("publishMavenPublicationToStagingRepository")
        if (stagingTask != null) {
            tasks.named("jreleaserStage").configure {
                dependsOn(stagingTask)
            }
        }
    }
}