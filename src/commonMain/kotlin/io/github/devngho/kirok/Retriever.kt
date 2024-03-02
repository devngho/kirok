package io.github.devngho.kirok

import kotlin.reflect.KClass

/**
 * Retriever is an interface that is used to retrieve models and server-side intents.
 * Retrievers must be an object that implements this interface.
 *
 * Retrievers can be setup like this:
 * ```
 * @Model
 * @RetrieveWith(MyRetriever::class)
 * data class TestModel(var a: Int, var b: String) {
 *
 *    companion object {
 *      @Init(useRetriever = true)
 *      fun init() {}
 *    }
 *  }
 */
interface Retriever<T: RetrieverInfo> {
    suspend fun <U: Any> retrieve(info: T?, clazz: KClass<U>, dataClazz: KClass<out RetrieverData>, clazzName: String, dataClazzName: String, data: RetrieverData): U
    suspend fun <U: Any> intent(info: T?, name: String, model: U, clazz: KClass<U>, dataClazz: KClass<out RetrieverData>, clazzName: String, dataClazzName: String, data: RetrieverData): U
}

/**
 * RetrieverInfo is an interface that is used to provide retriever-specific information with the Retriever.
 * The information can be anything required by retriever.
 *
 * Example: URL, Header (for retriever using http), etc.
 */
interface RetrieverInfo

/**
 * RetrieverData is an interface that is used to provide data with the Retriever.
 * The data can be anything.
 *
 * Example: Post id data, Query data, etc.
 */
interface RetrieverData