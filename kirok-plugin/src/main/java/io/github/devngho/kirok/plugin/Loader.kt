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
        }

        try {
            return Class.forName(className) as Class<T>
        } catch (_: Exception) {}

        try {
            return Thread.currentThread().contextClassLoader.loadClass(className) as Class<T>
        } catch (_: Exception) {}

        val files = target.configurations.getAt("jvmCompileClasspath").plus(File("${target.projectDir}/build/libs").listFiles()!!.filter { it.extension == "jar" && !it.name.contains("metadata") })

        target.logger.warn("try load class $className from \n  - ${files.joinToString("\n  - ") { it.absolutePath }}.")

        files.forEach {
            try {
                target.logger.warn("try load class $className from ${it.absolutePath}.")
                val child = URLClassLoader(
                    arrayOf<URL>(it.toURI().toURL()),
                    this.javaClass.getClassLoader()
                )
                return Class.forName(className, true, child) as Class<T>
            } catch (_: Exception) {}
        }

        return null
    }

    @Suppress("unchecked_cast")
    fun getModelData(target: Project):  Map<String, Map<String, Map<String, Any>>> =
        Gson().fromJson(File("${target.projectDir}/build/generated/ksp/wasm/wasmMain/resources/kirok_model.json").readText(), Map::class.java)
            as Map<String, Map<String, Map<String, String>>>
}