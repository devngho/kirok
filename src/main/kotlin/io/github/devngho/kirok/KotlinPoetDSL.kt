package io.github.devngho.kirok

import com.squareup.kotlinpoet.*

fun fileSpec(packageName: String, fileName: String, block: FileSpec.Builder.() -> Unit): FileSpec {
    val builder = FileSpec.builder(packageName, fileName)
    builder.block()
    return builder.build()
}

fun FileSpec.Builder.clazz(name: String, block: TypeSpec.Builder.() -> Unit): TypeSpec {
    val builder = TypeSpec.classBuilder(name)
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

fun TypeSpec.Builder.func(name: String, block: FunSpec.Builder.() -> Unit) {
    val builder = FunSpec.builder(name)
    builder.block()
    val clazz = builder.build()
    addFunction(clazz)
}

fun FunSpec.Builder.annotate(vararg annotations: AnnotationSpec): FunSpec.Builder {
    annotations.forEach {
        addAnnotation(it)
    }
    return this
}

fun FunSpec.Builder.annotate(packageName: String, name: String): FunSpec.Builder {
    addAnnotation(ClassName(packageName, name))
    return this
}

fun annotation(packageName: String, name: String): AnnotationSpec {
    return AnnotationSpec.builder(
        ClassName(packageName, name)
    ).build()
}