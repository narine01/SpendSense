package com.example.spendsense

import com.google.gson.JsonParser
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming

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

interface BackendApi {
    @Multipart
    @POST("scan-bill")
    suspend fun scanBill(@Part file: MultipartBody.Part): ScannedBill

    // @Streaming: do not buffer the whole body, hand it over as it arrives.
    @Streaming
    @POST("ask/stream")
    suspend fun askStream(@Body body: AskRequest): ResponseBody
}

/** One line of the server's newline-delimited JSON stream. */
sealed interface StreamEvent {
    data class Delta(val text: String) : StreamEvent
    data class Error(val message: String) : StreamEvent
    data object Done : StreamEvent
}

/** Parses one NDJSON line. Returns null for blank or malformed lines (they are skipped). */
fun parseStreamLine(line: String): StreamEvent? {
    if (line.isBlank()) return null
    return try {
        val obj = JsonParser.parseString(line).asJsonObject
        when {
            obj.has("delta") -> StreamEvent.Delta(obj.get("delta").asString)
            obj.has("error") -> StreamEvent.Error(obj.get("error").asString)
            obj.has("done") -> StreamEvent.Done
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}
