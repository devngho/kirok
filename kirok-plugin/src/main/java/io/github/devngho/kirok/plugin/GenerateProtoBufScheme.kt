package io.github.devngho.kirok.plugin

//import kotlinx.serialization.ExperimentalSerializationApi
//import kotlinx.serialization.InternalSerializationApi
//import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator
//import kotlinx.serialization.serializer
//import org.gradle.api.Project
//import org.gradle.api.Task
//import java.io.File
//
//object GenerateProtoBufScheme {
//    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
//    fun Task.generateProtoBufScheme(target: Project) {
//        doLast {
//            val modelClasses =
//                Loader.getModelData(target).keys.associateWith { Loader.getClass<Any>(target, it)!!.kotlin }
//
//            val generatorInstance = (Loader.getClass<ProtoBufSchemaGenerator>(target, "kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator"))!!.kotlin.objectInstance!!
//
//            modelClasses.forEach {
//                val schema = generatorInstance.generateSchemaText(it.value.serializer().descriptor)
//                target.logger.info(File("${target.projectDir}/build/generated/kirok/").mkdirs().toString())
//                val schemaFile = File("${target.projectDir}/build/generated/kirok/${it.key.split(".").last()}.proto")
//                schemaFile.createNewFile()
//                schemaFile.writeText(schema)
//            }
//        }
//    }
//}