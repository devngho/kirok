package io.github.devngho.kirok

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

object KirokModel {
    private fun FileSpec.Builder.saveFunction(name: String, model: KSClassDeclaration) = name kfun {
        "kotlinx.serialization" import "encodeToString" declareTo this@saveFunction
        "kotlinx.serialization.json" import "Json" declareTo this@saveFunction
        optIn(experimentalStdlibApi)

        "model" parameter model declareTo this

        addCode("return Json.encodeToString(model)\n")
    } returns String::class declareTo this

    private fun FileSpec.Builder.loadFunction(name: String, model: KSClassDeclaration) = name kfun  {
        "kotlinx.serialization" import "decodeFromString" declareTo this@loadFunction
        "kotlinx.serialization.json" import  "Json"  declareTo this@loadFunction
        optIn(experimentalStdlibApi)

        "model" parameter String::class declareTo this

        addCode(
                "return Json.decodeFromString<${model.simpleName.asString()}>(model)\n"
            )
    } returns model declareTo this

    private fun resolveIntents(resolver: Resolver, model: KSClassDeclaration) = resolver
        .getSymbolsWithAnnotation("io.github.devngho.kirok.Intent")
        .filterIsInstance<KSFunctionDeclaration>()
        .filter {
            it.parameters.first().type.resolve() == model.asStarProjectedType()
        }

    private fun resolveInitFunction(resolver: Resolver, model: KSClassDeclaration) = resolver
        .getSymbolsWithAnnotation("io.github.devngho.kirok.Init")
        .filterIsInstance<KSFunctionDeclaration>()
        .firstOrNull {
            it.returnType?.resolve() == model.asStarProjectedType()
        }

    private fun checkIntentUnique(intents: Sequence<KSFunctionDeclaration>, logger: KSPLogger) = intents
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

    fun generateModel(packageName: String, resolver: Resolver, model: KSClassDeclaration, logger: KSPLogger): FileSpec {
        val intents = resolveIntents(resolver, model)
        val init = resolveInitFunction(resolver, model)

        checkIntentUnique(intents, logger)

        return fileSpec(packageName, "${model.simpleName.asString()}Kirok") {
            val saveFunc = "save" + model.simpleName.asString()
            saveFunction(saveFunc, model)

            val loadFunc = "load" + model.simpleName.asString()
            loadFunction(loadFunc, model)

            "init" + model.simpleName.asString() kfun {
                if (init == null) {
                    logger.error(("Init function not found. Please define init function for ${model.simpleName.asString()} model, then add @Init annotation to init function."))
                    return@kfun
                }

                init.packageName.asString() import  init.simpleName.asString() declareTo this@fileSpec
                "kotlin.js" annotate  "JsExport" declareTo this

                optIn(jsExport)

                addCode(
                    "return $saveFunc(${init.simpleName.asString()}())\n"
                )
            } returns String::class declareTo this

            intents.forEach {
                val isSuspend = it.modifiers.any { a -> a == Modifier.SUSPEND }

                if (isSuspend) {
                    it.simpleName.asString() + model.simpleName.asString() kfun {
                        it.qualifiedName!!.getQualifier() import it.simpleName.asString() declareTo this@fileSpec
                        "kotlinx.coroutines" import "promise" declareTo this@fileSpec
                        "kotlinx.coroutines" import "GlobalScope" declareTo this@fileSpec

                        "model" parameter String::class declareTo this

                        it.parameters.subList(1, it.parameters.count()).forEachIndexed { i, p ->
                            "arg$i" parameter p.type.resolve().toClassName() declareTo this
                        }

                        "kotlin.js" annotate "JsExport" declareTo this
                        optIn(jsExport, delicateCoroutinesApi)

                        addCode("return GlobalScope.promise {\n")
//                        addCode("Promise { resolve, _ -> \n")
                        addCode("val nextModel = $loadFunc(model)\n")

                        if (it.parameters.count() == 1) addCode("${it.simpleName.asString()}(nextModel)\n")
                        else addCode(
                            "${it.simpleName.asString()}(nextModel${
                                List(
                                    it.parameters.subList(
                                        1,
                                        it.parameters.count()
                                    ).size
                                ) { i -> ", arg$i" }.joinToString("")
                            })\n"
                        )

//                        addCode("resolve($saveFunc(nextModel))\n")
//                        addCode("return@promise $saveFunc(nextModel)\n")
                        addCode("return@promise $saveFunc(nextModel).toJsString()\n")

                        this.addCode("}")
//                        this.addCode("} as Promise<%T?>", ClassName("kotlin.js", "JsString"))
                    } returns (ClassName("kotlin.js", "Promise").parameterizedBy(ClassName("kotlin.js", "JsAny").copy(nullable = true))) declareTo this
                } else {
                    it.simpleName.asString() + model.simpleName.asString() kfun {
                        it.qualifiedName!!.getQualifier() import it.simpleName.asString() declareTo this@fileSpec

                        "model" parameter String::class declareTo this

                        it.parameters.subList(1, it.parameters.count()).forEachIndexed { i, p ->
                            "arg$i" parameter p.type.resolve().toTypeName() declareTo this
                        }

                        "kotlin.js" annotate "JsExport" declareTo this
                        addCode("val nextModel = $loadFunc(model)\n")

                        if (it.parameters.count() == 1) addCode("${it.simpleName.asString()}(nextModel)\n")
                        else addCode(
                            "${it.simpleName.asString()}(nextModel${
                                List(
                                    it.parameters.subList(
                                        1,
                                        it.parameters.count()
                                    ).size
                                ) { i -> ", arg$i" }.joinToString("")
                            })\n"
                        )
                        addCode("return $saveFunc(nextModel)\n")

                    } returns String::class declareTo this
                }
            }
        }
    }
}