package io.github.devngho.kirok.plugin

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File


abstract class CopyBinding: DefaultTask() {
    @get:InputDirectory
    @get:Incremental
    abstract val inputJSDir: DirectoryProperty

    @get:OutputDirectory
    @get:Optional
    abstract val outputJSDir: DirectoryProperty

    @get:InputDirectory
    abstract val inputWasmDir: DirectoryProperty

    @get:OutputDirectory
    @get:Optional
    abstract val outputWasmDir: DirectoryProperty

    @TaskAction
    fun generateKirokBinding(inputChanges: InputChanges) {
        val binding = project.extensions.getByName("kirok") as KirokExtension

        inputChanges.getFileChanges(inputJSDir).forEach { change ->
            val f = change.file
            val out =
                if (outputJSDir.asFile.get().absolutePath ==
                    project.layout.projectDirectory.dir("build/generated/kirok").asFile.absolutePath) // is default, binding overrides
                    File(binding.wasmJsDir)
                else File(outputJSDir.asFile.get().absolutePath)

            if (change.changeType != ChangeType.REMOVED) try {
                val copyTarget = File(out, f.name.replace("${project.name}-wasm-js", "index"))

                copyTarget.writeText(
                    f.readText()
                        .replace("${project.name}-wasm-js", "index") // File name swap
                        .replace("./index.wasm", "/index.wasm")  // wasm file path swap
                        .run {
                            if (binding.neverUseNode) replace(
                                Regex("if \\(isNodeJs\\) \\{.+?\\}", RegexOption.DOT_MATCHES_ALL),
                                "if (isNodeJs) { /* REMOVED BY KIROK TO SUPPORT NON-NODE ENVIRONMENT(neverUseNode options), \n SEE https://kirok.nghodev.com/docs/gradle-options */}"
                            ) // Delete nodejs dep
                            else this
                        }
                        .run {
                            if (binding.disableImportMap) replace(Regex("[(]await _importModule[(]'(.+?)'[)][)]")) { m ->
                                "/* EDITED BY KIROK (disableImportMap options) */ await import('${m.groups[1]!!.value}')"
                            } // Disable import map
                            else this
                        }
                )
            } catch (_: Exception) { }
        }

        val out =
            if (outputWasmDir.asFile.get().absolutePath ==
                project.layout.projectDirectory.dir("build/generated/kirok").asFile.absolutePath)
                File(binding.wasmDir)
            else File(outputWasmDir.asFile.get().absolutePath)

        try {
            val targetWasm = File(out, "index.wasm")
            if (targetWasm.exists()) targetWasm.delete()
            runBlocking {
                delay(500L)
                inputWasmDir.asFile.get().walk().find { f -> f.extension == "wasm" }!!.copyTo(targetWasm, overwrite = true)
            }
        } catch (_: Exception) {}
    }
}