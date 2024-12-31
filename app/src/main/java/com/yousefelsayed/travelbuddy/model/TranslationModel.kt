package com.yousefelsayed.travelbuddy.model

import kotlinx.serialization.Serializable

@Serializable
data class TranslationModel(
    val original: String,
    val translated: String,
    var error: String? = null
)