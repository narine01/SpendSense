package com.example.spendsense

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody.Part.Companion.createFormData
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

class FileTooLargeException : IOException("File too large")

interface ExpenseRepository {
    fun observeAll(): Flow<List<Expense>>
    suspend fun get(id: Int): Expense?
    suspend fun save(expense: Expense)
    suspend fun delete(id: Int)
    suspend fun scanBill(uri: Uri): ScannedBill
    fun askStream(question: String): Flow<StreamEvent>
}

class ExpenseRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ExpenseDao,
    private val api: BackendApi
) : ExpenseRepository {

    override fun observeAll() = dao.getAll()
    override suspend fun get(id: Int) = dao.getById(id)
    override suspend fun save(expense: Expense) = dao.upsert(expense)
    override suspend fun delete(id: Int) = dao.deleteById(id)

    override suspend fun scanBill(uri: Uri): ScannedBill {
        val resolver = context.contentResolver
        try {
            val mime = resolver.getType(uri) ?: "image/jpeg"
            val bytes = withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: throw IOException("Cannot read file")
            if (bytes.size > MAX_FILE_BYTES) throw FileTooLargeException()
            val part = createFormData("file", "bill", bytes.toRequestBody(mime.toMediaType()))
            return api.scanBill(part)
        } finally {
            // Photos taken in the app are temporary files: don't keep bills on the device.
            if (uri.scheme == "file") uri.path?.let { File(it).delete() }
        }
    }

    override fun askStream(question: String): Flow<StreamEvent> = flow {
        // Read straight from the database (not a cached StateFlow that may still be empty).
        val rows = dao.getAll().first().take(MAX_EXPENSES).map {
            ExpenseDto(it.title, it.amount, it.category, it.date.toString())
        }
        val body = api.askStream(AskRequest(question, LocalDate.now().toString(), rows))
        body.use {
            val source = it.source()
            while (true) {
                val line = source.readUtf8Line() ?: break
                val event = parseStreamLine(line) ?: continue
                emit(event)
                if (event is StreamEvent.Done || event is StreamEvent.Error) break
            }
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val MAX_FILE_BYTES = 10_000_000
        const val MAX_EXPENSES = 300
    }
}
