package io.github.devngho.kirok.plugin

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

            kotlinExtension.sourceSets.forEach {
                it.dependencies {
                    implementation("io.github.devngho:kirok:$kirokVersion")
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
                    implementation("org.jetbrains.kotlinx:atomicfu:0.27.0")
                }
            }

            extensions.create("kirok", KirokExtension::class.java)

            tasks.register("generateKirokBinding", GenerateKirokBinding::class.java) {
                it.dependsOn("copyBinding")
            }
            tasks.register("copyBinding", CopyBinding::class.java) {
                val dependTargets =
                    listOf("compileKotlinWasmJs", "compileProductionExecutableKotlinWasmJs", "compileProductionExecutableKotlinWasmJsOptimize")
                        .filter { t -> tasks.findByName(t) != null }
                it.dependsOn(dependTargets)

                it.inputJSDir.set(layout.projectDirectory.dir("build/compileSync/wasmJs/main"))
                it.outputJSDir.set(layout.projectDirectory.dir("build/generated/kirok"))
                it.inputWasmDir.set(layout.projectDirectory.dir("build/compileSync/wasmJs/main"))
                it.outputWasmDir.set(layout.projectDirectory.dir("build/generated/kirok"))
            }
            tasks.getByName("assemble").finalizedBy("generateKirokBinding")
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

    var neverUseNode = false
    var disableImportMap = true
}

@Suppress("unused")
fun DependencyHandler.kirok(project: Project) {
    val kirokVersion = project.plugins.getPlugin(KirokGradlePlugin::class.java).getVersion(project)

    add("kspWasmJs", "io.github.devngho:kirok-ksp:$kirokVersion")
    add("kspWasmJsTest", "io.github.devngho:kirok-ksp:$kirokVersion")
    add("kspJvm", "io.github.devngho:kirok-ksp:$kirokVersion")
    add("kspJvmTest", "io.github.devngho:kirok-ksp:$kirokVersion")
}