package io.github.devngho.kirok

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import kotlin.reflect.KClass

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


data class DeclareTarget<T>(val declareFunc: T.() -> Unit)
data class FunctionDSL(val name: String, var block: FunSpec.Builder.() -> Unit, var returns: TypeName = Unit::class.asClassName())

//@Suppress("SpellCheckingInspection")
//infix fun String.kclass(block: TypeSpec.Builder.() -> Unit): DeclareTarget<FileSpec.Builder> = DeclareTarget { clazz(this@kclass, block) }

@Suppress("SpellCheckingInspection")
infix fun String.kfun(block: FunSpec.Builder.() -> Unit) = FunctionDSL(this, block)

infix fun String.parameter(type: TypeName): DeclareTarget<FunSpec.Builder> = DeclareTarget { addParameter(this@parameter, type) }
infix fun String.parameter(type: KClass<*>): DeclareTarget<FunSpec.Builder> = DeclareTarget { addParameter(this@parameter, type) }
infix fun String.parameter(type: KSClassDeclaration): DeclareTarget<FunSpec.Builder> = DeclareTarget { addParameter(this@parameter, type.toClassName()) }

infix fun FunctionDSL.returns(type: KClass<*>) = this.copy(returns = type.asClassName())
//infix fun FunctionDSL.returns(type: KSType) = this.copy(returns = type.toClassName())
infix fun FunctionDSL.returns(type: KSClassDeclaration) = this.copy(returns = type.toClassName())
infix fun FunctionDSL.returns(type: TypeName) = this.copy(returns = type)

infix fun String.import(name: String): DeclareTarget<FileSpec.Builder> = DeclareTarget { addImport(this@import, name) }
infix fun String.annotate(name: String): DeclareTarget<FunSpec.Builder> = DeclareTarget { annotate(this@annotate, name) }

infix fun <T> DeclareTarget<T>.declareTo(target: T) = this.declareFunc(target)
infix fun FunctionDSL.declareTo(target: FileSpec.Builder) = target.func(this.name) { this@declareTo.block(this); if (this@declareTo.returns != Unit::class) returns(this@declareTo.returns); }


val jsExport = ClassName("kotlin.js", "ExperimentalJsExport")
//val experimentalSerializationApi = ClassName("kotlinx.serialization", "ExperimentalSerializationApi")
//val experimentalEncodingApi = ClassName("kotlin.io.encoding", "ExperimentalEncodingApi")
val experimentalStdlibApi = ClassName("kotlin", "ExperimentalStdlibApi")
val delicateCoroutinesApi = ClassName("kotlinx.coroutines", "DelicateCoroutinesApi")