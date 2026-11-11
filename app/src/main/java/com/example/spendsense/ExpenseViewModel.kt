package com.example.spendsense

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class ExpenseViewModel : ViewModel() {

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private var nextId = 1

    fun addExpense(title: String, amount: Double, category: String) {
        _expenses.update { current ->
            listOf(Expense(nextId++, title, amount, category, LocalDate.now())) + current
        }
    }

    fun deleteExpense(id: Int) {
        _expenses.update { current -> current.filter { it.id != id } }
    }
}