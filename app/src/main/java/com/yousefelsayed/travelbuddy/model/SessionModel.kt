package com.yousefelsayed.travelbuddy.model

data class SessionModel(
    var selectedModel: String = "",
    var ogText: String = "Spoken text will be here",
    var ogLang: String = "",
    var targetLang: String = "",
    var ogLangCode: String = "",
    var targetLangCode: String = ""
)
