package io.github.devngho.kirok

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

object KirokModel {
    private fun FileSpec.Builder.saveFunction(name: String, model: KSClassDeclaration) = func(name) {
        addImport("kotlinx.serialization", "encodeToByteArray")
        addImport("kotlinx.serialization.protobuf", "ProtoBuf")
        addParameter("model", model.toClassName())
        optIn(experimentalSerializationApi, experimentalStdlibApi)
        returns(String::class)

        addCode("return ProtoBuf.encodeToByteArray(model).toHexString()\n")
    }

    private fun FileSpec.Builder.loadFunction(name: String, model: KSClassDeclaration) = func(name) {
        addParameter("model", String::class)
        addImport("kotlinx.serialization", "decodeFromByteArray")
        addImport("kotlinx.serialization.protobuf", "ProtoBuf")
        optIn(experimentalSerializationApi, experimentalStdlibApi)
        returns(model.toClassName())

        addCode(
            CodeBlock.of(
                "return ProtoBuf.decodeFromByteArray<${model.simpleName.asString()}>(model.hexToByteArray())\n"
            ))
    }

    fun generateModel(packageName: String, resolver: Resolver, model: KSClassDeclaration, logger: KSPLogger): FileSpec {
        val intents = resolver
            .getSymbolsWithAnnotation("io.github.devngho.kirok.Intent")
            .filterIsInstance<KSFunctionDeclaration>()
            .filter {
                it.parameters.first().type.resolve() == model.asStarProjectedType()
            }
        val init = resolver
            .getSymbolsWithAnnotation("io.github.devngho.kirok.Init")
            .filterIsInstance<KSFunctionDeclaration>()
            .firstOrNull {
                it.returnType?.resolve() == model.asStarProjectedType()
            }

        // check name of intents are unique
        intents
            .map { it.simpleName.asString() }
            .let {
                it.map { s ->
                    // count of name in list
                    s to it.count { c -> c == s }
                }
            }
            .filter { it.second != 1 }
            .forEach {
                logger.error("Intent name ${it.first} must be unique. Please rename the intent.")
            }

        val modelFile =
            fileSpec(packageName, "${model.simpleName.asString()}Kirok") {
                val saveFunc = "save" + model.simpleName.asString()
                saveFunction(saveFunc, model)

                val loadFunc = "load" + model.simpleName.asString()
                loadFunction(loadFunc, model)

                func("init" + model.simpleName.asString()) {
                    if (init == null) {
                        logger.error(("Init function not found. Please define init function for ${model.simpleName.asString()} model, then add @Init annotation to init function."))
                        return@func
                    }

                    addImport(init.packageName.asString(), init.simpleName.asString())
                    annotate("kotlin.js", "JsExport")
                    optIn(jsExport)
                    returns(String::class)
                    addCode(
                        CodeBlock.of(
                        "return $saveFunc(${init.simpleName.asString()}())\n"
                    ))
                }

                intents.forEach {
                    func(it.simpleName.asString() + model.simpleName.asString()) {
                        addImport(it.qualifiedName!!.getQualifier(), it.simpleName.asString())
                        addParameter("model", String::class)
                        it.parameters.subList(1, it.parameters.count()).forEachIndexed { i, p ->
                            addParameter("arg$i", p.type.resolve().toTypeName()) }
                        returns(String::class)
                        annotate("kotlin.js", "JsExport")
                        optIn(jsExport)
                        addCode("val nextModel = $loadFunc(model).copy()\n")
                        if (it.parameters.count() == 1) addCode("${it.simpleName.asString()}(nextModel)\n")
                        else addCode("${it.simpleName.asString()}(nextModel${List(it.parameters.subList(1, it.parameters.count()).size) { i -> ", arg$i" }.joinToString("")})\n")
                        addCode(
                            CodeBlock.of(
                            "return $saveFunc(nextModel)\n"
                        ))
                    }
                }
            }

        return modelFile
    }
}