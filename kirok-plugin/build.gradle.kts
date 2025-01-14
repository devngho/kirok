plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("com.gradle.plugin-publish") version "1.1.0"
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.5.1")
    compileOnly("org.slf4j:slf4j-api:2.0.7")
}

gradlePlugin {
    website.set("https://github.com/devngho/kirok")
    vcsUrl.set("https://github.com/devngho/kirok.git")
    plugins {
        create("kirok-plugin") {
            id = "io.github.devngho.kirok.plugin"
            displayName = "kirok Gradle Plugin"
            description = "kirok Gradle Plugin for using kirok easily"
            implementationClass = "io.github.devngho.kirok.plugin.KirokGradlePlugin"
            tags.set(listOf("wasm", "kotlin", "kirok"))
        }
    }
}

publishing {
    repositories {
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
}

signing {
    sign(publishing.publications)
}

afterEvaluate {
    tasks.withType<GenerateMavenPom>().configureEach {
        doFirst {
            (this as GenerateMavenPom).pom.apply {
                name.set("kirok-plugin")
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
    }
}

kotlin.jvmToolchain(21)