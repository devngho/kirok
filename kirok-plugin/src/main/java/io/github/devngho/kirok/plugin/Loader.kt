package io.github.devngho.kirok.plugin

import com.google.gson.Gson
import org.gradle.api.Project
import java.io.File
import java.net.URL
import java.net.URLClassLoader

object Loader {
    @Suppress("unchecked_cast")
    fun <T> getClass(target: Project, className: String): Class<T>? {
        when(className) {
            "kotlin.Int" -> return Int::class.java as Class<T>
            "kotlin.String" -> return String::class.java as Class<T>
            "kotlin.Boolean" -> return Boolean::class.java as Class<T>
            "kotlin.Double" -> return Double::class.java as Class<T>
            "kotlin.Float" -> return Float::class.java as Class<T>
            "kotlin.Long" -> return Long::class.java as Class<T>
            "kotlin.Short" -> return Short::class.java as Class<T>
            "kotlin.Byte" -> return Byte::class.java as Class<T>
            "kotlin.Char" -> return Char::class.java as Class<T>
            "kotlin.collections.List" -> return List::class.java as Class<T>
            "kotlin.collections.Map" -> return Map::class.java as Class<T>
            "kotlin.collections.Set" -> return Set::class.java as Class<T>
            "kotlin.collections.Collection" -> return Collection::class.java as Class<T>
            "kotlin.collections.MutableList" -> return MutableList::class.java as Class<T>
            "kotlin.collections.MutableMap" -> return MutableMap::class.java as Class<T>
            "kotlin.collections.MutableSet" -> return MutableSet::class.java as Class<T>
            "kotlin.collections.MutableCollection" -> return MutableCollection::class.java as Class<T>
        }

        try {
            return Class.forName(className) as Class<T>
        } catch (_: Exception) {}

        try {
            return Thread.currentThread().contextClassLoader.loadClass(className) as Class<T>
        } catch (_: Exception) {}

        val files = target.configurations.getAt("jvmCompileClasspath").plus(File("${target.projectDir}/build/libs").listFiles()!!.filter { it.extension == "jar" && !it.name.contains("metadata") })

//        target.logger.warn("try load class $className from \n  - ${files.joinToString("\n  - ") { it.absolutePath }}.")

        files.forEach {
            try {
//                target.logger.warn("try load class $className from ${it.absolutePath}.")
                val child = URLClassLoader(
                    arrayOf<URL>(it.toURI().toURL()),
                    this.javaClass.getClassLoader()
                )
                return (try { child.loadClass(className) } catch (_: Exception) {  } ?: Class.forName(className, true, child)) as Class<T>
            } catch (_: Exception) {}
        }

        target.logger.warn("failed to load class $className from ${files.count()} jar files.")
        target.logger.warn("kirok has tried to load class $className from \n  - ${files.joinToString("\n  - ") { it.absolutePath }}.")

        return null
    }

    @Suppress("unchecked_cast")
    fun getModelData(target: Project):  Map<String, Map<String, Map<String, Any>>> =
        Gson().fromJson(File("${target.projectDir}/build/generated/ksp/wasm/wasmMain/resources/kirok_model.json").readText(), Map::class.java)
            as Map<String, Map<String, Map<String, String>>>
}