package com.example.speechtotext

data class TranslationRequest(
    val q: String,
    val target: String,
    val source: String,
    val format: String = "text",
    val formality: String = "auto",
    val context: String = "speech translator demo",
    val instructions: String = "Keep the translation natural.",
    val translationMemory: Boolean = true
)