@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    kotlin("jvm") version "2.1.0" apply false
    kotlin("multiplatform") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
    id("org.jetbrains.dokka") version "2.0.0"
    signing
    `maven-publish`
}

group = "io.github.devngho"
version = "1.1.4"

repositories {
    mavenCentral()
}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

fun PublishingExtension.kirok() {
    signing {
        sign(publishing.publications)
    }

    repositories {
        mavenLocal()
        val id: String =
            if (project.hasProperty("repoUsername")) project.property("repoUsername") as String
            else System.getenv("repoUsername")
        val pw: String =
            if (project.hasProperty("repoPassword")) project.property("repoPassword") as String
            else System.getenv("repoPassword")
        if (!version.toString().endsWith("SNAPSHOT")) {
            maven("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
                name = "sonatypeReleaseRepository"
                credentials {
                    username = id
                    password = pw
                }
            }
        }
    }

    fun MavenPublication.kirok() {
        pom {
            name.set(artifactId)
            description.set("Frontend Logic Library for Kotlin/Wasm")
            url.set("https://github.com/devngho/kirok")

            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://github.com/devngho/kirok/blob/master/LICENSE")
                }
            }
            developers {
                developer {
                    id.set("devngho")
                    name.set("devngho")
                    email.set("yjh135908@gmail.com")
                }
            }
            scm {
                connection.set("https://github.com/devngho/kirok.git")
                developerConnection.set("https://github.com/devngho/kirok.git")
                url.set("https://github.com/devngho/kirok")
            }
        }
    }

    publications.withType(MavenPublication::class) {
        groupId = project.group as String?
        artifactId = "kirok"
        version = project.version as String?

        artifact(tasks["javadocJar"])

        kirok()
    }
}

kotlin {
    publishing {
        kirok()
    }

    jvm()
    wasmJs {
        browser { binaries.executable() }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("reflect"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
            }
        }

        applyDefaultHierarchyTemplate()
    }
}

tasks {
    val taskList = this.toList().map { it.name }
    getByName("signKotlinMultiplatformPublication") {
        if (taskList.contains("publishJvmPublicationToSonatypeReleaseRepositoryRepository"))
            dependsOn(
                "publishJvmPublicationToSonatypeReleaseRepositoryRepository",
                "publishJvmPublicationToMavenLocalRepository",
                "publishJvmPublicationToMavenLocal"
            )
        else dependsOn("publishJvmPublicationToMavenLocalRepository", "publishJvmPublicationToMavenLocal")
    }
    getByName("signWasmJsPublication") {
        if (taskList.contains("publishJvmPublicationToSonatypeReleaseRepositoryRepository"))
            dependsOn(
                "publishJvmPublicationToSonatypeReleaseRepositoryRepository",
                "publishKotlinMultiplatformPublicationToSonatypeReleaseRepositoryRepository",
                "publishJvmPublicationToMavenLocal",
                "publishJvmPublicationToMavenLocalRepository",
                "publishKotlinMultiplatformPublicationToMavenLocal",
                "publishKotlinMultiplatformPublicationToMavenLocalRepository"
            )
        else
            dependsOn(
                "publishJvmPublicationToMavenLocal",
                "publishKotlinMultiplatformPublicationToMavenLocal",
                "publishKotlinMultiplatformPublicationToMavenLocalRepository"
            )
    }
}