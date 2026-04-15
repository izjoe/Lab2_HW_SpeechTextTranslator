package com.example.speechtotext

import com.google.gson.annotations.SerializedName

data class TranslationResponse(
    @SerializedName("translatedText")
    val translatedText: String
)