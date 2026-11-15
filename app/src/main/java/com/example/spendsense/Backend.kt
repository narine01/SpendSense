package com.example.spendsense

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

data class ScannedBill(
    val merchant: String,
    val amount: Double,
    val date: String,
    val category: String
)

interface BackendApi {
    @Multipart
    @POST("scan-bill")
    suspend fun scanBill(@Part file: MultipartBody.Part): ScannedBill
}

object Backend {
    private const val BASE_URL = "http://127.0.0.1:8000/" // need to change before deploying to playstore

    val api: BackendApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApi::class.java)
    }
}