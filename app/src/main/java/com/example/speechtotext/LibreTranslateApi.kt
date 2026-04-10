package com.example.speechtotext

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface LibreTranslateApi {
    @POST("translate")
    fun translate(@Body request: TranslationRequest): Call<TranslationResponse>
}
