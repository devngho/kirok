package io.github.devngho.kirok

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getKotlinClassByName
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object KirokModel {
    private val json = ClassName("kotlinx.serialization.json", "Json")
    private fun FileSpec.Builder.saveFunction(name: String, model: KSClassDeclaration) = name kfun {
        "model" parameter model

        "return %T.%M(model)"(json, MemberName("kotlinx.serialization", "encodeToString"))
    } returns String::class declareTo this

    private fun FileSpec.Builder.loadFunction(name: String, model: KSClassDeclaration): FunSpec = name kfun  {
        "model" parameter String::class

        "return %T.%M<%T>(model)"(json, MemberName("kotlinx.serialization", "decodeFromString"), model.toClassName())

    } returns model declareTo this

    /**
     * This function finds all intents in the project which has no parent declaration.
     *
     * Example:
     * ```
     * fun onUserInput(model: Model, input: String): Model
     *    model.input = input
     *    return model
     *    // ...
     * }
     * ```
     *
     * @return Sequence of KSFunctionDeclaration
     */
    fun resolveIntents(resolver: Resolver, model: KSClassDeclaration) = resolver
        .getSymbolsWithAnnotation("io.github.devngho.kirok.Intent")
        .filterIsInstance<KSFunctionDeclaration>()
        .filter {
            (it.parameters.firstOrNull() ?: return@filter false).type.resolve() == model.asStarProjectedType()
                    && it.parentDeclaration == null
        }

    /**
     * This function finds all intents in the project which has parent declaration or extension receiver.
     *
     * Example:
     * ```
     * // Extension receiver
     * fun Model.onUserInput(input: String): Model = this
     *
     * // Parent declaration
     * data class Model(var input: String0 {
     *    fun onUserInput(newInput: String) {
     *      input = newInput
     *    }
     *    // ...
     * }
     * ```
     */
    fun resolveMethodIntents(resolver: Resolver, model: KSClassDeclaration) =
        (resolver
            .getSymbolsWithAnnotation("io.github.devngho.kirok.Intent")
            .filterIsInstance<KSFunctionDeclaration>()
            .filter {
                it.parentDeclaration?.qualifiedName?.asString() == model.qualifiedName?.asString()
            } +
                resolver
                    .getSymbolsWithAnnotation("io.github.devngho.kirok.Intent")
                    .filterIsInstance<KSFunctionDeclaration>()
                    .filter {
                        it.extensionReceiver?.resolve()?.declaration?.qualifiedName?.asString() == model.qualifiedName?.asString()
                    })

    fun resolveInitFunction(resolver: Resolver, model: KSClassDeclaration) = resolver
        .getSymbolsWithAnnotation("io.github.devngho.kirok.Init")
        .filterIsInstance<KSFunctionDeclaration>()
        .firstOrNull {
            it.returnType?.resolve() == model.asStarProjectedType() || // normal init function
                ((it.firstAnnotation("io.github.devngho.kirok.Init")?.first<Boolean>() == true) // or useRetriever = true
                 && it.parentDeclaration?.parentDeclaration == model)           // and parent declaration is model
        }

    private fun resolveRetrieverInfoCompanion(resolver: Resolver, model: KSClassDeclaration) = resolver
        .getSymbolsWithAnnotation("io.github.devngho.kirok.RetrieveInfo")
        .filterIsInstance<KSFunctionDeclaration>()
        .filter {
            it.parentDeclaration?.parentDeclaration == model
        }

    private fun resolveRetrieverInfoMethod(resolver: Resolver, model: KSClassDeclaration) = resolver
        .getSymbolsWithAnnotation("io.github.devngho.kirok.RetrieveInfo")
        .filterIsInstance<KSFunctionDeclaration>()
        .filter {
            it.parentDeclaration == model
        }

    private fun resolveRetrieverDataCompanion(resolver: Resolver, model: KSClassDeclaration) = resolver
        .getSymbolsWithAnnotation("io.github.devngho.kirok.RetrieveData")
        .filterIsInstance<KSFunctionDeclaration>()
        .filter {
            it.parentDeclaration?.parentDeclaration == model
        }

    private fun resolveRetrieverDataMethod(resolver: Resolver, model: KSClassDeclaration) = resolver
        .getSymbolsWithAnnotation("io.github.devngho.kirok.RetrieveData")
        .filterIsInstance<KSFunctionDeclaration>()
        .filter {
            it.parentDeclaration == model
        }

    private fun validIntent(intents: Sequence<KSFunctionDeclaration>, logger: KSPLogger) {
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
    }

    @OptIn(ExperimentalContracts::class)
    private fun validInit(init: KSFunctionDeclaration?, model: KSClassDeclaration, logger: KSPLogger): Boolean {
        contract {
            returns(true) implies (init != null)
        }

        if (init == null) {
            logger.error(("Init function not found. Please define init function for ${model.simpleName.asString()} model, then add @Init annotation to init function."))
            return false
        }
        val parent = init.parentDeclaration
        if (parent is KSClassDeclaration && !parent.isCompanionObject) {
            logger.error("Init function must be static. Use companion object instead.")
            return false
        }

        return true
    }

    private fun FunctionBodyDSL.generateIntentCaller(loadFunc: FunSpec, it: KSFunctionDeclaration, isMethod: Boolean = false) {
        "val nextModel = %N(model)"(loadFunc)

        if ((it.parameters.count() == 1 && !isMethod) || (it.parameters.isEmpty() && isMethod)) {
            generateIntentCallerWithoutParameter(it, isMethod)
        } else {
            generateIntentCallerWithParameter(it, isMethod)
        }
    }
    private fun FunctionBodyDSL.generateIntentCallerWithoutParameter(
        it: KSFunctionDeclaration,
        isMethod: Boolean = false
    ) {
        if (isMethod) {
            if (it.extensionReceiver != null) "nextModel.%M()"(it.toMemberName()) // extension function should be imported
            else "nextModel.%N()"(it.toMemberName()) // member function should not be imported
        }
        else "%M(nextModel)"(it.toMemberName())
    }

    private fun FunctionBodyDSL.generateIntentCallerWithParameter(it: KSFunctionDeclaration, isMethod: Boolean = false) {
        val argsString = List(
            it.parameters.subList(
                if (isMethod) 0 else 1,
                it.parameters.count()
            ).size
        ) { i -> "arg$i" }.joinToString(", ")

        if (isMethod) "nextModel.%N(%L)"(it.toMemberName(), argsString)
        else "%M(%L)"(it.toMemberName(), argsString)
    }

    private fun FunctionBodyDSL.generateIntentParameters(func: KSFunctionDeclaration, isMethod: Boolean) {
        "kotlin.js" annotate "JsExport" declareTo this

        "model" parameter String::class

        if ((func.parameters.count() == 1 && !isMethod) || (func.parameters.isEmpty() && isMethod)) return

        "parameters" parameter String::class

        "val list = %T.decodeFromString<%T>(parameters)"(json, List::class.parameterizedBy(String::class))

        func.parameters.forEachIndexed { i, p ->
            "val arg$i = list[$i].let { %T.decodeFromString<%T>(it) }"(json, p.type.resolve().toTypeName())
        }
    }

    private fun createInitFunction(
        init: KSFunctionDeclaration,
        model: KSClassDeclaration,
        saveFunc: FunSpec,
        logger: KSPLogger,
        resolver: Resolver
    ): FunctionDSL {
        val intentAnnotation = init.firstAnnotation("io.github.devngho.kirok.Init")
        val useRetriever = intentAnnotation?.arguments?.firstOrNull()?.value as? Boolean ?: false

        if (useRetriever) return createRetrieverInitFunction(init, model, saveFunc, logger, resolver)

        val isSuspend = init.modifiers.any { it == Modifier.SUSPEND }

        return "init" + model.simpleName.asString() kfun {
            "kotlin.js" annotate  "JsExport" declareTo this

            "val list = %T.decodeFromString<%T>(parameters)"(json, List::class.parameterizedBy(String::class))

            init.parameters.forEachIndexed { i, p ->
                "val arg$i = list[$i].let { %T.decodeFromString<%T>(it) }"(json, p.type.resolve().toTypeName())
            }

            val parameterText = List(init.parameters.count()) { i -> "arg$i" }.joinToString(", ")

            if (isSuspend) {
                optIn(jsExport, delicateCoroutinesApi)
                kControlFlow("return GlobalScope.promise") {
                    if (init.parentDeclaration == null) {
                        "%N(%M($parameterText))"(saveFunc, init.toMemberName())
                    } else {
                        "%N(%T.%M($parameterText))"(saveFunc, model.toClassName(), init.toMemberName())
                    }
                }()
            } else {
                optIn(jsExport)
                if (init.parentDeclaration == null) {
                    "return %N(%M($parameterText))"(saveFunc, init.toMemberName())
                } else {
                    "return %N(%T.%M($parameterText))"(saveFunc, model.toClassName(), init.toMemberName())
                }
            }

//            init.parameters.forEachIndexed { i, p ->
//                "arg$i" parameter p.type.resolve().toTypeName()
//            }
            "parameters" parameter String::class

        } returns (if (isSuspend) ClassName("kotlin.js", "Promise").parameterizedBy(ClassName("kotlin.js", "JsAny").copy(nullable = true))
        else ClassName("kotlin", "String"))
    }

    sealed interface RetrieverMetaLocation {
        data class ReturnValueSingle(
            override val type: KSType
        ): RetrieverMetaLocation
        data class ReturnValuePair(
            override val type: KSType
        ): RetrieverMetaLocation

        data class Companion(val function: KSFunctionDeclaration, override val type: KSType?): RetrieverMetaLocation
        data class Method(val function: KSFunctionDeclaration, override val type: KSType?): RetrieverMetaLocation

        data class None(override val type: KSType?): RetrieverMetaLocation

        val type: KSType?
    }

    private fun findRetrieverAt(declaration: KSDeclaration, logger: KSPLogger): KSType? {
        val retrieveWith = declaration.firstAnnotation("io.github.devngho.kirok.RetrieveWith")

        if (retrieveWith != null) {
            val retriever = retrieveWith.arguments.firstOrNull()?.value as? KSType

            return retriever ?: run {
                logger.error("Retriever not found for function ${declaration.qualifiedName?.asString()}")
                throw IllegalStateException("Retriever not found for function ${declaration.qualifiedName?.asString()}")
            }
        }

        return null
    }

    private fun findRetriever(function: KSFunctionDeclaration, logger: KSPLogger, model: KSClassDeclaration): KSType {
        findRetrieverAt(function, logger)?.let { return it }
        findRetrieverAt(model, logger)?.let { return it }

        logger.error("Retriever not found for function ${function.qualifiedName?.asString()}")
        throw IllegalStateException("Retriever not found for function ${function.qualifiedName?.asString()}")
    }

    private fun findRetrieverInfo(retriever: KSType, function: KSFunctionDeclaration, resolver: Resolver, model: KSClassDeclaration): RetrieverMetaLocation {
        val retrieverInfoType = (retriever.declaration as KSClassDeclaration).superTypes.first().resolve().arguments.firstOrNull()?.type ?: return RetrieverMetaLocation.None(null)

        if (function.returnType?.resolve() == retrieverInfoType.resolve()) {
            return RetrieverMetaLocation.ReturnValueSingle(retrieverInfoType.resolve())
        }

        // or return value is a pair
        if (function.returnType?.resolve()?.arguments?.firstOrNull()?.type?.resolve() == retrieverInfoType.resolve() && function.returnType?.resolve()?.declaration?.qualifiedName?.asString() == "kotlin.Pair") {
            return RetrieverMetaLocation.ReturnValuePair(retrieverInfoType.resolve())
        }

        val companion = resolveRetrieverInfoCompanion(resolver, model).toList()
        val method = resolveRetrieverInfoMethod(resolver, model).toList()

        if (method.isNotEmpty()) {
            val m = method.firstOrNull { it.returnType?.resolve() == retrieverInfoType.resolve() }
            if (m != null) return RetrieverMetaLocation.Method(m, retrieverInfoType.resolve())
        }

        if (companion.isNotEmpty()) {
            val c = companion.firstOrNull { it.returnType?.resolve() == retrieverInfoType.resolve() }
            if (c != null) return RetrieverMetaLocation.Companion(c, retrieverInfoType.resolve())
        }

        return RetrieverMetaLocation.None(retrieverInfoType.resolve())
    }

    @OptIn(KspExperimental::class)
    private fun findRetrieverData(function: KSFunctionDeclaration, resolver: Resolver, model: KSClassDeclaration): RetrieverMetaLocation {
        val retrieverDataType = resolver.getKotlinClassByName("io.github.devngho.kirok.RetrieverData")?.asStarProjectedType() ?: return RetrieverMetaLocation.None(null)

        if (function.returnType?.resolve()?.let { retrieverDataType.isAssignableFrom(it) } == true) {
            val type = function.returnType?.resolve() ?: return RetrieverMetaLocation.ReturnValueSingle(retrieverDataType)

            return RetrieverMetaLocation.ReturnValueSingle(type)
        }

        // or return value is a pair
        if (function.returnType?.resolve()?.arguments?.getOrNull(1)?.type?.resolve()?.let { retrieverDataType.isAssignableFrom(it) } == true &&
            function.returnType?.resolve()?.declaration?.qualifiedName?.asString() == "kotlin.Pair") {
            val type = function.returnType?.resolve()?.arguments?.getOrNull(1)?.type?.resolve() ?: return RetrieverMetaLocation.ReturnValuePair(retrieverDataType)

            return RetrieverMetaLocation.ReturnValuePair(type)
        }

        val companion = resolveRetrieverDataCompanion(resolver, model).toList()
        val method = resolveRetrieverDataMethod(resolver, model).toList()

        if (method.isNotEmpty()) {
            val m = method.firstOrNull { retrieverDataType.isAssignableFrom(it.returnType?.resolve() ?: return@firstOrNull false) }
            if (m != null) return RetrieverMetaLocation.Method(m, retrieverDataType)
        }

        if (companion.isNotEmpty()) {
            val c = companion.firstOrNull { retrieverDataType.isAssignableFrom(it.returnType?.resolve() ?: return@firstOrNull false) }
            if (c != null) return RetrieverMetaLocation.Companion(c, retrieverDataType)
        }

        return RetrieverMetaLocation.None(retrieverDataType)
    }

    private fun createRetrieverInitFunction(
        init: KSFunctionDeclaration,
        model: KSClassDeclaration,
        saveFunc: FunSpec,
        logger: KSPLogger,
        resolver: Resolver
    ): FunctionDSL {
        return "init" + model.simpleName.asString() kfun {
            val retriever = findRetriever(init, logger, model)
            val retrieverInfo = findRetrieverInfo(retriever, init, resolver, model)
            val retrieverData = findRetrieverData(init, resolver, model)

            "kotlin.js" annotate  "JsExport" declareTo this

            "val list = %T.decodeFromString<%T>(parameters)"(json, List::class.parameterizedBy(String::class))

            init.parameters.forEachIndexed { i, p ->
                "val arg$i = list[$i].let { %T.decodeFromString<%T>(it) }"(json, p.type.resolve().toTypeName())
            }

            val parameterText = List(init.parameters.count()) { i -> "arg$i" }.joinToString(", ")

            optIn(jsExport, delicateCoroutinesApi)
            kControlFlow("return GlobalScope.promise") {
                if (init.parentDeclaration == null) {
                    "val result = %M($parameterText)"(init.toMemberName())
                } else {
                    "val result = %T.%M($parameterText)"(model.toClassName(), init.toMemberName())
                }

                when(retrieverInfo) {
                    is RetrieverMetaLocation.ReturnValueSingle -> {
                        "val info = result"()
                    }
                    is RetrieverMetaLocation.ReturnValuePair -> {
                        "val (info, data) = result"()
                    }
                    is RetrieverMetaLocation.Companion -> {
                        "val info = %T.%M()"(model.toClassName(), retrieverInfo.function.toMemberName())
                    }
                    is RetrieverMetaLocation.Method, // can't use method for retriever info at init
                    is RetrieverMetaLocation.None -> {
                        "val info = null"()
                    }
                }

                when(retrieverData) {
                    is RetrieverMetaLocation.ReturnValueSingle -> {
                        "val data = result"()
                    }

                    is RetrieverMetaLocation.ReturnValuePair -> {
                        // already defined
                    }

                    is RetrieverMetaLocation.Companion -> {
                        "val data = %T.%M()"(model.toClassName(), retrieverData.function.toMemberName())
                    }

                    is RetrieverMetaLocation.Method, // can't use method for retriever data at init
                    is RetrieverMetaLocation.None -> {
                        "val data = object: %T {}"(retrieverData.type!!.toClassName())
                    }
                }

                "return@promise %N(%T.retrieve(info, %T::class, %T::class, %S, %S, data)).toJsString()"(saveFunc, retriever.toClassName(), model.toClassName(), retrieverData.type!!.toClassName(), model.qualifiedName!!.asString(), retrieverData.type?.declaration?.qualifiedName?.asString() ?: "")
            }()

//            init.parameters.forEachIndexed { i, p ->
//                "arg$i" parameter p.type.resolve().toTypeName()
//            }
            "parameters" parameter String::class

        } returns ClassName("kotlin.js", "Promise").parameterizedBy(ClassName("kotlin.js", "JsAny").copy(nullable = true))
    }

    private fun createRetrieverIntentFunction(
        intent: KSFunctionDeclaration,
        model: KSClassDeclaration,
        loadFunc: FunSpec,
        saveFunc: FunSpec,
        logger: KSPLogger,
        resolver: Resolver,
        isMethod: Boolean = false
    ): FunctionDSL = intent.simpleName.asString() + model.simpleName.asString() kfun {
            val retriever = findRetriever(intent, logger, model)
            val retrieverInfo = findRetrieverInfo(retriever, intent, resolver, model)
            val retrieverData = findRetrieverData(intent, resolver, model)

            "kotlin.js" annotate  "JsExport" declareTo this

            "val list = %T.decodeFromString<%T>(parameters)"(json, List::class.parameterizedBy(String::class))

            intent.parameters.forEachIndexed { i, p ->
                "val arg$i = list[$i].let { %T.decodeFromString<%T>(it) }"(json, p.type.resolve().toTypeName())
            }

            val parameterText = List(intent.parameters.count()) { i -> "arg$i" }.joinToString(", ")

            "val nextModel = %N(model)"(loadFunc)

            optIn(jsExport, delicateCoroutinesApi)
            kControlFlow("return GlobalScope.promise") {
                if (isMethod) {
                    "val result = nextModel.%N($parameterText)"(intent.toMemberName())
                } else {
                    "val result = %M(nextModel, $parameterText)"(intent.toMemberName())
                }

                when(retrieverInfo) {
                    is RetrieverMetaLocation.ReturnValueSingle -> {
                        "val info = result"()
                    }
                    is RetrieverMetaLocation.ReturnValuePair -> {
                        "val (info, data) = result"()
                    }
                    is RetrieverMetaLocation.Companion -> {
                        "val info = %T.%M()"(model.toClassName(), retrieverInfo.function.toMemberName())
                    }
                    is RetrieverMetaLocation.Method -> {
                        "val info = nextModel.%N()"(retrieverInfo.function.toMemberName())
                    }
                    is RetrieverMetaLocation.None -> {
                        "val info = null"()
                    }
                }

                when(retrieverData) {
                    is RetrieverMetaLocation.ReturnValueSingle -> {
                        "val data = result"()
                    }

                    is RetrieverMetaLocation.ReturnValuePair -> {
                        // already defined
                    }

                    is RetrieverMetaLocation.Companion -> {
                        "val data = %T.%M()"(model.toClassName(), retrieverData.function.toMemberName())
                    }

                    is RetrieverMetaLocation.Method -> {
                        "val data = nextModel.%N()"(retrieverData.function.toMemberName())
                    }

                    is RetrieverMetaLocation.None -> {
                        "val data = object: %T {}"(retrieverData.type!!.toClassName())
                    }
                }

                "return@promise %N(%T.intent(info, %S, nextModel, %T::class, %T::class, %S, %S, data)).toJsString()"(saveFunc, retriever.toClassName(), intent.simpleName.asString(), model.toClassName(), retrieverData.type!!.toClassName(), model.qualifiedName!!.asString(), retrieverData.type?.declaration?.qualifiedName?.asString() ?: "")
            }()

//            init.parameters.forEachIndexed { i, p ->
//                "arg$i" parameter p.type.resolve().toTypeName()
//            }
            "model" parameter String::class
            "parameters" parameter String::class

        } returns ClassName("kotlin.js", "Promise").parameterizedBy(ClassName("kotlin.js", "JsAny").copy(nullable = true))

    private fun createIntentFunction(
        intent: KSFunctionDeclaration,
        model: KSClassDeclaration,
        loadFunc: FunSpec,
        saveFunc: FunSpec,
        logger: KSPLogger,
        resolver: Resolver,
        isMethod: Boolean = false
    ): FunctionDSL {
        val intentAnnotation = intent.firstAnnotation("io.github.devngho.kirok.Intent")
        val useRetriever = intentAnnotation?.arguments?.firstOrNull()?.value as? Boolean ?: false

        if (useRetriever) return createRetrieverIntentFunction(intent, model, loadFunc, saveFunc, logger, resolver, isMethod)

        val isSuspend = intent.modifiers.any { a -> a == Modifier.SUSPEND }

        return if (isSuspend) {
            intent.simpleName.asString() + model.simpleName.asString() kfun {
                generateIntentParameters(intent, isMethod)
                optIn(jsExport, delicateCoroutinesApi)

                kControlFlow("return GlobalScope.promise") {
                    generateIntentCaller(loadFunc, intent, isMethod)

                    "return@promise %N(nextModel).toJsString()"(saveFunc)
                }()

            } returns (ClassName("kotlin.js", "Promise").parameterizedBy(ClassName("kotlin.js", "JsAny").copy(nullable = true)))
        } else {
            intent.simpleName.asString() + model.simpleName.asString() kfun {
                generateIntentParameters(intent, isMethod)
                optIn(jsExport)

                generateIntentCaller(loadFunc, intent, isMethod)

                "return %N(nextModel)"(saveFunc)

            } returns String::class
        }
    }

    fun generateModel(packageName: String, resolver: Resolver, model: KSClassDeclaration, logger: KSPLogger): Pair<FileSpec, Dependencies> {
        val intents = resolveIntents(resolver, model)
        val methodIntents = resolveMethodIntents(resolver, model)
        val init = resolveInitFunction(resolver, model)

        validIntent(intents, logger)

        return fileSpec(packageName, "${model.simpleName.asString()}Kirok") {
            "kotlinx.coroutines" import "promise" declareTo this@fileSpec
            "kotlinx.coroutines" import "GlobalScope" declareTo this@fileSpec

            val saveFunc = saveFunction("save" + model.simpleName.asString(), model)
            val loadFunc = loadFunction("load" + model.simpleName.asString(), model)

            if (!validInit(init, model, logger)) return@fileSpec
            createInitFunction(init, model, saveFunc, logger, resolver) declareTo this@fileSpec

            intents.forEach {
                createIntentFunction(it, model, loadFunc, saveFunc, logger, resolver) declareTo this@fileSpec
            }

            methodIntents.forEach {
                createIntentFunction(it, model, loadFunc, saveFunc, logger, resolver, true) declareTo this@fileSpec
            }

        } to Dependencies(true, *listOf(
            model.containingFile!!,
            *intents.map { it.containingFile!! }.toList().toTypedArray(),
            *methodIntents.map { it.containingFile!! }.toList().toTypedArray(),
            init?.containingFile!!
        ).distinctBy { it.filePath }.toTypedArray())
    }
}