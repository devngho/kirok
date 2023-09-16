package io.github.devngho.kirok

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

class KirokGradlePlugin: Plugin<Project> {
    override fun apply(target: Project) {
        target.logger.warn("Hi from Kirok Gradle Plugin!")
        target.kotlinExtension.sourceSets.forEach {
            it.dependencies {
                implementation("io.github.devngho:kirok:1.0-SNAPSHOT")
                implementation("com.google.code.gson:gson:2.10.1")
            }
        }
        target.dependencies {
            add("kspCommonMainMetadata", project(":kirok-processor"))
            add("kspWasm", project(":kirok-processor"))
            add("kspWasmTest", project(":kirok-processor"))
        }
    }
}