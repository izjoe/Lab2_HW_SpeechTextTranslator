package com.example.speechtotext

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("language/translate/v2")
    suspend fun translate(
        @Header("Authorization") authorization: String,
        @Body request: TranslationRequest
    ): Response<ResponseBody>
}