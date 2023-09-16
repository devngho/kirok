package io.github.devngho.kirok.plugin

import io.github.devngho.kirok.binding.Binding
import kotlinx.coroutines.runBlocking
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File
import java.nio.file.Path


object GenerateKirokBinding {
    fun Task.generateKirokBinding(target: Project) {
        doLast {
            val binding = target.extensions.getByName("kirok") as KirokExtension
            val bindingList = binding.binding.map {
                getBinding(target, it)
            }.filterIsInstance<Binding>()

            listOf(File(binding.wasmDir), File(binding.bindingDir)).forEach { if (!it.exists()) it.mkdirs() }

            val model = Loader.getModelData(target)

            runBlocking {
                bindingList.forEach { b ->
                    b.create(Path.of(target.projectDir.path, binding.bindingDir), model.map { (k, v) ->
                        Binding.BindingModel(k,
                            v["values"]!!.map { (k, _) ->
                                k to /* getClass<Any>(target, v)!!.kotlin */ Any::class
                            }.toMap(),
                            v["intents"]!!.mapNotNull { (k, v) ->
                                if (v is List<*>) k to v.map { /* getClass<Any>(target, v)!!.kotlin */ Any::class } else null
                            }.toMap(),
                            "${target.projectDir}/build/generated/kirok/$k.proto"
                        )
                    })
                }
            }

            try {
                File("${target.projectDir}/build/compileSync/wasm/main")
                    .listFiles()!!.first().listFiles()!!.first().listFiles()?.find { it.extension == "wasm" }
                    ?.copyTo(File(binding.wasmDir, "index.wasm"))
            } catch (_: Exception) {}

            try {
                File("${target.projectDir}/build/compileSync/wasm/main")
                    .listFiles()!!.first().listFiles()!!.first().listFiles()?.filter { it.extension.contains("js") }
                    ?.forEach {
                        val copyTarget = File(binding.wasmJsDir, it.name.replace("${project.name}-wasm", "index"))
                        copyTarget.writeText(it.readText().replace("${project.name}-wasm", "index"))
                    }
            } catch (_: Exception) {}
        }
    }

    private fun getBinding(target: Project, className: String): Binding? {
        return Loader.getClass<Binding>(target, className)?.getDeclaredConstructor()?.newInstance()
    }
}