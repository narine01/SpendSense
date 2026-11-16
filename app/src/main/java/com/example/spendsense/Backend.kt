package com.example.spendsense

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
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

data class ExpenseDto(
    val title: String,
    val amount: Double,
    val category: String,
    val date: String
)

data class AskRequest(
    val question: String,
    val today: String,
    val expenses: List<ExpenseDto>
)

data class AskResponse(val answer: String)

interface BackendApi {
    @Multipart
    @POST("scan-bill")
    suspend fun scanBill(@Part file: MultipartBody.Part): ScannedBill

    @POST("ask")
    suspend fun ask(@Body body: AskRequest): AskResponse
}

object Backend {
    // Emulator with `adb reverse tcp:8000 tcp:8000`: 127.0.0.1 reaches your Mac.
    // Before publishing: your deployed https:// URL.
    private const val BASE_URL = "https://spendsense-backend-hgtl.onrender.com/"

    val api: BackendApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("X-App-Token", BuildConfig.APP_TOKEN)
                        .build()
                )
            }
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApi::class.java)
    }
}