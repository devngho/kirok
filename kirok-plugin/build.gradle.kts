plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("com.gradle.plugin-publish") version "1.1.0"
    `java-gradle-plugin`
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
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

kotlin.jvmToolchain(19)