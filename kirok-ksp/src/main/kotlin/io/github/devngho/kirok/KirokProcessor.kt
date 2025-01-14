package io.github.devngho.kirok

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getKotlinClassByName
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ksp.writeTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement

class KirokProcessor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger, options: Map<String, String>): SymbolProcessor {
    private val useLog = options["kirok.use_log"] == "true"

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (resolver.getDeclarationsFromPackage("kotlin.wasm").toList().isNotEmpty()) {
            if (useLog) logger.warn("KirokProcessor building wasm code")
            processWasm(resolver)
        }

        return emptyList()
    }

    override fun finish() {
        super.finish()
        if (useLog) logger.warn("KirokProcessor finished")
    }

    private fun processWasm(resolver: Resolver) {
        generateModels(resolver)
        buildModelJson(resolver)
//        preventBodyRemoving()
    }
    private fun resolveModels(resolver: Resolver): Sequence<KSClassDeclaration> {
        return resolver
            .getSymbolsWithAnnotation("io.github.devngho.kirok.Model")
            .filterIsInstance<KSClassDeclaration>()
            .filterNot { it.isExpect }
    }

    /**
     * @return package name
     */
    private fun generateModels(resolver: Resolver): String {
        var p = ""
        resolveModels(resolver)
            .forEach {
                if (useLog) logger.warn("Building model ${it.simpleName.asString()}")
                p = it.packageName .asString()  + "._kirok"
                val m = KirokModel.generateModel(p, resolver, it, logger)
                m.first.writeTo(codeGenerator, m.second)
            }

        return p
    }

    private fun buildModelJson(resolver: Resolver) {
        resolveModels(resolver)
            .map { it to it.getAllProperties().map { p -> p.simpleName.asString() to p.type.resolve().typeString() } }
            .run {
                try {
                    codeGenerator.createNewFile(
                        dependencies = Dependencies(true, *this.map { it.first.containingFile!! }.distinctBy { it.filePath }.toList().toTypedArray()),
                        packageName = "",
                        fileName = "kirok_model",
                        extensionName = "json"
                    ).bufferedWriter().use {
                        it.write(buildModelJson(resolver))
                    }
                } catch (_: Exception) {}
            }
    }

    @Serializable
    data class ModelData(
        val values: Map<String, String>,
        val init: Map<String, String>,
        val isInitSuspend: Boolean,
        val intents: Map<String, Map<String, String>>,
    )

    private val json = Json { prettyPrint = true }

    private fun  List<KSValueParameter>.toMap(): Map<String, String> = this.associate { it.name!!.asString() to it.type.resolve().typeString() }

    private fun Sequence<Pair<KSClassDeclaration, Sequence<Pair<String, String>>>>
            .buildModelJson(resolver: Resolver): String {
        val jsonElement = buildJsonObject {
            this@buildModelJson.forEachIndexed { _, outerPair ->
                val initFunction = KirokModel.resolveInitFunction(resolver, outerPair.first)
                val intents = KirokModel.resolveIntents(resolver, outerPair.first)
                val methodIntents = KirokModel.resolveMethodIntents(resolver, outerPair.first)

                put(outerPair.first.qualifiedName?.asString()!!,
                    Json.encodeToJsonElement(ModelData(
                        outerPair.second.map { it.first to it.second }.toMap(),
                        initFunction!!.parameters.toMap(),
                        initFunction.modifiers.contains(Modifier.SUSPEND) || ((initFunction.firstAnnotation("io.github.devngho.kirok.Init")?.first<Boolean>()) == true),
                        intents.associate { intent ->
                            val isSuspend = intent.modifiers.contains(Modifier.SUSPEND) || ((intent.firstAnnotation("io.github.devngho.kirok.Intent")?.first<Boolean>()) == true)

                            (if (isSuspend) "SUSPEND_" else "") + intent.simpleName.asString() to
                                    intent.parameters.toMap()
                        } + methodIntents.associate { intent ->
                            val isSuspend = intent.modifiers.contains(Modifier.SUSPEND) || ((intent.firstAnnotation("io.github.devngho.kirok.Intent")?.first<Boolean>()) == true)

                            (if (isSuspend) "SUSPEND_" else "") + intent.simpleName.asString() to
                                    mapOf("model" to outerPair.first.asStarProjectedType().typeString()) + intent.parameters.toMap()
                        }
                    ))
                )
            }
        }

        return json.encodeToString(jsonElement)
    }

//    private fun preventBodyRemoving() {
//        try {
//            fileSpec("", "__Kirok__") {
//                addImport("kotlinx.browser", "document")
//                "__kirok_get_body__" kfun {
//                    addKdoc("NEVER USE IT!\nThis function is generated for some reason.\nKotlin compiler generates js file including javascript features if it used.\nThis function is used to prevent it.\n")
//                    annotate("kotlin.js", "JsExport")
//                    optIn(jsExport)
//                    "return document.body?.innerHTML"()
//                } returns ClassName("kotlin", "String").copy(nullable = true)
//                "__kirok_set_body__" kfun {
//                    addKdoc("NEVER USE IT!\nThis function is generated for some reason.\nKotlin compiler generates js file including javascript features if it used.\nThis function is used to prevent it.\n")
//                    annotate("kotlin.js", "JsExport")
//                    optIn(jsExport)
//                    "document.body?.innerHTML = \n\"NEVER USE __kirok_set_body__ function. See comments in generated code by ksp.\""()
//                }
//            }.writeTo(codeGenerator, Dependencies(false))
//        } catch (_: Exception) {}
//    }

    private fun KSType.typeString(): String = this.qualifier() + this.arguments.joinToString(separator = ", ", prefix = "<", postfix = ">") { "${it.variance.label} ${it.type!!.resolve().typeString()}".trim() }
    private fun KSType.qualifier(): String {
        val isSubclass = this.declaration.parentDeclaration is KSClassDeclaration
        return if (isSubclass) {
            val parent = this.declaration.parentDeclaration as KSClassDeclaration
            parent.asStarProjectedType().qualifier() + "$" + this.declaration.simpleName.asString()
        } else {
            this.declaration.qualifiedName!!.asString()
        }
    }
}

class KirokProcessorProvider: SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return KirokProcessor(environment.codeGenerator, environment.logger, environment.options)
    }
}