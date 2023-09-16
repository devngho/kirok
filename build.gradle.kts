plugins {
    kotlin("multiplatform") version "1.9.0"
    id("org.jetbrains.dokka") version "1.8.20"
    id("com.google.devtools.ksp") version "1.9.0-1.0.13" apply false
    signing
    `maven-publish`
}

group = "io.github.devngho"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


fun PublishingExtension.kirok() {
    signing {
        sign(publishing.publications)
    }

    repositories {
        if (version.toString().endsWith("SNAPSHOT")) {
            mavenLocal()
        } else {
            maven("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
                name = "sonatypeReleaseRepository"
                credentials(PasswordCredentials::class)
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
        kirok()
    }
}

kotlin {
    publishing {
        kirok()
    }

    jvm()
    wasm {
        browser { binaries.executable() }
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
                implementation("com.squareup:kotlinpoet-ksp:1.14.2")
                implementation("com.google.devtools.ksp:symbol-processing-api:1.9.0-1.0.13")
            }
            kotlin.srcDir("src/main/kotlin")
            resources.srcDir("src/main/resources")
        }
    }
}