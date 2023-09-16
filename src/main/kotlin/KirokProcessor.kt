import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import io.github.devngho.kirok.*

class KirokProcessor(val codeGenerator: CodeGenerator, val logger: KSPLogger, val options: Map<String, String>): SymbolProcessor {
    val useLog = options["kirok.use_log"] == "true"

    override fun process(resolver: Resolver): List<KSAnnotated> {
        var p: String

        resolver
            .getSymbolsWithAnnotation("io.github.devngho.kirok.Model")
            .filterIsInstance<KSClassDeclaration>()
            .forEach {
                if (useLog) logger.warn("Building model ${it.simpleName.asString()}")
                p = it.packageName .asString()  + "._kirok"
                KirokModel.generateModel(p, resolver, it, logger).writeTo(codeGenerator, Dependencies(true))
            }

        resolver
            .getSymbolsWithAnnotation("io.github.devngho.kirok.Model")
            .filterIsInstance<KSClassDeclaration>()
            .map { it to it.getAllProperties().map { p -> p.simpleName.asString() to p.type.resolve().toClassName().canonicalName } }
            .run {
                val stringBuilder = StringBuilder("{\n")

                this.forEachIndexed { outerIndex, outerPair ->
                    val intents = resolver
                        .getSymbolsWithAnnotation("io.github.devngho.kirok.Intent")
                        .filterIsInstance<KSFunctionDeclaration>()
                        .filter {
                            it.parameters.first().type.resolve().toClassName().canonicalName == outerPair.first.toClassName().canonicalName
                        }

                    stringBuilder.append("  \"${outerPair.first}\": {\n")
                    stringBuilder.append("        \"values\": {\n")
                    outerPair.second.forEachIndexed { index, innerPair ->
                        stringBuilder.append("    \"${innerPair.first}\": \"${innerPair.second}\"")
                        if (index != outerPair.second.count() - 1) {
                            stringBuilder.append(",\n")
                        } else {
                            stringBuilder.append("\n")
                        }
                    }
                    stringBuilder.append("    },")
                    stringBuilder.append("    \"intents\": {\n")
                    intents.forEachIndexed { index, intent ->
                        stringBuilder.append("      \"${intent.simpleName.asString()}\": [${intent.parameters.joinToString(separator = ",") { "\"${it.type.resolve().toClassName().canonicalName}\"" }}]\n")
                        if (index != intents.count() - 1) {
                            stringBuilder.append(",\n")
                        } else {
                            stringBuilder.append("\n")
                        }
                    }
                    stringBuilder.append("    }")
                    stringBuilder.append("  }")
                    if (outerIndex != this.count() - 1) {
                        stringBuilder.append(",\n")
                    } else {
                        stringBuilder.append("\n")
                    }
                }

                stringBuilder.append("}")

                try {
                    codeGenerator.createNewFile(
                        dependencies = Dependencies(false),
                        packageName = "",
                        fileName = "kirok_model",
                        extensionName = "json"
                    ).bufferedWriter().use {
                        it.write(stringBuilder.toString())
                    }
                } catch (_: Exception) {}
            }

        try {
            fileSpec("", "__Kirok__") {
                addImport("kotlinx.browser", "document")
                func("__kirok_get_body__") {
                    addKdoc("NEVER USE IT!\nThis function is generated for some reason.\nKotlin compiler generates js file including javascript features if it used.\nThis function is used to prevent it.\n")
                    annotate("kotlin.js", "JsExport")
                    optIn(jsExport)
                    returns(ClassName("kotlin", "String").copy(nullable = true))
                    addCode("return document.body?.innerHTML")
                }
                func("__kirok_set_body__") {
                    addKdoc("NEVER USE IT!\nThis function is generated for some reason.\nKotlin compiler generates js file including javascript features if it used.\nThis function is used to prevent it.\n")
                    annotate("kotlin.js", "JsExport")
                    optIn(jsExport)
                    addCode("document.body?.innerHTML = \n\"NEVER USE __kirok_set_body__ function. See comments in generated code by ksp.\"")
                }
            }.writeTo(codeGenerator, Dependencies(true))
        } catch (_: Exception) {}

        return emptyList()
    }

    override fun finish() {
        super.finish()
        if (useLog) logger.warn("KirokProcessor finished")
    }
}

class KirokProcessorProviderWasm: SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return KirokProcessor(environment.codeGenerator, environment.logger, environment.options)
    }
}