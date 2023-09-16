package io.github.devngho.kirok

import com.squareup.kotlinpoet.*

fun fileSpec(packageName: String, fileName: String, block: FileSpec.Builder.() -> Unit): FileSpec {
    val builder = FileSpec.builder(packageName, fileName)
    builder.block()
    return builder.build()
}

//fun FileSpec.Builder.clazz(name: String, block: TypeSpec.Builder.() -> Unit): TypeSpec {
//    val builder = TypeSpec.classBuilder(name)
//    builder.block()
//    val clazz = builder.build()
//    addType(clazz)
//    return clazz
//}

fun FileSpec.Builder.obj(name: String, block: TypeSpec.Builder.() -> Unit): TypeSpec {
    val builder = TypeSpec.objectBuilder(name)
    builder.block()
    val clazz = builder.build()
    addType(clazz)
    return clazz
}

fun FileSpec.Builder.func(name: String, block: FunSpec.Builder.() -> Unit) {
    val builder = FunSpec.builder(name)
    builder.block()
    val clazz = builder.build()
    addFunction(clazz)
}

//fun TypeSpec.Builder.func(name: String, block: FunSpec.Builder.() -> Unit) {
//    val builder = FunSpec.builder(name)
//    builder.block()
//    val clazz = builder.build()
//    addFunction(clazz)
//}

//fun FunSpec.Builder.annotate(vararg annotations: AnnotationSpec): FunSpec.Builder {
//    annotations.forEach {
//        addAnnotation(it)
//    }
//    return this
//}

fun FunSpec.Builder.annotate(packageName: String, name: String): FunSpec.Builder {
    addAnnotation(ClassName(packageName, name))
    return this
}

fun annotation(packageName: String, name: String): AnnotationSpec {
    return AnnotationSpec.builder(
        ClassName(packageName, name)
    ).build()
}

fun FunSpec.Builder.optIn(vararg names: ClassName): FunSpec.Builder {
    addAnnotation(annotation("kotlin", "OptIn").toBuilder().addMember(names.joinToString(separator = ",") { "%T::class" }, *names).build())
    return this
}

val jsExport = ClassName("kotlin.js", "ExperimentalJsExport")
val experimentalSerializationApi = ClassName("kotlinx.serialization", "ExperimentalSerializationApi")
//val experimentalEncodingApi = ClassName("kotlin.io.encoding", "ExperimentalEncodingApi")
val experimentalStdlibApi = ClassName("kotlin", "ExperimentalStdlibApi")