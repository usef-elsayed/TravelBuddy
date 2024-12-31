package com.yousefelsayed.travelbuddy.model

import kotlinx.serialization.Serializable

@Serializable
data class LanguagesModel(
    val lang: List<String>,
    val langMap: Map<String,String>
)