package io.github.devngho.kirok.plugin

import io.github.devngho.kirok.plugin.GenerateKirokBinding.generateKirokBinding
import io.github.devngho.kirok.plugin.GenerateProtoBufScheme.generateProtoBufScheme
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import java.net.URI


class KirokGradlePlugin: Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            val kirokVersion = getVersion(this)

            if (!plugins.hasPlugin("com.google.devtools.ksp")) {
                logger.error("KSP plugin not found. Please apply the plugin first.")
                return
            }

            repositories.maven {
                it.url = URI("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
            }
            kotlinExtension.sourceSets.forEach {
                it.dependencies {
                    implementation("io.github.devngho:kirok:$kirokVersion")
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.1")
                }
            }
            kotlinExtension.sourceSets.maybeCreate("jvmMain").apply {
                dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-jvm:1.5.1")
                }
            }
            kotlinExtension.sourceSets.maybeCreate("wasmMain").apply {
                dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-wasm:1.5.2-wasm0")
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-wasm:1.5.2-wasm0")
                    implementation("org.jetbrains.kotlinx:atomicfu-wasm:0.20.2-wasm0")
                }
            }
            extensions.create("kirok", KirokExtension::class.java)

            task("generateKirokBinding").generateKirokBinding(target)
            task("generateProtoBufScheme").generateProtoBufScheme(target)

            tasks.getByName("assemble").finalizedBy("generateProtoBufScheme", "generateKirokBinding")
        }
    }

    fun getVersion(target: Project): String {
        val artifact = target.buildscript.configurations.getAt("classpath").resolvedConfiguration.resolvedArtifacts.find {
            it.moduleVersion.id.group == "io.github.devngho" && it.moduleVersion.id.name == "kirok-plugin"
        }
        return artifact?.moduleVersion?.id?.version ?: throw IllegalStateException("Kirok plugin not found. Please apply the plugin first.")
    }
}

open class KirokExtension {
    var binding: List<String> = listOf()
    var bindingDir = "./build/generated/kirok"
    var wasmDir = "./build/generated/kirok"
    var wasmJsDir = "./build/generated/kirok"
}

@Suppress("unused")
fun DependencyHandler.kirok(project: Project) {
    val kirokVersion = project.plugins.getPlugin(KirokGradlePlugin::class.java).getVersion(project)

    add("kspWasm", "io.github.devngho:kirok:$kirokVersion")
    add("kspWasmTest", "io.github.devngho:kirok:$kirokVersion")
    add("kspJvm", "io.github.devngho:kirok:$kirokVersion")
    add("kspJvmTest", "io.github.devngho:kirok:$kirokVersion")
}