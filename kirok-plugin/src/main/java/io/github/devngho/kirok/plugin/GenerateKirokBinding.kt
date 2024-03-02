package io.github.devngho.kirok.plugin

import io.github.devngho.kirok.binding.Binding
import io.github.devngho.kirok.binding.BindingModel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Path
import kotlin.reflect.full.primaryConstructor


abstract class GenerateKirokBinding: DefaultTask() {
    @TaskAction
    fun generateKirokBinding() {
        val binding = project.extensions.getByName("kirok") as KirokExtension
        val bindingList = binding.binding.map {
            getBinding(project, it)
        }.filterIsInstance<Binding>()

        listOf(File(binding.wasmDir), File(binding.bindingDir)).forEach { if (!it.exists()) it.mkdirs() }

        val models = Loader.getModelData(project)

        runBlocking {
            // Parse serialized model and its intent
            bindingList.forEach { b ->
                b.create(Path.of(project.projectDir.path, binding.bindingDir), models.map { (k, v) ->
                    val model = v.jsonObject

                    BindingModel(k,
                        model["values"]!!.jsonObject.map { (k, m) ->
                            k to Loader.parseType<Any>(project, m.jsonPrimitive.content)!!
                        }.toMap(),
                        model["intents"]!!.jsonObject.mapValues { (_, intent) ->
                            intent.jsonObject.mapValues { (_, type) -> Loader.parseType<Any>(project, type.jsonPrimitive.content)!! }
                        },
                        model["init"]!!.jsonObject.mapValues {
                            Loader.parseType<Any>(project, it.value.jsonPrimitive.content)!!
                        },
                        model["isInitSuspend"]!!.jsonPrimitive.boolean
                    )
                })
            }
        }
    }

    private fun getBinding(target: Project, className: String): Binding? {
        return Loader.getClass<Binding>(target, className)?.primaryConstructor?.call()
    }
}