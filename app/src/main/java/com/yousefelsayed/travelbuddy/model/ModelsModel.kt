package com.yousefelsayed.travelbuddy.model

import kotlinx.serialization.Serializable

@Serializable
data class ModelsModel(
    val models: List<String>
)