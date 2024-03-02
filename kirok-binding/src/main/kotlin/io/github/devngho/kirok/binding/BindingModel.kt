package io.github.devngho.kirok.binding

import kotlin.reflect.KType

data class BindingModel(
    val name: String,
    val values: Map<String, KType>,
    val intents: Map<String, Map<String, KType>>,
    val init: Map<String, KType>,
    val isInitSuspend: Boolean
//        val protoFilePath: String
)