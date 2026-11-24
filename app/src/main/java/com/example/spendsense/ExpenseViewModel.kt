package com.example.spendsense

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import javax.inject.Inject

sealed interface ScanState {
    data object Idle : ScanState
    data object Loading : ScanState
    data class Success(val bill: ScannedBill) : ScanState
    data class Error(val message: String) : ScanState
}

data class ChatMessage(val text: String, val fromUser: Boolean)

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    val expenses: StateFlow<List<Expense>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveExpense(id: Int, title: String, amount: Double, category: String, date: LocalDate) {
        viewModelScope.launch { repository.save(Expense(id, title, amount, category, date)) }
    }

    suspend fun getExpense(id: Int): Expense? = repository.get(id)

    fun deleteExpense(id: Int) {
        viewModelScope.launch { repository.delete(id) }
    }

    // ---- Bill scan ----

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    private var scanJob: Job? = null

    fun scanBill(uri: Uri) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _scanState.value = ScanState.Loading
            try {
                _scanState.value = ScanState.Success(repository.scanBill(uri))
            } catch (e: CancellationException) {
                throw e // never swallow cancellation
            } catch (e: Exception) {
                Log.e("SpendSense", "Scan failed", e)
                _scanState.value = ScanState.Error(
                    when {
                        e is FileTooLargeException -> "That file is too large (max 10 MB)."
                        e is HttpException && e.code() == 429 ->
                            "The AI has hit its daily limit. Please try again later."
                        else -> "Couldn't read that bill. Try again or enter it manually."
                    }
                )
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _scanState.value = ScanState.Idle
    }

    fun resetScan() {
        _scanState.value = ScanState.Idle
    }

    // ---- Ask your spending (streamed) ----

    private val _chat = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chat: StateFlow<List<ChatMessage>> = _chat.asStateFlow()

    private val _asking = MutableStateFlow(false)
    val asking: StateFlow<Boolean> = _asking.asStateFlow()
    private var askJob: Job? = null

    fun ask(question: String) {
        val q = question.trim()
        if (q.isEmpty() || _asking.value) return
        _asking.value = true
        // The answer bubble is added empty and filled in as pieces arrive.
        _chat.update { it + ChatMessage(q, fromUser = true) + ChatMessage("", fromUser = false) }

        askJob = viewModelScope.launch {
            try {
                repository.askStream(q).collect { event ->
                    when (event) {
                        is StreamEvent.Delta -> appendToLastReply(event.text)
                        is StreamEvent.Error -> setLastReply(event.message, onlyIfEmpty = true)
                        StreamEvent.Done -> Unit
                    }
                }
                setLastReply("I couldn't come up with an answer.", onlyIfEmpty = true)
            } catch (e: CancellationException) {
                // User pressed Stop: keep what has arrived so far.
                setLastReply("Stopped.", onlyIfEmpty = true)
                throw e
            } catch (e: Exception) {
                Log.e("SpendSense", "Ask failed", e)
                val message = if (e is HttpException && e.code() == 429)
                    "The AI is busy or has hit its daily limit. Please try again later."
                else
                    "Sorry, I couldn't answer that right now. Please try again."
                setLastReply(message, onlyIfEmpty = true)
            } finally {
                _asking.value = false
            }
        }
    }

    fun stopAsking() {
        askJob?.cancel()
    }

    private fun appendToLastReply(piece: String) = _chat.update { list ->
        list.dropLast(1) + list.last().copy(text = list.last().text + piece)
    }

    private fun setLastReply(text: String, onlyIfEmpty: Boolean) = _chat.update { list ->
        val last = list.lastOrNull() ?: return@update list
        if (last.fromUser || (onlyIfEmpty && last.text.isNotEmpty())) list
        else list.dropLast(1) + last.copy(text = text)
    }
}
