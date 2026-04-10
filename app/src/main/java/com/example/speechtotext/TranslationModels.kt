package com.example.speechtotext

import com.google.gson.annotations.SerializedName

data class TranslationRequest(
    @SerializedName("q") val query: String,
    @SerializedName("source") val source: String = "auto",
    @SerializedName("target") val target: String,
    @SerializedName("format") val format: String = "text",
    @SerializedName("api_key") val apiKey: String = ""
)

data class TranslationResponse(
    @SerializedName("translatedText") val translatedText: String
)
