plugins {
    kotlin("jvm")
    `maven-publish`
    `java-gradle-plugin`
    signing
}

group = "io.github.devngho"
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
    implementation(project(":kirok-binding"))
    implementation(project(":"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.5.1")
    implementation("com.google.code.gson:gson:2.10.1")
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

kotlin.jvmToolchain(19)

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