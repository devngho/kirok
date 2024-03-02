package io.github.devngho.kirok

import com.google.devtools.ksp.symbol.*
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

fun annotation(packageName: String, name: String): AnnotationSpec {
    return AnnotationSpec.builder(
        ClassName(packageName, name)
    ).build()
}


interface DeclareTarget<T> {
    val declareFunc: T.() -> Unit
}

data class DeclareTargetImpl<T>(override val declareFunc: T.() -> Unit): DeclareTarget<T>

data class FunctionDSL(val name: String, var block: FunctionBodyDSL.() -> Unit, var returns: TypeName = Unit::class.asClassName(), var modifiers: MutableList<KModifier> = mutableListOf())

class FunctionBodyDSL(name: String) {
    sealed interface FunctionControlFlowDSL {
        data class If(val condition: String): FunctionControlFlowDSL
        data class Other(val statement: String): FunctionControlFlowDSL
    }

    private val builder = FunSpec.builder(name)

    operator fun String.invoke(vararg args: Any) {
        builder.addStatement(this, *args)
    }

    fun statement(statement: String, vararg args: Any) {
        builder.addStatement(statement, *args)
    }

    fun addKdoc(block: String) {
        builder.addKdoc(block)
    }

    fun kIf(condition: String, block: FunctionBodyDSL.() -> Unit): FunctionControlFlowDSL.If {
        builder.beginControlFlow("if ($condition)")
        block()

        return FunctionControlFlowDSL.If(condition)
    }

    fun FunctionControlFlowDSL.If.kElseIf(condition: String, block: FunctionBodyDSL.() -> Unit): FunctionControlFlowDSL.If {
        builder.nextControlFlow("else if ($condition)")
        block()
        return this
    }

    fun FunctionControlFlowDSL.If.kElse(block: FunctionBodyDSL.() -> Unit): FunctionControlFlowDSL.If {
        builder.nextControlFlow("else")
        block()
        return this
    }

    fun kControlFlow(controlFlow: String, block: FunctionBodyDSL.() -> Unit): FunctionControlFlowDSL.Other {
        builder.beginControlFlow(controlFlow)
        block()
        return FunctionControlFlowDSL.Other(controlFlow)
    }

    fun annotate(packageName: String, name: String) {
        builder.addAnnotation(annotation(packageName, name))
    }

    fun annotateWith(packageName: String, name: String, block: AnnotationSpec.Builder.() -> Unit) {
        builder.addAnnotation(annotation(packageName, name).toBuilder().apply(block).build())
    }

    fun optIn(vararg names: ClassName) {
         builder.addAnnotation(annotation("kotlin", "OptIn").toBuilder().addMember(names.joinToString(separator = ",") { "%T::class" }, *names).build())
    }

    fun optIn(vararg names: String) {
        builder.addAnnotation(annotation("kotlin", "OptIn").toBuilder().addMember(names.joinToString(separator = ",") { "%T::class" }, *names).build())
    }

    infix fun String.parameter(type: TypeName) = builder.addParameter(this, type)
    infix fun String.parameter(type: KClass<*>) = builder.addParameter(this, type)
    infix fun String.parameter(type: KSClassDeclaration) = builder.addParameter(this, type.toClassName())

    operator fun FunctionControlFlowDSL.invoke() {
        builder.endControlFlow()
    }

    fun done() = builder
}

@Suppress("SpellCheckingInspection")
infix fun String.kobj(block: TypeSpec.Builder.() -> Unit): DeclareTarget<FileSpec.Builder> = DeclareTargetImpl { obj(this@kobj, block) }

@Suppress("SpellCheckingInspection")
infix fun String.kfun(block: FunctionBodyDSL.() -> Unit) = FunctionDSL(this, block)

infix fun FunctionDSL.returns(type: KClass<*>) = this.copy(returns = type.asClassName())
//infix fun FunctionDSL.returns(type: KSType) = this.copy(returns = type.toClassName())
infix fun FunctionDSL.returns(type: KSClassDeclaration) = this.copy(returns = type.toClassName())
infix fun FunctionDSL.returns(type: KSType) = this.copy(returns = type.toClassName())
infix fun FunctionDSL.returns(type: TypeName) = this.copy(returns = type)

infix fun FunctionDSL.modifier(modifier: KModifier) = this.apply { this.modifiers.add(modifier) }

infix fun String.import(name: String): DeclareTarget<FileSpec.Builder> =
    DeclareTargetImpl { addImport(this@import, name) }
infix fun String.annotate(name: String): DeclareTarget<FunctionBodyDSL> =
    DeclareTargetImpl { annotate(this@annotate, name) }

infix fun <T> DeclareTarget<T>.declareTo(target: T) = this.declareFunc(target)
infix fun FunctionDSL.declareTo(target: FileSpec.Builder) =
    FunctionBodyDSL(this@declareTo.name)
        .apply(block)
        .done()
        .apply {
            if (this@declareTo.returns != Unit::class) returns(this@declareTo.returns)
        }
        .build()
        .also { target.addFunction(it) }

infix fun FunctionDSL.declareTo(target: TypeSpec.Builder) =
    FunctionBodyDSL(this@declareTo.name)
        .apply(block)
        .done()
        .apply {
            if (this@declareTo.returns != Unit::class) returns(this@declareTo.returns)
            addModifiers(this@declareTo.modifiers)
        }
        .build()
        .apply { target.addFunction(this)  }

infix fun DeclareTarget<TypeSpec.Builder>.declareTo(target: TypeSpec.Builder) = this.declareFunc(target)

fun KSFunctionDeclaration.toMemberName() =
    if (this.parentDeclaration != null) {
        val parent = this.parentDeclaration as KSClassDeclaration
        MemberName(parent.qualifiedName!!.asString(), this.simpleName.asString())
    } else {
        MemberName(this.packageName.asString(), this.simpleName.asString())
    }

fun KClass<*>.toClassName() = ClassName(this.java.`package`.name, this.simpleName!!)

fun KSDeclaration.firstAnnotation(qualifiedName: String) = this.annotations.firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == qualifiedName }
inline fun <reified T: Any> KSAnnotation.first(): T? = this.arguments.firstOrNull { it.value is T }?.value as T?


val jsExport = ClassName("kotlin.js", "ExperimentalJsExport")
//val experimentalSerializationApi = ClassName("kotlinx.serialization", "ExperimentalSerializationApi")
//val experimentalEncodingApi = ClassName("kotlin.io.encoding", "ExperimentalEncodingApi")
val experimentalStdlibApi = ClassName("kotlin", "ExperimentalStdlibApi")
val delicateCoroutinesApi = ClassName("kotlinx.coroutines", "DelicateCoroutinesApi")