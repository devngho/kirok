package io.github.devngho.kirok.plugin

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.gradle.api.Project
import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType

object Loader {
    private data class Result(var name: String, val sub: MutableList<Result>)

    private fun parseInput(input: String): Result {
        val stack = mutableListOf<Result>()
        var currentResult = Result("", mutableListOf())
        val currentName = StringBuilder()

        for (char in input) {
            when (char) {
                '<' -> {
                    stack.add(currentResult)
                    currentResult = Result(currentName.toString(), mutableListOf())
                    currentName.clear()
                }
                '>', ',' -> {
                    if (currentName.isNotEmpty()) {
                        currentResult.sub.add(Result(currentName.toString(), mutableListOf()))
                        currentName.clear()
                    }
                    if (char == '>') {
                        val lastResult = currentResult
                        currentResult = stack.removeAt(stack.size - 1)
                        currentResult.sub.add(lastResult)
                    }
                }
                ' ' -> {
                    // Ignore spaces
                }
                else -> {
                    currentName.append(char)
                }
            }
        }

        return currentResult
    }

    fun <T: Any> parseType(target: Project, string: String): KType? = parseTypeResult<T>(target, parseInput(string).sub.first().apply { target.logger.info("parsed type $string -> $this") })

    private fun <T: Any> parseTypeResult(target: Project, result: Result): KType? {
        val clazz = getClass<T>(target, result.name) ?: return null

        return if (result.sub.isEmpty()) {
            clazz.createType()
        } else {
            clazz.createType(
                result.sub.map {
                    KTypeProjection(
                        when (it.name) {
                            "in" -> KVariance.IN
                            "out" -> KVariance.OUT
                            else -> KVariance.INVARIANT
                        },
                        parseTypeResult<Any>(target, it)!!
                    )
                }
            )
        }
    }

    @Suppress("unchecked_cast")
    fun <T : Any> getClass(target: Project, className: String): KClass<T>? {
        when(className) {
            "kotlin.Int" -> Int::class.java as Class<T>
            "kotlin.String" -> String::class.java as Class<T>
            "kotlin.Boolean" -> Boolean::class.java as Class<T>
            "kotlin.Double" -> Double::class.java as Class<T>
            "kotlin.Float" -> Float::class.java as Class<T>
            "kotlin.Long" -> Long::class.java as Class<T>
            "kotlin.Short" -> Short::class.java as Class<T>
            "kotlin.Byte" -> Byte::class.java as Class<T>
            "kotlin.Char" -> Char::class.java as Class<T>
            "kotlin.collections.List" -> List::class.java as Class<T>
            "kotlin.collections.Map" -> Map::class.java as Class<T>
            "kotlin.collections.Set" -> Set::class.java as Class<T>
            "kotlin.collections.Collection" -> Collection::class.java as Class<T>
            "kotlin.collections.MutableList" -> MutableList::class.java as Class<T>
            "kotlin.collections.MutableMap" -> MutableMap::class.java as Class<T>
            "kotlin.collections.MutableSet" -> MutableSet::class.java as Class<T>
            "kotlin.collections.MutableCollection" -> MutableCollection::class.java as Class<T>
            "kotlin.Pair" -> Pair::class.java as Class<T>
            "kotlin.Triple" -> Triple::class.java as Class<T>
            else -> null
        }?.let { return it.kotlin }

        try {
            return (Class.forName(className) as Class<T>).kotlin
        } catch (_: Exception) {}

        try {
            return (Thread.currentThread().contextClassLoader.loadClass(className) as Class<T>).kotlin
        } catch (_: Exception) {}

        val files =
            target.configurations.getAt("jvmCompileClasspath") +
                (File("${target.projectDir}/build/libs").listFiles()!!.filter { it.extension == "jar" && !it.name.contains("metadata") })

        try {
            val child = URLClassLoader(
                files.map { it.absoluteFile.toURI().toURL() }.toTypedArray(),
                this::class.java.getClassLoader()
            )
            return ((try { child.loadClass(className) } catch (_: Exception) {  } ?: Class.forName(className, true, child)) as Class<T>).kotlin
        } catch (_: Exception) {}

        target.logger.warn("failed to load class $className from ${files.count()} jar files.")
        target.logger.warn("kirok has tried to load class $className from \n  - ${files.map { it.absoluteFile.toURI().toURL() }.joinToString("\n  - ")}.")

        return null
    }

    fun getModelData(target: Project) =
        Json.parseToJsonElement(File("${target.projectDir}/build/generated/ksp/wasmJs/wasmJsMain/resources/kirok_model.json").readText()).jsonObject
}