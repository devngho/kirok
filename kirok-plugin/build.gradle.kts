import java.net.URL
import java.util.zip.ZipFile

plugins {
    kotlin("jvm")
    `maven-publish`
    `java-gradle-plugin`
    signing
}

group = "io.github.devngho"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
    compileOnly("org.slf4j:slf4j-api:2.0.7")
}

gradlePlugin {
    plugins {
        create("kirok-plugin") {
            id = "io.github.devngho.kirok.plugin"
            displayName = "kirok Gradle Plugin"
            description = "kirok Gradle Plugin for using kirok easily"
            implementationClass = "io.github.devngho.kirok.plugin.KirokGradlePlugin"
        }
    }
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

    publications.withType(MavenPublication::class) {
        groupId = project.group as String?
        artifactId = "kirok-plugin"
        version = project.version as String?

        pom {
            name.set(artifactId)
            description.set("WASM MVI 프레임워크")
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
}

kotlin {
    publishing {
        kirok()
    }
}