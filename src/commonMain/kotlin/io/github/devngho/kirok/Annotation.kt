package io.github.devngho.kirok

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MetaSerializable
import kotlin.reflect.KClass

/**
 * This annotation is used to mark a class as a model.
 * A model is a class that is used to store data.
 * A model class must be annotated with this annotation.
 */
@OptIn(ExperimentalSerializationApi::class)
@MetaSerializable
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class Model

/**
 * This annotation is used to mark a function as an intent.
 * An intent is a function that is used to update a model.
 * You can use suspend modifier to make an intent function suspendable.
 *
 * An intent function must follow one of these rules:
 * - First parameter of An intent function must be a model class.
 * - An intent function must be an extension function of a model class.
 * - An intent function must be a method of a model class.
 *
 * Example:
 * ```
 * @Model
 * data class TestModel(var a: Int, var b: String)
 *
 * @Intent
 * fun TestModel.testIntent() {
 *    // the model is already copied, so you can modify it directly.
 *    a += 1
 *    b = "test"
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Intent(val useRetriever: Boolean = false)

/**
 * This annotation is used to mark a function as an init function.
 * An init function is a function that is used to create and initialize a model.
 * An init function must be a top-level or static function.
 * An init function must return a model class.
 *
 * Example:
 * ```
 * @Model
 * data class TestModel(var a: Int, var b: String) {
 *   companion object {
 *     @Init
 *     fun init(): TestModel = TestModel(0, "")
 *   }
 * }
 *
 * // or
 * @Init
 * fun init(): TestModel = TestModel(0, "")
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Init(val useRetriever: Boolean = false)

/**
 * This annotation is used to mark which retriever is used to retrieve data for a model.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class RetrieveWith(val retriever: KClass<out Retriever<out RetrieverInfo>>)

/**
 * This annotation is used to mark a function or parameter as info of a retriever.
 * If a function is marked with this annotation, the function will be used to retrieve info from a retriever.
 * If a parameter is marked with this annotation, the parameter will be used to retrieve info from a retriever.
 *
 * Priority: Return value > Function at parent class > Function at companion object
 *
 * Example:
 * ```
 * @Model
 * data class TestModel(var a: Int, var b: String) {
 *   companion object {
 *     @Init(useRetriever = true)
 *     fun init() {} // this function will use `data1` as a retriever info.
 *
 *     @RetrieveInfo
 *     fun info1() = KtorRetrieverInfo(
 *       path = "https://example.com",
 *       headers = mapOf("a" to "1")
 *     )
 *   }
 *
 *   @RetrieveInfo
 *   fun info2() = KtorRetrieverInfo(
 *     path = "https://example.com",
 *     headers = mapOf("a" to "2")
 *   )
 *
 *   @Intent(useRetriever = true)
 *   fun testIntent() {} // this function will use `data2` as a retriever data.
 *
 *   @Intent(useRetriever = true)
 *   fun testIntent2(): KtorRetrieverInfo {
 *     return KtorRetrieverInfo() // this function will use this return value as a retriever data.
 *   }
 *   @Intent(useRetriever = true)
 *   fun testIntent3(): = KtorRetrieverInfo() to TestModelData(a) // this function will use this return value as a retriever data, with the first value as a retriever info.
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class RetrieveInfo

/**
 * This annotation is used to mark a function or parameter as a data of a retriever.
 * If a function is marked with this annotation, the function will be used to retrieve data from a retriever.
 * If a parameter is marked with this annotation, the parameter will be used to retrieve data from a retriever.
 *
 * Priority: Return value > Function at parent class > Function at companion object
 *
 * Example:
 * ```
 * @Model
 * data class TestModel(var a: Int, var b: String) {
 *   companion object {
 *     @Init(useRetriever = true)
 *     fun init() {} // this function will use `data1` as a retriever info.
 *
 *     @RetrieveData
 *     fun data1() = TestModelData(a)
 *   }
 *
 *   @RetrieveData
 *   fun data2() = TestModelData(a)
 *
 *   @Intent(useRetriever = true)
 *   fun testIntent() {} // this function will use `data2` as a retriever data.
 *
 *   @Intent(useRetriever = true)
 *   fun testIntent2(): TestModelData {
 *     return TestModelData(a) // this function will use this return value as a retriever data.
 *   }
 *   @Intent(useRetriever = true)
 *   fun testIntent3(): = KtorRetrieverInfo() to TestModelData(a) // this function will use this return value as a retriever data, with the first value as a retriever info.
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class RetrieveData