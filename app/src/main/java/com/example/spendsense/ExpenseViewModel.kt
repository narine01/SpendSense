package com.example.spendsense

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody.Part.Companion.createFormData
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.LocalDate

sealed interface ScanState {
    data object Idle : ScanState
    data object Loading : ScanState
    data class Success(val bill: ScannedBill) : ScanState
    data class Error(val message: String) : ScanState
}

data class ChatMessage(val text: String, val fromUser: Boolean)

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = Room.databaseBuilder(app, AppDatabase::class.java, "spendsense.db")
        .build()
        .expenseDao()

    val expenses: StateFlow<List<Expense>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveExpense(id: Int, title: String, amount: Double, category: String, date: LocalDate) {
        viewModelScope.launch {
            dao.upsert(Expense(id, title, amount, category, date))
        }
    }

    suspend fun getExpense(id: Int): Expense? = dao.getById(id)

    fun deleteExpense(id: Int) {
        viewModelScope.launch { dao.deleteById(id) }
    }

    // ---- Bill scan ----

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    fun scanBill(uri: Uri) {
        viewModelScope.launch {
            _scanState.value = ScanState.Loading
            try {
                val resolver = getApplication<Application>().contentResolver
                val mime = resolver.getType(uri) ?: "image/jpeg"
                val bytes = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IOException("Cannot read file")
                val part = createFormData("file", "bill", bytes.toRequestBody(mime.toMediaType()))
                _scanState.value = ScanState.Success(Backend.api.scanBill(part))
            } catch (e: Exception) {
                Log.e("SpendSense", "Scan failed", e)
                _scanState.value = ScanState.Error("Couldn't read that bill. Try again or enter it manually.")
            }
        }
    }

    fun resetScan() {
        _scanState.value = ScanState.Idle
    }

    // ---- Ask your spending ----

    private val _chat = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chat: StateFlow<List<ChatMessage>> = _chat.asStateFlow()

    private val _asking = MutableStateFlow(false)
    val asking: StateFlow<Boolean> = _asking.asStateFlow()

    fun ask(question: String) {
        if (question.isBlank() || _asking.value) return
        viewModelScope.launch {
            _chat.update { it + ChatMessage(question.trim(), fromUser = true) }
            _asking.value = true
            try {
                // Read straight from the database. `expenses.value` can still be the empty
                // initial value if no screen is collecting it right now.
                val rows = dao.getAll().first().take(300).map {
                    ExpenseDto(it.title, it.amount, it.category, it.date.toString())
                }
                val reply = Backend.api.ask(
                    AskRequest(question.trim(), LocalDate.now().toString(), rows)
                )
                _chat.update { it + ChatMessage(reply.answer, fromUser = false) }
            } catch (e: Exception) {
                Log.e("SpendSense", "Ask failed", e)
                _chat.update {
                    it + ChatMessage("Sorry, I couldn't answer that right now. Please try again.", fromUser = false)
                }
            } finally {
                _asking.value = false
            }
        }
    }
}